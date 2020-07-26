package se.kry.codetest;

import io.vertx.core.*;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

    private final HashMap<String, String> services = new HashMap<>();
    private final BackgroundPoller poller = new BackgroundPoller();

    private static final Logger LOG;

    static {
        LOG = LoggerFactory.getLogger(MainVerticle.class);
    }

    @Override
    public void init(final Vertx vertx, final Context context) {
        super.init(vertx, context);
    }

    @Override
    public void start(Future<Void> startFuture) {

        LOG.info("NILOG::http.port from property :: " + config().getInteger("http.port", 8080));

        String dbUrl = config().getString("datasource.url", "jdbc:hsqldb:mem:test?shutdown=true");
        String driverClassName = config().getString("datasource.driver.class.name", "org.hsqldb.jdbcDriver");
        String username = config().getString("datasource.driver.username", "sa");
        String password = config().getString("datasource.driver.password", "");

        JDBCClient jdbcClient = JDBCClient.createShared(vertx,
                new JsonObject().put("url", dbUrl)
                        .put("user", username)
                        .put("password", password)
                        .put("driver_class", driverClassName).put("max_pool_size", 30));


        setUpDDL(jdbcClient, startFuture, ready -> {
            Router router = Router.router(vertx);
            router.route().handler(BodyHandler.create());
            //services.put("https://www.kry.se", "UNKNOWN");
            //vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(services));
            setRoutes(router);
            vertx
                    .createHttpServer()
                    .requestHandler(router)
                    .listen(8080, result -> {
                        if (result.succeeded()) {
                            System.out.println("KRY code test service started");
                            startFuture.complete();
                        } else {
                            startFuture.fail(result.cause());
                        }
                    });
        });
    }

    private void setUpDDL(JDBCClient jdbcClient, Future<Void> startFuture, Handler<Void> done) {
        LOG.error("NILOG::setUpDDL");

        jdbcClient.getConnection(connection -> {
            if (connection.failed()) {
                LOG.error("NILOG::Could not connect to the database, exiting!!");
                startFuture.fail(connection.cause());
                throw new RuntimeException(connection.cause());
            }
            FileSystem vertxFileSystem = vertx.fileSystem();
            vertxFileSystem.readFile("ddl/schema.sql", readFile -> {
                if (readFile.failed()) {
                    LOG.error("NILOG::Could not fine schema file, exiting!!");
                    startFuture.fail(readFile.cause());
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
                                startFuture.fail(ddl.cause());
                                throw new RuntimeException(ddl.cause());
                            }
                            done.handle(null);
                        });
            });

        });
    }

    private void setRoutes(Router router) {
        router.route("/*").handler(StaticHandler.create());
        router.get("/service").handler(req -> {
            List<JsonObject> jsonServices = services
                    .entrySet()
                    .stream()
                    .map(service ->
                            new JsonObject()
                                    .put("name", service.getKey())
                                    .put("status", service.getValue()))
                    .collect(Collectors.toList());
            req.response()
                    .putHeader("content-type", "application/json")
                    .end(new JsonArray(jsonServices).encode());
        });
        router.post("/service").handler(req -> {
            JsonObject jsonBody = req.getBodyAsJson();
            services.put(jsonBody.getString("url"), "UNKNOWN");
            req.response()
                    .putHeader("content-type", "text/plain")
                    .end("OK");
        });
    }

}



