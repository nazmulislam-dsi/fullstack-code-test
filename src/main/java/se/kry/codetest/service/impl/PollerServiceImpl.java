package se.kry.codetest.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import se.kry.codetest.dto.PollerPostDTO;
import se.kry.codetest.dto.PollerPutDTO;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.service.PollerService;

public class PollerServiceImpl implements PollerService {

    PollerDao pollerDao;
    JWTAuth auth;

    public PollerServiceImpl(PollerDao pollerDao, JWTAuth auth) {
        this.pollerDao = pollerDao;
        this.auth = auth;
    }

    @Override
    public void getPollerList(Integer pollerId, String userId, String pollerName, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void deleteAllPoller(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void getPollerListByUser(String userId, Integer pollerId, String pollerName, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void createPoller(String userId, PollerPostDTO pollerPostDTO, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void deleteAllPollerByUser(String userId, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void updatePoller(String userId, String pollerId, PollerPutDTO body, OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }
}
