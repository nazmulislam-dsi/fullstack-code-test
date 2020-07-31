package se.kry.codetest.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.UserService;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;

public class UserServiceImpl implements UserService {
    private static final Logger LOG = LoggerFactory.getLogger(UserService.class);

    UserDao userDao;
    JWTAuth auth;

    public UserServiceImpl(UserDao userDao, JWTAuth auth) {
        this.userDao = userDao;
        this.auth = auth;
    }

    @Override
    public void login(AuthCredentials body, OperationRequest context,
                      Handler<AsyncResult<OperationResponse>> resultHandler) {
        body.hashPassword();
        userDao.userInfoMatched(body).onComplete(ar -> {
            if (ar.succeeded()) {
                if (ar.result()) {
                    LOG.info("Logged user {}", body.getUsername());
                    resultHandler.handle(Future.succeededFuture(
                            OperationResponse.completedWithPlainText(
                                    Buffer.buffer(generateToken(body.getUsername()))
                            )
                    ));
                } else {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("Wrong username or password")))
                    );
                }
            } else {
                resultHandler.handle(Future.succeededFuture(
                        new OperationResponse()
                                .setStatusCode(500)
                                .setStatusMessage("Bad Request")
                                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                .setPayload(Buffer.buffer(ar.cause().getMessage()))));
            }
        });
    }

    @Override
    public void register(AuthCredentials body, OperationRequest context,
                         Handler<AsyncResult<OperationResponse>> resultHandler) {
        body.hashPassword();
        userDao.addUser(body).onComplete(ar -> {
            if (ar.succeeded()) {
                if (!ar.result()) {
                    LOG.warn("User is trying to register again: " + body.getUsername());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("Username or password format is not correct")))
                    );
                } else {
                    LOG.info("User successfully registered: {}", body.getUsername());
                    resultHandler.handle(Future.succeededFuture(
                            OperationResponse.completedWithPlainText(
                                    Buffer.buffer(generateToken(body.getUsername())))
                    ));
                }
            } else {
                if(ar.cause() instanceof JdbcSQLIntegrityConstraintViolationException){
                    LOG.warn("User is trying to register again: " + body.getUsername());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("User " + body.getUsername() + " already exists.")))
                    );
                }else {
                    resultHandler.handle(Future.failedFuture(ar.cause()));
                }
            }
        });
    }

    private String generateToken(String userId) {
        return auth.generateToken(
                new JsonObject().put("userId", userId),
                new JWTOptions().setExpiresInMinutes(60)
                        .setIssuer("Poller Manager Backend")
                        .setSubject("Poller Manager API").setAlgorithm("RS256")
        );
    }
}
