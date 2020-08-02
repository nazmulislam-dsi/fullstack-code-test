package se.kry.codetest.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import io.vertx.ext.web.api.generator.WebApiServiceGen;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.service.impl.PollerServiceImpl;

@WebApiServiceGen
public interface PollerService {
    static PollerService create(PollerDao pollerDao) {
        return new PollerServiceImpl(pollerDao);
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
