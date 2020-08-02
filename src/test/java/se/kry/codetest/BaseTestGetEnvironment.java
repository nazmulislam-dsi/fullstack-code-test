package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;

public abstract class BaseTestGetEnvironment {
    protected JsonObject conf;

    @BeforeAll
    public void beforeAll(Vertx vertx, VertxTestContext testContext) throws Exception {
        conf = new JsonObject(vertx.fileSystem().readFileBlocking("conf/test_config.json"));
    }
}
