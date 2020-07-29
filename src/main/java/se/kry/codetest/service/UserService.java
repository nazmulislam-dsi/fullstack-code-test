package se.kry.codetest.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.impl.UserServiceImpl;

@WebApiServiceGen
public interface UserService {

    void login(
            AuthCredentials body,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void register(
            AuthCredentials body,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    static UserService create(UserDao userDao, JWTAuth auth) {
        return new UserServiceImpl(userDao, auth);
    }

}
