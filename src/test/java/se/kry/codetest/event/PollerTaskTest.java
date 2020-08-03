package se.kry.codetest.event;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.event.impl.PollerTaskImpl;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.BaseServicesTest;
import se.kry.codetest.service.PollerService;
import se.kry.codetest.service.PollerServiceTest;
import se.kry.codetest.service.UserService;
import se.kry.codetest.worker.PollerWorker;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static se.kry.codetest.utils.TestUtils.*;

@ExtendWith(VertxExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PollerTaskTest extends BaseServicesTest {
    private static final Logger LOG = LoggerFactory.getLogger(PollerTaskTest.class);

    private PollerDao pollerDao;
    private PollerTask pollerTask;
    private PollerWorker pollerWorker;
    private String workerId;

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
                                    pollerDao = PollerDao.create(jdbcClient);
                                    pollerTask = new PollerTaskImpl(vertx,pollerDao);
                                    pollerWorker = new PollerWorker(vertx,pollerDao);
                                    vertx.deployVerticle(pollerWorker, id -> {
                                        if(id.failed()){
                                            testContext.failNow(id.cause());
                                        }else{
                                            workerId = id.result();
                                        }
                                    });
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

        wipeDatabase(jdbcClient)
                .compose(v -> registerBeforeTestLogin(new AuthCredentials("tester",
                        "tester"), testContext).onComplete(authCreate->{
                    ServicePostDTO servicePostDTO =
                            new ServicePostDTO("Google", "https://google.com");
                    pollerDao.addService("tester",servicePostDTO).onComplete(serviceCreate ->{
                        testContext.completeNow();
                    });
                }));
        testContext.awaitCompletion(1000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void checkUpdateServiceStatusWithSchedulerMethod(Vertx vertx, VertxTestContext test) throws InterruptedException {

        Checkpoint statusCheck = test.checkpoint();
        Checkpoint payloadCheck = test.checkpoint();
        pollerTask.updateStatusOfServiceByPolling(event -> {
            assertTrue(event.succeeded());
            statusCheck.flag();
            vertx.setPeriodic(3000, id -> {
                pollerDao.getServiceList("tester",null,"Google").onComplete(data->{
                    LOG.info("checking status.");
                    if(data.succeeded()){
                        List<Service> serviceList = data.result();
                        if(data.result()!=null && serviceList.size()>0){
                            Service service = serviceList.get(0);
                            if(service.getStatus().equals("FAIL") || service.getStatus().equals("OK")){
                                payloadCheck.flag();
                                vertx.cancelTimer(id);
                            }
                        }else{
                            test.failNow(data.cause());
                        }
                    }else{
                        test.failNow(data.cause());
                    }
                });
            });

        });
        test.awaitCompletion(10000, TimeUnit.MILLISECONDS);
    }

    @AfterAll
    public void afterAll(Vertx vertx, VertxTestContext testContext) throws Exception {
        vertx.undeploy(workerId,event -> {
            if(event.succeeded()){
                testContext.completeNow();
            }else{
                testContext.failNow(event.cause());
            }
        });
    }
}
