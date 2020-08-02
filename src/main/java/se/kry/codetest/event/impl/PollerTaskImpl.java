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
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.utils.Constants;

import java.util.List;

public class PollerTaskImpl implements PollerTask {
    private static final Logger LOG = LoggerFactory.getLogger(PollerTask.class);
    private PollerDao pollerDao;
    Vertx vertx;

    public PollerTaskImpl(Vertx vertx, PollerDao pollerDao) {
        this.pollerDao = pollerDao;
        this.vertx = vertx;
    }

    @Override
    public void updateStatusOfServiceByPolling(Handler<AsyncResult<Boolean>> handler) {
        LOG.info("NILOG::updateStatusOfServiceByPolling called.");
        vertx.eventBus().send(Constants.MESSAGE_FOR_WORKER_NAME, null);
        LOG.info("NILOG::Passed the message to event bus.");
        handler.handle(Future.succeededFuture(true));
    }
}
