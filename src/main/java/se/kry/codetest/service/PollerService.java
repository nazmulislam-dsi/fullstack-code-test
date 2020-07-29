package se.kry.codetest.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;
import se.kry.codetest.dto.PollerPostDTO;
import se.kry.codetest.dto.PollerPutDTO;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.service.impl.PollerServiceImpl;

@WebApiServiceGen
public interface PollerService {
    static PollerService create(PollerDao pollerDao, JWTAuth auth) {
        return new PollerServiceImpl(pollerDao, auth);
    }

    void getPollerList(Integer pollerId, String userId, String pollerName,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void deleteAllPoller(
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void getPollerListByUser(String userId, Integer pollerId, String pollerName,
                             OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void createPoller(String userId, PollerPostDTO pollerPostDTO,
                      OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void deleteAllPollerByUser(String userId,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void updatePoller(String userId, String pollerId, PollerPutDTO body,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

}
