package se.kry.codetest;

import io.vertx.core.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.serviceproxy.ServiceBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.PollerService;
import se.kry.codetest.service.UserService;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

    //private final HashMap<String, String> services = new HashMap<>();
    //private final BackgroundPoller poller = new BackgroundPoller();

    HttpServer server;
    ServiceBinder serviceBinder;

    List<MessageConsumer<JsonObject>> registeredConsumers = new ArrayList<>();

    private static final Logger LOG;

    static {
        LOG = LoggerFactory.getLogger(MainVerticle.class);
    }

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

                String dbUrl = config().getString("datasource.url", "jdbc:h2:mem:h2test;MODE=Oracle;DB_CLOSE_DELAY=-1");
                String driverClassName = config().getString("datasource.driver.class.name", "org.h2.Driver");
                String username = config().getString("datasource.driver.username", "sa");
                String password = config().getString("datasource.driver.password", "");
                Boolean setupDbSchema = config().getBoolean("datasource.schema.setup", true);
                /*Boolean jwtAlgorithm = config().getBoolean("jwt.algorithm");
                Boolean jwtPublicKey = config().getBoolean("jwt.publicKey");
                Boolean jwtSecretKey = config().getBoolean("jwt.secretKey");*/

                JDBCClient jdbcClient = JDBCClient.createShared(vertx,
                        new JsonObject().put("url", dbUrl)
                                .put("user", username)
                                .put("password", password)
                                .put("driver_class", driverClassName).put("max_pool_size", 30), "dataSource");
                if(setupDbSchema){
                    setUpDDL(jdbcClient, result -> {
                        LOG.error("NILOG::setUpDDL done.");
                    });
                }
                JsonObject jwkObject = jwk.result().toJsonObject();
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions(jwkObject);
                JWTAuth auth = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(pubSecKeyOptions));
                PollerDao pollerDao = PollerDao.create(jdbcClient);
                UserDao userDao  = UserDao.create(jdbcClient);
                startServices(pollerDao,userDao,auth);
                startHttpServer(auth).onComplete(promise);
            }
        });

    }

    @Override
    public void stop() throws Exception {
        super.stop();
        if(server != null) server.close();
        registeredConsumers.forEach(c -> serviceBinder.unregister(c));
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


                //routerFactory.setOptions(new RouterFactoryOptions().setMountValidationFailureHandler(true));

                routerFactory.addGlobalHandler(LoggerHandler.create());

                routerFactory.mountServicesFromExtensions();

                routerFactory.addSecurityHandler("loggedUserToken", JWTAuthHandler.create(auth));

                Router router = routerFactory.getRouter();
                router.errorHandler(400,routingContext -> {
                    Throwable failure = routingContext.failure();
                    if (failure instanceof ValidationException)

                        routingContext
                                .response()
                                .setStatusCode(400)
                                .putHeader("content-type", "application/text")
                                .end();
                });
                router.route("/*").handler(StaticHandler.create());
                server = vertx.createHttpServer(new HttpServerOptions().setPort(config()
                        .getInteger("http.port", 8080)).setHost(config()
                        .getString("host.name", "localhost")));
                server.requestHandler(router).listen();
                promise.complete();
            } else {
                promise.fail(openAPI3RouterFactoryAsyncResult.cause());
            }
        });
        return promise.future();
    }

    private void setUpDDL(JDBCClient jdbcClient, Handler<Void> done) {
        LOG.error("NILOG::setUpDDL");

        jdbcClient.getConnection(connection -> {
            if (connection.failed()) {
                LOG.error("NILOG::Could not connect to the database, exiting!!");
                this.getVertx().close();
                throw new RuntimeException(connection.cause());
            }
            FileSystem vertxFileSystem = vertx.fileSystem();
            vertxFileSystem.readFile("ddl/schema.sql", readFile -> {
                if (readFile.failed()) {
                    LOG.error("NILOG::Could not fine schema file, exiting!!");
                    this.getVertx().close();
                    throw new RuntimeException();
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
                            this.getVertx().close();
                            throw new RuntimeException(ddl.cause());
                        }
                        done.handle(null);
                    });
            });

        });
    }

    private void startServices(PollerDao pollerDao, UserDao userDao, JWTAuth auth) {
        serviceBinder = new ServiceBinder(vertx);

        registeredConsumers = new ArrayList<>();

        PollerService pollerService = PollerService.create(pollerDao, auth);
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
    }

}



