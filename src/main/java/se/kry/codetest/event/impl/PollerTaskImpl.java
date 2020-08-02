package se.kry.codetest.event.impl;

import io.vertx.core.json.JsonObject;
import se.kry.codetest.event.PollerTask;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.ServicePollerDao;
import se.kry.codetest.utils.Constants;

import java.util.List;

public class PollerTaskImpl implements PollerTask {
    private static final Logger LOG = LoggerFactory.getLogger(PollerTask.class);
    private ServicePollerDao servicePollerDao;
    Vertx vertx;

    public PollerTaskImpl(Vertx vertx, ServicePollerDao servicePollerDao) {
        this.servicePollerDao = servicePollerDao;
        this.vertx = vertx;
    }

    @Override
    public void updateStatusOfServiceByPolling(Handler<AsyncResult<Boolean>> handler) {
        servicePollerDao.getServiceList(null, null,null).onComplete(event->{
            if(event.succeeded()){
                List<Service> serviceList = event.result();
                if(serviceList.size()>0){
                    for(Service service: serviceList){
                        String data = new JsonObject()
                                .put("id", service.getId())
                                .put("url", service.getUrl())
                                .encode();
                        vertx.eventBus().request(Constants.MESSAGE_FOR_WORKER_NAME, data, replyEvent -> {
                            if(replyEvent.succeeded()){
                                JsonObject replyData = new JsonObject(replyEvent.result().body().toString());
                                LOG.info("NILOG::reply::"+replyData);
                                String status = replyData.getString("status");
                                Integer id = replyData.getInteger("id");
                                servicePollerDao.updateServiceStatus(id,status).onComplete(databaseResult->{
                                    LOG.info("NILOG::Service status updated. ID: "+id+" Status: "+status);
                                });
                            }else{
                                LOG.info("NILOG::",replyEvent.cause());
                            }
                        });
                    }
                    handler.handle(Future.succeededFuture(true));
                }else{
                    handler.handle(Future.succeededFuture(true));
                }
            }else{
                handler.handle(Future.failedFuture(event.cause()));
            }
        });
    }
}
