package se.kry.codetest.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.kry.codetest.utils.TestUtils.*;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UsersServiceTest extends BaseServicesTest {
    private static final Logger LOG = LoggerFactory.getLogger(UsersServiceTest.class);

    JDBCClient jdbcClient;
    private UserDao userDao;
    private JWTAuth auth;
    private OperationRequest loggedContext;

    @Override
    @BeforeAll
    public void beforeAll(Vertx vertx, VertxTestContext testContext) throws Exception {
        super.beforeAll(vertx, testContext);
        setupDatabase(vertx, conf).onComplete(database->{
            if(database.succeeded()) {
                jdbcClient = database.result();
                setupSchema(vertx, testContext, jdbcClient).onComplete(schema->{
                    if(schema.succeeded()){
                        if(schema.result()){
                            setupAuth(vertx,testContext,conf).onComplete(jwtAuth -> {
                                if(jwtAuth.succeeded()){
                                    auth = jwtAuth.result();
                                    userDao = UserDao.create(jdbcClient);
                                    userService = UserService.create(userDao, auth);
                                    testContext.completeNow();
                                } else testContext.failNow(jwtAuth.cause());
                            });
                        }
                    } else testContext.failNow(schema.cause());
                });
            } else testContext.failNow(database.cause());
        });
    }

    @BeforeEach
    public void before(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        wipeDatabase(jdbcClient).onComplete(testContext.succeeding(v -> testContext.completeNow()));
        /*testContext.assertComplete(
                wipeDatabase(jdbcClient)
                        .compose(v -> registerBeforeTestLogin(new AuthCredentials("tester", "tester"), testContext))
        ).onComplete(ar -> {
            loggedContext = new OperationRequest().setUser(new JsonObject().put("userId", "tester"));
            testContext.completeNow();
        });
        testContext.awaitCompletion(1000, TimeUnit.MILLISECONDS);*/
    }

    @Test
    public void registerTest(VertxTestContext test) {
        AuthCredentials credentials = new AuthCredentials("tester", "tester");
        AuthCredentials credentialsHashed = credentials.hashPassword();
        userService.register(credentials, new OperationRequest(), test.succeeding(operationResponse -> {
            test.verify(() -> {
                assertSuccessResponse("text/plain", operationResponse);
                assertNotNull(operationResponse.getPayload().toString());
            });
            jdbcClient.getConnection(res -> {
                if (res.succeeded()) {
                    SQLConnection connection = res.result();
                    String sql = "SELECT * FROM USERS WHERE username = ?";
                    connection.queryWithParams(sql,
                            new JsonArray().add(credentialsHashed.getUsername()),
                            result -> {
                                try {
                                    if (result.failed()) {
                                        throw new RuntimeException("Error occurred while executing the query.");
                                    }
                                    List<JsonObject> jsonObject = result.result().getRows();
                                    LOG.info("NILOG::User From DB Size::"+jsonObject.size());
                                    test.verify(() -> {
                                        assertEquals(1, jsonObject.size());
                                        assertEquals(credentialsHashed.getPassword(),
                                                jsonObject.get(0).getString("password"));
                                    });
                                    test.completeNow();
                                }catch (Exception ex){
                                    test.failNow(ex);
                                }finally {
                                    connection.close();
                                }
                            });
                } else {
                    test.failNow(res.cause());
                }
            });
        }));
    }

    @Test
    public void registerAlreadyExistingUserTest(VertxTestContext test) {
        registerBeforeTestLogin(new AuthCredentials("tester", "tester2"), test)
                .onComplete(v -> {
                    userService.register(new AuthCredentials("tester", "tester"),
                            new OperationRequest(), test.succeeding(operationResponse -> {
                        test.verify(() -> {
                            LOG.info("NILOG::register method called.");
                            assertTextResponse(400,
                                    "Bad Request",
                                    "User tester already exists.", operationResponse);
                        });
                        test.completeNow();
                    }));
                });
    }

    @Test
    public void loginTest(VertxTestContext test) {
        registerBeforeTestLogin(new AuthCredentials("tester", "tester"), test)
                .onComplete(v -> {
                    userService.login(new AuthCredentials("tester", "tester"),
                            new OperationRequest(), test.succeeding(operationResponse -> {
                        test.verify(() -> {
                            assertSuccessResponse("text/plain", operationResponse);
                            assertNotNull(operationResponse.getPayload().toString());
                        });
                        test.completeNow();
                    }));
                });
    }

    @Test
    public void loginTestWrongUsername(VertxTestContext test) {
        AuthCredentials credentials = new AuthCredentials("tester2", "tester");
        registerBeforeTestLogin(credentials, test)
                .onComplete(v -> {
                    userService.login(credentials, new OperationRequest(), test.succeeding(operationResponse -> {
                        test.verify(() -> {
                            assertTextResponse(400,
                                    "Bad Request",
                                    "Wrong username or password", operationResponse);
                        });
                        test.completeNow();
                    }));
                });
    }

    @Test
    public void loginTestWrongPassword(VertxTestContext test) {
        AuthCredentials credentials = new AuthCredentials("tester", "tester");
        registerBeforeTestLogin(credentials, test)
                .onComplete(v -> {
                    userService.login(credentials.setPassword("tester2"),
                            new OperationRequest(), test.succeeding(operationResponse -> {
                        test.verify(() -> {
                            assertTextResponse(400,
                                    "Bad Request",
                                    "Wrong username or password", operationResponse);
                        });
                        test.completeNow();
                    }));
                });
    }

}
