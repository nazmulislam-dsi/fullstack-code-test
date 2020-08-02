package se.kry.codetest.event.impl;

import io.vertx.core.json.JsonObject;
import org.apache.commons.lang3.StringUtils;
import se.kry.codetest.event.PollerTask;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.utils.Constants;

import java.util.List;

public class PollerTaskImpl implements PollerTask {
    private static final Logger LOG = LoggerFactory.getLogger(PollerTask.class);
    private final PollerDao pollerDao;
    private final Vertx vertx;

    public PollerTaskImpl(Vertx vertx, PollerDao pollerDao) {
        this.pollerDao = pollerDao;
        this.vertx = vertx;
    }

    @Override
    public void updateStatusOfServiceByPolling(Handler<AsyncResult<Boolean>> handler) {
        LOG.info("NILOG::Called to update service status.");
        pollerDao.getServiceList(null, null,null).onComplete(event->{
            if(event.succeeded()){
                List<Service> serviceList = event.result();
                LOG.info("NILOG::Total services found: "+serviceList.size());
                if(serviceList!=null && serviceList.size()>0){
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
                                if(StringUtils.isNotBlank(status)){
                                    pollerDao.updateServiceStatus(id,status).onComplete(databaseResult->{
                                        LOG.info("NILOG::Service status updated. ID: "+id+" Status: "+status);
                                    });
                                }else{
                                    LOG.info("NILOG::Service status can not be empty or null.");
                                }
                            }else{
                                LOG.info("NILOG::",replyEvent.cause());
                            }
                        });
                    }
                    LOG.info("NILOG::All status of services have been updated.");
                    handler.handle(Future.succeededFuture(true));
                }else{
                    LOG.info("NILOG::No services found.");
                    handler.handle(Future.succeededFuture(true));
                }
            }else{
                LOG.info("NILOG::Database returned error.");
                handler.handle(Future.failedFuture(event.cause()));
            }
        });
    }
}
