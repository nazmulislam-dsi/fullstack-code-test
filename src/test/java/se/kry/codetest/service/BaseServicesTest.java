package se.kry.codetest.service;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.junit5.VertxTestContext;
import se.kry.codetest.BaseTestGetEnvironment;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;

import static se.kry.codetest.utils.TestUtils.assertSuccessResponse;

public class BaseServicesTest extends BaseTestGetEnvironment {

    protected UserService userService;
    protected UserDao userDao;
    protected JDBCClient jdbcClient;
    protected JWTAuth auth;

    protected Future<String> registerBeforeTestLogin(AuthCredentials credentials, VertxTestContext test) {
        Promise<String> promise = Promise.promise();
        userService.register(credentials, new OperationRequest(), test.succeeding(operationResponse -> {
            test.verify(() -> {
                assertSuccessResponse("text/plain", operationResponse);
            });
            promise.complete(operationResponse.getPayload().toString());
        }));
        return promise.future();
    }
}
