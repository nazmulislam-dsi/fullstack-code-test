package se.kry.codetest.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.persistence.ServicePollerDao;
import se.kry.codetest.service.impl.ServicePollerServiceImpl;

@WebApiServiceGen
public interface ServicePollerService {
    static ServicePollerService create(ServicePollerDao servicePollerDao, JWTAuth auth) {
        return new ServicePollerServiceImpl(servicePollerDao, auth);
    }

    void getServiceList(Integer serviceId, String serviceName,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void deleteAllService(
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void createService(ServicePostDTO body,
                       OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

    void updateService(Integer serviceId, ServicePutDTO body,
            OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler);

}
