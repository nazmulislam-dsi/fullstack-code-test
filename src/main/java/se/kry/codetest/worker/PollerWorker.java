package se.kry.codetest.worker;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.utils.Constants;



public class PollerWorker extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(PollerWorker.class);
    private final WebClient client;
    private final PollerDao pollerDao;

    public PollerWorker(Vertx vertx, PollerDao pollerDao) {
        client = WebClient.create(vertx);
        this.pollerDao = pollerDao;
    }

    @Override
    public void start(Promise<Void> done){
        LOG.info("NILOG::Starting Worker.");
        MessageConsumer<String> consumer= vertx.eventBus().consumer(Constants.MESSAGE_FOR_WORKER_NAME);
        consumer.handler(m -> {
            JsonObject data = new JsonObject(m.body());
            String url = data.getString("url");
            try{
                client.getAbs(url)
                        .send(response -> {
                            String status = "FAIL";
                            if (response.succeeded()) {
                                status = (200 == response.result().statusCode() ? "OK" : "FAIL");
                            } else {
                                status = "FAIL";
                            }
                            //statusFuture.complete(data.put("status",status));
                            data.put("status",status);
                            LOG.error("NILOG:: Sending status: " + status);
                            m.reply(data);
                        });
            } catch (Exception e) {
                LOG.error("NILOG::Failed to test the URL: " + url, e);
                //statusFuture.complete(data.put("status", "FAIL"));
                m.fail(HttpResponseStatus.BAD_REQUEST.code(),e.getMessage());
            }

        });
        LOG.info("NILOG::Worker started.");
        done.complete();
    }
}
