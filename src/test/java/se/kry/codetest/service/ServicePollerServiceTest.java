package se.kry.codetest.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.ServicePollerDao;
import se.kry.codetest.persistence.UserDao;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.kry.codetest.utils.TestUtils.*;
import static se.kry.codetest.utils.TestUtils.wipeDatabase;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ServicePollerServiceTest extends BaseServicesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServicePollerServiceTest.class);

    private ServicePollerDao servicePollerDao;
    private ServicePollerService servicePollerService;
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
                                    servicePollerDao = ServicePollerDao.create(jdbcClient);
                                    servicePollerService = ServicePollerService.create(servicePollerDao,auth);
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
        testContext.assertComplete(
                wipeDatabase(jdbcClient)
                        .compose(v -> registerBeforeTestLogin(new AuthCredentials("tester", "tester"), testContext))
        ).onComplete(ar -> {
            loggedContext = new OperationRequest().setUser(new JsonObject().put("userId", "tester"));
            testContext.completeNow();
        });
        testContext.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint payloadCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google","https://google.com");
        servicePollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
                    test.verify(() -> {
                        assertSuccessCreateResponse("application/json", res);
                        statusCheck.flag();
                        Service service = new Service(res.getPayload().toJsonObject());
                        assertNotNull(service.getId());
                        assertNotNull(service.getCreatedDate());
                        assertNotNull(service.getUserId());
                        assertEquals("Google",  service.getName());
                        assertEquals("https://google.com", service.getUrl());
                        assertEquals("tester", service.getUserId());
                        payloadCheck.flag();
                    });
                })
        );
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createServiceAlreadyExistingServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google","https://google.com");
        servicePollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            servicePollerService.createService(servicePostDTO, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertTextResponse(400,
                            "Bad Request",
                            "Invaild request with service name: " + servicePostDTO.getName(), resAgain);
                        });
                    secondStatusCheck.flag();
                }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint getReqCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google","https://google.com");
        servicePollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            ServicePostDTO servicePostDTOLinkedIn = new ServicePostDTO("LinkedIn","https://linkedin.com");
            servicePollerService.createService(servicePostDTOLinkedIn, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertSuccessCreateResponse("application/json", res);
                    secondStatusCheck.flag();
                    servicePollerService.getServiceList(null,null,loggedContext,
                            test.succeeding(resGet -> {
                                LOG.info("NILOG::test response for getServiceTest::"+res.getPayload().toString());
                                List<Service> serviceList = null;
                                try {
                                    serviceList = getObjectListFromJsonString(resGet.getPayload().toString(), Service.class);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    test.failNow(e);
                                }
                                assertEquals(2,serviceList.size());
                                getReqCheck.flag();
                            }));
                });
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }
}
