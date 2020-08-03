package se.kry.codetest;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.event.PollerTask;
import se.kry.codetest.event.impl.PollerTaskImpl;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.PollerService;
import se.kry.codetest.service.UserService;
import se.kry.codetest.worker.PollerWorker;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOG;

    static {
        LOG = LoggerFactory.getLogger(MainVerticle.class);
    }

    HttpServer server;
    ServiceBinder serviceBinder;
    List<MessageConsumer<JsonObject>> registeredConsumers = new ArrayList<>();
    long timerID = 0l;
    JDBCClient jdbcClient;

    @Override
    public void init(final Vertx vertx, final Context context) {
        super.init(vertx, context);
    }


    @Override
    public void start(Promise<Void> promise) throws Exception {
        super.start();
        String jwkPath = config().getString("jwk.path", "jwk.json");
        loadResource(jwkPath).onComplete(jwk -> {
            if (jwk.failed()) promise.fail(jwk.cause());
            else {
                LOG.info("NILOG::http.port from property :: " + config().getInteger("http.port", 8080));

                String dbUrl = config().getString("datasource.url",
                        "jdbc:h2:mem:h2test;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");
                String driverClassName = config().getString("datasource.driver.class.name", "org.h2.Driver");
                String username = config().getString("datasource.driver.username", "sa");
                String password = config().getString("datasource.driver.password", "");
                Boolean setupDbSchema = config().getBoolean("datasource.schema.setup", false);
                int delay = config().getInteger("poller.status.check.scheduler.time.in.ms",30000);
                int workerPoolSize = config().getInteger("worker.pool.size",10);
                Boolean populatedDataSQL = config().getBoolean("populate.data.sql",false);

                jdbcClient = JDBCClient.createShared(vertx,
                        new JsonObject().put("url", dbUrl)
                                .put("user", username)
                                .put("password", password)
                                .put("driver_class", driverClassName)
                                .put("max_pool_size", 30), "dataSource");
                if (setupDbSchema) {
                    setUpDDL(jdbcClient).onComplete(result -> {
                        LOG.info("NILOG::setUpDDL done.");
                        environmentConfig(jwk,workerPoolSize,delay,promise,populatedDataSQL);
                    });
                }else{
                    environmentConfig(jwk,workerPoolSize,delay,promise,populatedDataSQL);
                }
            }
        });

    }

    @Override
    public void stop() throws Exception {
        if(timerID != 0l) vertx.cancelTimer(timerID);
        if (server != null) server.close();
        registeredConsumers.forEach(c -> serviceBinder.unregister(c));
        super.stop();
    }

    private void environmentConfig(AsyncResult<Buffer> jwk, int workerPoolSize, int delay,
                                           Promise<Void> promise, Boolean populatedDataSQL){
        JsonObject jwkObject = jwk.result().toJsonObject();
        PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions(jwkObject);
        JWTAuth auth = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(pubSecKeyOptions));
        PollerDao pollerDao = PollerDao.create(jdbcClient);
        UserDao userDao = UserDao.create(jdbcClient);
        startServices(pollerDao, userDao, auth);

        pollerDao.deleteAllService().onComplete(event -> {
            LOG.info("NILOG::All existing services has deleted.");
        });

        DeploymentOptions workerOpts = new DeploymentOptions()
                .setWorker(true)
                .setWorkerPoolSize(workerPoolSize);

        vertx.deployVerticle(new PollerWorker(vertx,pollerDao), workerOpts, r -> {
            if(r.succeeded()){
                System.out.println("Successfully deployed worker verticle.");
            } else {
                promise.fail(r.cause());
            }
        });

        PollerTask pollerTask = PollerTask.createProxy(vertx, "polling.service_manager");

        timerID = vertx.setPeriodic(delay, aLong -> {
            LOG.info("NILOG::Calling task to update status.");
            pollerTask.updateStatusOfServiceByPolling(event -> {
                if(event.failed()){
                    LOG.error("NILOG::",event.cause());
                    promise.fail(event.cause());
                }else{
                    LOG.info("Task to update status has called.");
                }
            });
        });

        startHttpServer(auth).onComplete(event -> {
            if(event.failed()){
                LOG.error("NILOG::Stopping everything.");
                promise.fail(event.cause());
            }else{
                if(populatedDataSQL){
                    injectDataSql(jdbcClient).onComplete(result -> {
                        if(result.succeeded()) {
                            LOG.info("NILOG::injectDataSql done.");
                            promise.complete();
                        }else{
                            promise.fail(result.cause());
                        }
                    });
                }else{
                    promise.complete();
                }
            }
        });
    }

    private Future<Buffer> loadResource(String path) {
        Promise<Buffer> promise = Promise.promise();
        vertx.fileSystem().readFile(path, res -> {
            if (res.succeeded()) {
                promise.complete(res.result());
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }


    private Future<Void> startHttpServer(JWTAuth auth) {
        Promise<Void> promise = Promise.promise();
        OpenAPI3RouterFactory.create(this.vertx, "api_desc.yaml", openAPI3RouterFactoryAsyncResult -> {
            if (openAPI3RouterFactoryAsyncResult.succeeded()) {
                OpenAPI3RouterFactory routerFactory = openAPI3RouterFactoryAsyncResult.result();

                routerFactory.addGlobalHandler(LoggerHandler.create());

                routerFactory.mountServicesFromExtensions();

                routerFactory.addSecurityHandler("loggedUserToken", JWTAuthHandler.create(auth));

                Router router = routerFactory.getRouter();

                router.route("/*").handler(StaticHandler.create());
                try {
                    server = vertx.createHttpServer()
                            .requestHandler(router)
                            .listen(config().getInteger("http.port", 8080),config()
                                    .getString("host.name", "0.0.0.0"), asyncServerStart -> {
                                if (asyncServerStart.succeeded()) {
                                    LOG.info("NILOG::HTTP server running on port "
                                            +config().getInteger("http.port", 8080));
                                    promise.complete();
                                } else {
                                    LOG.error("NILOG::", asyncServerStart.cause());
                                    promise.fail(asyncServerStart.cause());
                                }
                            });
                }catch (Exception ex){
                    promise.fail(ex);
                }
            } else {
                promise.fail(openAPI3RouterFactoryAsyncResult.cause());
            }
        });
        return promise.future();
    }

    private Future<Void> injectDataSql(JDBCClient jdbcClient) {
        LOG.error("NILOG::injectDataSql");
        Promise<Void> promise = Promise.promise();
        jdbcClient.getConnection(connection -> {
            if (connection.failed()) {
                LOG.error("NILOG::Could not connect to the database, exiting!!");
                promise.fail(connection.cause());
            }
            FileSystem vertxFileSystem = vertx.fileSystem();
            vertxFileSystem.readFile("ddl/data.sql", readFile -> {
                if (readFile.failed()) {
                    LOG.error("NILOG::Could not fine data SQL file, exiting!!");
                    promise.fail(readFile.cause());
                }
                String dataFromSQL = readFile.result().toString(Charset.forName("utf-8"));
                LOG.info("DATA SQL::\n" + dataFromSQL);
                String finalSchema = dataFromSQL.replace("\n", " ");
                final SQLConnection conn = connection.result();
                conn.execute(
                        finalSchema,
                        dataInjection -> {
                            if (dataInjection.failed()) {
                                LOG.error("NILOG::Could not able to setup data SQL, exiting!!");
                                promise.fail(dataInjection.cause());
                            }
                            promise.complete();
                        });
            });

        });
        return promise.future();
    }

    private Future<Void> setUpDDL(JDBCClient jdbcClient) {
        LOG.error("NILOG::setUpDDL");
        Promise<Void> promise = Promise.promise();
        jdbcClient.getConnection(connection -> {
            if (connection.failed()) {
                LOG.error("NILOG::Could not connect to the database, exiting!!");
                promise.fail(connection.cause());
            }
            FileSystem vertxFileSystem = vertx.fileSystem();
            vertxFileSystem.readFile("ddl/schema.sql", readFile -> {
                if (readFile.failed()) {
                    LOG.error("NILOG::Could not fine schema file, exiting!!");
                    promise.fail(readFile.cause());
                }
                String schema = readFile.result().toString(Charset.forName("utf-8"));
                LOG.info("SCHEMA::\n" + schema);
                String finalSchema = schema.replace("\n", " ");
                final SQLConnection conn = connection.result();
                conn.execute(
                        finalSchema,
                        ddl -> {
                            if (ddl.failed()) {
                                LOG.error("NILOG::Could not able to setup schema, exiting!!");
                                promise.fail(ddl.cause());
                            }
                            promise.complete();
                        });
            });

        });
        return promise.future();
    }

    private void startServices(PollerDao pollerDao, UserDao userDao, JWTAuth auth) {
        serviceBinder = new ServiceBinder(vertx);

        registeredConsumers = new ArrayList<>();

        PollerService pollerService = PollerService.create(pollerDao);
        registeredConsumers.add(
                serviceBinder
                        .setAddress("poller.service_manager")
                        .register(PollerService.class, pollerService)
        );

        UserService userService = UserService.create(userDao, auth);
        registeredConsumers.add(
                serviceBinder
                        .setAddress("user.service_manager")
                        .register(UserService.class, userService)
        );

        PollerTask pollerTask = new PollerTaskImpl(vertx, pollerDao);
        registeredConsumers.add(
                serviceBinder
                        .setAddress("polling.service_manager")
                        .register(PollerTask.class, pollerTask)
        );
    }

}



