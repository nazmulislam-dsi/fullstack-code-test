package se.kry.codetest.worker;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.utils.Constants;

import java.util.List;


public class PollerWorker extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(PollerWorker.class);
    private final WebClient client;
    private final PollerDao pollerDao;

    public PollerWorker(Vertx vertx, PollerDao pollerDao) {
        client = WebClient.create(vertx);
        this.pollerDao = pollerDao;
    }

    @Override
    public void start(Promise<Void> done) {
        LOG.info("NILOG::Starting Worker.");
        MessageConsumer<String> consumer = vertx.eventBus().consumer(Constants.MESSAGE_FOR_WORKER_NAME);
        consumer.handler(m -> {
            try{
                LOG.info("NILOG::Worker will start updating status..");
                getServiceListDatabase().onComplete(event -> {
                    LOG.info("Update all service status has finished.");
                });
            } catch (EncodeException e) {
                e.printStackTrace();
                m.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                        "Failed to get status of poller.");
            }
        });
        LOG.info("NILOG::Worker started.");
        done.complete();
    }
    private Future<Boolean> updateServiceStatusInDatabase(Integer id, String status ){
        Promise<Boolean> promise = Promise.promise();
        pollerDao.updateServiceStatus(id, status).onComplete(databaseResult -> {
            LOG.info("NILOG::Service status updated. ID: " + id + " Status: " + status);
            promise.complete(true);
        });
        return promise.future();
    }
    private Future<Boolean> getServiceListDatabase(){
        Promise<Boolean> promise = Promise.promise();
        pollerDao.getServiceList(null, null,null).onComplete(getServiceList->{
            if(getServiceList.succeeded()){
                List<Service> serviceList = getServiceList.result();
                if(serviceList!=null && serviceList.size()>0){
                    //vertx.executeBlocking(future -> {
                        LOG.info("NILOG::Total services found: "+serviceList.size());
                        for(Service service: serviceList){
                            String url = service.getUrl();
                            Integer id = Long.valueOf(service.getId()).intValue();
                            try {
                                client.getAbs(url)
                                        .send(response -> {
                                            String status = "FAIL";
                                            if (response.succeeded()) {
                                                status = (200 == response.result().statusCode() ? "OK" : "FAIL");
                                            }
                                            updateServiceStatusInDatabase(id,status).onComplete(event -> {
                                                LOG.info("NILOG::Returned from updateServiceStatusInDatabase.");
                                            });
                                        });
                            } catch (Exception e) {
                                LOG.error("NILOG::Failed to test the URL: " + url, e);
                                String status = "FAIL";
                                updateServiceStatusInDatabase(id,status).onComplete(event -> {
                                    LOG.info("NILOG::Returned from updateServiceStatusInDatabase.");
                                });
                            }
                        }
                    //    future.complete();
                    //}, res -> {
                        LOG.info("NILOG::All status of services have been updated.");
                        promise.complete(true);
                    //});

                }else {
                    LOG.info("NILOG::No services found.");
                    promise.complete();
                }
            }
        });
        return promise.future();
    }
}
