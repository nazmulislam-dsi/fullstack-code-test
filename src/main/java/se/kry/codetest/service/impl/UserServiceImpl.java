package se.kry.codetest.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.UserService;

public class UserServiceImpl implements UserService {
    UserDao userDao;
    JWTAuth auth;

    public UserServiceImpl(UserDao userDao, JWTAuth auth) {
        this.userDao = userDao;
        this.auth = auth;
    }

    @Override
    public void login(AuthCredentials body, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void register(AuthCredentials body, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }
}
