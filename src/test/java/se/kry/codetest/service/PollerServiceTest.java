package se.kry.codetest.service;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
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
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.ServicePollerDao;
import se.kry.codetest.persistence.UserDao;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static se.kry.codetest.utils.TestUtils.*;
import static se.kry.codetest.utils.TestUtils.wipeDatabase;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PollerServiceTest extends BaseServicesTest {
    private static final Logger LOG = LoggerFactory.getLogger(PollerServiceTest.class);

    private ServicePollerDao servicePollerDao;
    private PollerService pollerService;
    private OperationRequest loggedContext;

    @Override
    @BeforeAll
    public void beforeAll(Vertx vertx, VertxTestContext testContext) throws Exception {
        super.beforeAll(vertx, testContext);
        setupDatabase(vertx, conf).onComplete(database -> {
            if (database.succeeded()) {
                jdbcClient = database.result();
                setupSchema(vertx, testContext, jdbcClient).onComplete(schema -> {
                    if (schema.succeeded()) {
                        if (schema.result()) {
                            setupAuth(vertx, testContext, conf).onComplete(jwtAuth -> {
                                if (jwtAuth.succeeded()) {
                                    auth = jwtAuth.result();
                                    userDao = UserDao.create(jdbcClient);
                                    userService = UserService.create(userDao, auth);
                                    servicePollerDao = ServicePollerDao.create(jdbcClient);
                                    pollerService = PollerService.create(servicePollerDao);
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
                        .compose(v -> registerBeforeTestLogin(new AuthCredentials("tester",
                                "tester"), testContext))
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
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
                    test.verify(() -> {
                        assertSuccessCreateResponse("application/json", res);
                        statusCheck.flag();
                        Service service = new Service(res.getPayload().toJsonObject());
                        assertNotNull(service.getId());
                        assertNotNull(service.getCreatedDate());
                        assertNotNull(service.getUserId());
                        assertEquals("Google", service.getName());
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
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            pollerService.createService(servicePostDTO, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertBadRequestResponse(resAgain,
                            "Invalid request with service name: " + servicePostDTO.getName());
                });
                secondStatusCheck.flag();
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createServiceInvalidURLServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "google");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertBadRequestResponse(res, "Invalid request with url: "
                        + servicePostDTO.getUrl());
                statusCheck.flag();
            });
            pollerService.createService(servicePostDTO, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertBadRequestResponse(resAgain,
                            "Invalid request with url: " + servicePostDTO.getUrl());
                });
                secondStatusCheck.flag();
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getServiceEmptyResTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        pollerService.getServiceList(null, null, loggedContext,
                test.succeeding(resGet -> {
                    LOG.info("NILOG::test response for getServiceTest::" + resGet.getPayload().toString());
                    try {
                        assertEmptyResponse(resGet);
                        String serviceList = resGet.getPayload().toString();
                        assertEquals("", serviceList);
                        statusCheck.flag();
                    } catch (Exception e) {
                        e.printStackTrace();
                        test.failNow(e);
                    }
                }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint getReqCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            ServicePostDTO servicePostDTOLinkedIn = new ServicePostDTO("LinkedIn", "https://linkedin.com");
            pollerService.createService(servicePostDTOLinkedIn, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertSuccessCreateResponse("application/json", resAgain);
                    secondStatusCheck.flag();
                    pollerService.getServiceList(null, null, loggedContext,
                            test.succeeding(resGet -> {
                                LOG.info("NILOG::test response for getServiceTest::" + resGet.getPayload().toString());
                                List<Service> serviceList = null;
                                try {
                                    serviceList = resGet.getPayload().toJsonArray().getList();
                                    assertEquals(2, serviceList.size());
                                    getReqCheck.flag();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    test.failNow(e);
                                }
                            }));
                });
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createServiceWithSameUserSameNameTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            loggedContext = new OperationRequest().setUser(new JsonObject().put("userId", "anotherTester"));
            ServicePostDTO servicePostDTOLinkedIn = new ServicePostDTO("Google", "https://google.com");
            pollerService.createService(servicePostDTOLinkedIn, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertBadRequestResponse(resAgain);
                    secondStatusCheck.flag();
                });
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void createServiceWithDiffUserButSameNameTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint getReqCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            AuthCredentials credentials = new AuthCredentials("anotherTester", "anotherTester");
            registerBeforeTestLogin(credentials, test)
                    .onComplete(v -> {
                        loggedContext = new OperationRequest().setUser(new JsonObject().put("userId", "anotherTester"));
                        ServicePostDTO servicePostDTOLinkedIn = new ServicePostDTO("Google", "https://google.com");
                        pollerService.createService(servicePostDTOLinkedIn, loggedContext, test.succeeding(resAgain -> {
                            test.verify(() -> {
                                assertSuccessCreateResponse("application/json", resAgain);
                                secondStatusCheck.flag();
                                pollerService.getServiceList(null, "Google", loggedContext,
                                        test.succeeding(resGet -> {
                                            LOG.info("NILOG::test response for getServiceTestWithName::" + resGet.getPayload()
                                                    .toString());
                                            List<Service> serviceList = null;
                                            try {
                                                serviceList = resGet.getPayload().toJsonArray().stream()
                                                        .map(r -> new Service((JsonObject) r)).collect(Collectors.toList());
                                                assertEquals(1, serviceList.size());
                                                assertEquals("Google", serviceList.get(0).getName());
                                                getReqCheck.flag();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                test.failNow(e);
                                            }
                                        }));
                            });
                        }));
                    });
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void getServiceFilterTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint getReqCheck = test.checkpoint();
        Checkpoint getReqCheckIDFilter = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            ServicePostDTO servicePostDTOLinkedIn = new ServicePostDTO("LinkedIn", "https://linkedin.com");
            pollerService.createService(servicePostDTOLinkedIn, loggedContext, test.succeeding(resAgain -> {
                test.verify(() -> {
                    assertSuccessCreateResponse("application/json", resAgain);
                    Service service = new Service(res.getPayload().toJsonObject());
                    Long idToFetchLater = service.getId();
                    secondStatusCheck.flag();
                    pollerService.getServiceList(null, "Google", loggedContext,
                            test.succeeding(resGet -> {
                                LOG.info("NILOG::test response for getServiceTestWithName::" + resGet.getPayload()
                                        .toString());
                                List<Service> serviceList = null;
                                try {
                                    serviceList = resGet.getPayload().toJsonArray().stream()
                                            .map(r -> new Service((JsonObject) r)).collect(Collectors.toList());
                                    assertEquals(1, serviceList.size());
                                    assertEquals("Google", serviceList.get(0).getName());
                                    getReqCheck.flag();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    test.failNow(e);
                                }
                            }));
                    pollerService.getServiceList(new Long(idToFetchLater).intValue(), null,
                            loggedContext,
                            test.succeeding(resGet -> {
                                LOG.info("NILOG::test response for getServiceTest::" + resGet.getPayload().toString());
                                List<Service> serviceList = null;
                                try {
                                    serviceList = resGet.getPayload().toJsonArray().stream()
                                            .map(r -> new Service((JsonObject) r)).collect(Collectors.toList());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    test.failNow(e);
                                }
                                assertEquals(1, serviceList.size());
                                assertEquals("Google", serviceList.get(0).getName());
                                getReqCheckIDFilter.flag();
                            }));
                });
            }));
        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void updateServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint updateReqCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            Service service = new Service(res.getPayload().toJsonObject());
            Long idToFetchLater = service.getId();
            ServicePutDTO servicePutDTO = new ServicePutDTO().setName("Google-EDIT");
            pollerService.updateService(new Long(idToFetchLater).intValue(), servicePutDTO, loggedContext,
                    test.succeeding(resAgain -> {
                        test.verify(() -> {
                            assertSuccessResponse("application/json", resAgain);
                            secondStatusCheck.flag();
                            LOG.info("NILOG::test response for updateServiceTest::" + resAgain.getPayload().toString());
                            Service updatedService = new Service(resAgain.getPayload().toJsonObject());
                            assertEquals(idToFetchLater, updatedService.getId());
                            assertNotNull(service.getCreatedDate());
                            assertEquals("tester", updatedService.getUserId());
                            assertEquals("Google-EDIT", updatedService.getName());
                            assertEquals("https://google.com", updatedService.getUrl());
                            updateReqCheck.flag();
                        });
                    }));

        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void updateServiceWithWrongUserTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            Service service = new Service(res.getPayload().toJsonObject());
            Long idToFetchLater = service.getId();
            ServicePutDTO servicePutDTO = new ServicePutDTO().setName("Google-EDIT");
            loggedContext = new OperationRequest().setUser(new JsonObject().put("userId", "anotherTester"));
            pollerService.updateService(new Long(idToFetchLater).intValue(), servicePutDTO, loggedContext,
                    test.succeeding(resAgain -> {
                        test.verify(() -> {
                            assertBadRequestResponse(resAgain,
                                    "Provided information does not matched");
                            secondStatusCheck.flag();

                        });
                    }));

        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void deleteServiceTest(VertxTestContext test) throws InterruptedException {
        Checkpoint statusCheck = test.checkpoint();
        Checkpoint secondStatusCheck = test.checkpoint();
        Checkpoint thirdStatusCheck = test.checkpoint();
        ServicePostDTO servicePostDTO = new ServicePostDTO("Google", "https://google.com");
        pollerService.createService(servicePostDTO, loggedContext, test.succeeding(res -> {
            test.verify(() -> {
                assertSuccessCreateResponse("application/json", res);
                statusCheck.flag();
            });
            Service service = new Service(res.getPayload().toJsonObject());
            Long idToFetchLater = service.getId();
            pollerService.deleteAllService(loggedContext,
                    test.succeeding(resAgain -> {
                        test.verify(() -> {
                            assertEmptyResponse(resAgain);
                            secondStatusCheck.flag();
                            pollerService.getServiceList(new Long(idToFetchLater).intValue(), null,
                                    loggedContext,
                                    test.succeeding(getRes -> {
                                        assertEmptyResponse(resAgain);
                                        thirdStatusCheck.flag();
                                    }));
                        });
                    }));

        }));
        test.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }
}
