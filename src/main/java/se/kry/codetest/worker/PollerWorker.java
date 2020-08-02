package se.kry.codetest.worker;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.EncodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.utils.Constants;


public class PollerWorker extends AbstractVerticle {
    private static final Logger LOG = LoggerFactory.getLogger(PollerWorker.class);
    private final WebClient client;
    public PollerWorker(Vertx vertx) {
        client = WebClient.create(vertx);
    }

    @Override
    public void start(Promise<Void> done){
        MessageConsumer<String> consumer= vertx.eventBus().consumer(Constants.MESSAGE_FOR_WORKER_NAME);
        consumer.handler(m -> {
            JsonObject data = new JsonObject(m.body());
            try{
                String url = data.getString("url");
                Promise<JsonObject> statusFuture = Promise.promise();
                try {
                    client.getAbs(url)
                            .send(response -> {
                                if (response.succeeded()) {
                                    statusFuture.complete(data.put("status", 200 ==
                                            response.result().statusCode() ? "OK" : "FAIL"));
                                } else {
                                    statusFuture.complete(data.put("status", "FAIL"));
                                }
                                m.reply(data);
                            });
                } catch (Exception e) {
                    LOG.error("NILOG::Failed to test the URL: " + url, e);
                    statusFuture.complete(data.put("status", "FAIL"));
                    m.reply(data);
                }
            }catch (EncodeException e){
                m.fail(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(),
                        "Failed to get status of poller.");
            }
        });

    }
}
