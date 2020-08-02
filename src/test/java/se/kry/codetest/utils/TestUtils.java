package se.kry.codetest.utils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.FileSystem;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.junit5.VertxTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

    public static Future<Boolean> wipeDatabase(JDBCClient jdbcClient) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String sql = "DELETE FROM SERVICE; DELETE FROM USERS;";
                connection.query(sql,
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::Delete data from tables.");
                                    promise.complete(true);
                                }
                            } catch (Exception ex) {
                                promise.fail(ex);
                            } finally {
                                connection.close();
                            }
                        });
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public static Future<Boolean> setupSchema(Vertx vertx, VertxTestContext testContext, JDBCClient jdbcClient) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(connection -> {
            if (connection.failed()) {
                LOG.error("NILOG::Could not connect to the database, exiting!!");
                testContext.failNow(connection.cause());
            }
            LOG.info("NILOG::Connected to test DB.");
            FileSystem vertxFileSystem = vertx.fileSystem();
            vertxFileSystem.readFile("ddl/schema.sql", readFile -> {
                if (readFile.failed()) {
                    LOG.error("NILOG::Could not fine schema file, exiting!!");
                    testContext.failNow(readFile.cause());
                }
                LOG.info("NILOG::Got SQL.");
                String schema = readFile.result().toString(Charset.forName("utf-8"));
                LOG.info("SCHEMA::\n" + schema);
                String finalSchema = schema.replace("\n", " ");
                final SQLConnection conn = connection.result();
                conn.execute(
                        finalSchema,
                        ddl -> {
                            if (ddl.failed()) {
                                LOG.error("NILOG::Could not able to setup schema, exiting!!");
                                testContext.failNow(ddl.cause());
                            } else {
                                promise.complete(true);
                            }
                        });
            });
        });
        return promise.future();
    }

    public static Future<JWTAuth> setupAuth(Vertx vertx, VertxTestContext testContext, JsonObject conf) {
        Promise<JWTAuth> promise = Promise.promise();
        String jwkPath = conf.getString("jwk.path");
        loadResource(vertx, jwkPath).onComplete(jwk -> {
            if (jwk.failed()) testContext.failNow(jwk.cause());
            else {
                JsonObject jwkObject = jwk.result().toJsonObject();
                PubSecKeyOptions pubSecKeyOptions = new PubSecKeyOptions(jwkObject);
                JWTAuth auth = JWTAuth.create(vertx, new JWTAuthOptions().addPubSecKey(pubSecKeyOptions));
                promise.complete(auth);
            }
        });
        return promise.future();
    }

    public static Future<JDBCClient> setupDatabase(Vertx vertx, JsonObject conf) {
        Promise<JDBCClient> promise = Promise.promise();
        String dbUrl = conf.getString("datasource.url");
        String driverClassName = conf.getString("datasource.driver.class.name");
        String username = conf.getString("datasource.driver.username");
        String password = conf.getString("datasource.driver.password");
        LOG.info("NILOG::dbUrl::" + dbUrl);
        LOG.info("NILOG::driverClassName::" + driverClassName);
        LOG.info("NILOG::username::" + username);
        LOG.info("NILOG::password::" + password);

        JDBCClient jdbcClient = JDBCClient.createShared(vertx,
                new JsonObject().put("url", dbUrl)
                        .put("user", username)
                        .put("password", password)
                        .put("driver_class", driverClassName).put("max_pool_size", 30), "dataSource");
        promise.complete(jdbcClient);
        return promise.future();

    }

    private static Future<Buffer> loadResource(Vertx vertx, String path) {
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

    public static void assertSuccessResponse(OperationResponse actual) {
        assertEquals(Integer.valueOf(200), actual.getStatusCode());
        assertEquals("OK", actual.getStatusMessage());
    }

    public static void assertSuccessResponse(String expectedContentType, OperationResponse actual) {
        assertEquals(Integer.valueOf(200), actual.getStatusCode());
        assertEquals("OK", actual.getStatusMessage());
        assertEquals(expectedContentType, actual.getHeaders().get("content-type"));
    }

    public static void assertBadRequestResponse(OperationResponse actual) {
        assertEquals(Integer.valueOf(400), actual.getStatusCode());
        assertEquals("Bad Request", actual.getStatusMessage());
        assertEquals("text/plain", actual.getHeaders().get("content-type"));
    }

    public static void assertBadRequestResponse(OperationResponse actual, String expectedStatusMessage) {
        assertEquals(Integer.valueOf(400), actual.getStatusCode());
        assertEquals("Bad Request", actual.getStatusMessage());
        assertEquals(expectedStatusMessage, actual.getPayload().toString());
        assertEquals("text/plain", actual.getHeaders().get("content-type"));
    }

    public static void assertSuccessCreateResponse(String expectedContentType, OperationResponse actual) {
        assertEquals(expectedContentType, actual.getHeaders().get("content-type"));
        assertEquals("Created", actual.getStatusMessage());
        assertEquals(Integer.valueOf(201), actual.getStatusCode());
    }

    public static void assertEmptyResponse(OperationResponse actual) {
        assertEquals("text/plain", actual.getHeaders().get("content-type"));
        assertEquals("No Content", actual.getStatusMessage());
        assertEquals(Integer.valueOf(204), actual.getStatusCode());
    }

    public static void assertTextResponse(int expectedStatusCode, String expectedStatusMessage, String expectedResult, OperationResponse actual) {
        assertEquals(Integer.valueOf(expectedStatusCode), actual.getStatusCode());
        assertEquals(expectedStatusMessage, actual.getStatusMessage());
        assertEquals("text/plain", actual.getHeaders().get("content-type"));
        assertEquals(expectedResult, actual.getPayload().toString());
    }
}
