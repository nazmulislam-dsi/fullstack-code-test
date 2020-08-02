package se.kry.codetest.event;

import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.codegen.annotations.VertxGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

@VertxGen
@ProxyGen
public interface PollerTask {

    static PollerTask createProxy(Vertx vertx, String address) {
        return new PollerTaskVertxEBProxy(vertx, address);
    }

    void updateStatusOfServiceByPolling(Handler<AsyncResult<Boolean>> handler);
}
