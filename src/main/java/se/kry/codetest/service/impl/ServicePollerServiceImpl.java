package se.kry.codetest.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.ServicePollerDao;
import se.kry.codetest.service.ServicePollerService;

import java.util.stream.Collectors;

public class ServicePollerServiceImpl implements ServicePollerService {
    private static final Logger LOG = LoggerFactory.getLogger(ServicePollerService.class);

    ServicePollerDao servicePollerDao;
    JWTAuth auth;

    public ServicePollerServiceImpl(ServicePollerDao servicePollerDao, JWTAuth auth) {
        this.servicePollerDao = servicePollerDao;
        this.auth = auth;
    }

    @Override
    public void getServiceList(Integer serviceId, String serviceName,
                               OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::getServiceList has called.");
        servicePollerDao.getServiceList(context.getUser().getString("userId"),serviceId,serviceName)
                .onComplete(event -> {
                    if(event.succeeded()){
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(200)
                                        .setStatusMessage("Operation successful")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                        .setPayload(
                                                new JsonArray(event.result().stream().map(Service::toJson)
                                                        .collect(Collectors.toList())).toBuffer()))
                        );
                    } else {
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(500)
                                        .setStatusMessage("Internal server error")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                        .setPayload(Buffer.buffer(""))));
                    }
                });
    }

    @Override
    public void deleteAllService(OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {

    }

    @Override
    public void createService(ServicePostDTO body,
                              OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::createService has called.");
        LOG.info("NILOG::servicePostDTO"+body.toString());
        servicePollerDao.addService(context.getUser().getString("userId"),body).onComplete(ar -> {
            if (ar.succeeded()) {
                if (ar.result()==null) {
                    LOG.warn("Poller with is name exist. Poller name: " + body.getName());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(
                                            Buffer.buffer("Invaild request with service name: " + body.getName())))
                    );
                } else {
                    LOG.info("Poller successfully created: {}", ar.result());
                    resultHandler.handle(Future.succeededFuture(
                        new OperationResponse()
                                .setStatusCode(201)
                                .setStatusMessage("Created")
                                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                .setPayload(ar.result().toJson().toBuffer()))
                    );
                }
            } else {
                if(ar.cause() instanceof JdbcSQLIntegrityConstraintViolationException){
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("Invaild request with service name: "
                                            + body.getName()))));
                } else {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(500)
                                    .setStatusMessage("Internal server error")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer(""))));
                }
            }
        });
    }

    @Override
    public void updateService(Integer serviceId, ServicePutDTO body,
                              OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::updateService has called.");
        LOG.info("NILOG::servicePutDTO"+body.toString());
        servicePollerDao.updateServiceName(context.getUser().getString("userId"),serviceId,body).onComplete(ar -> {
            if (ar.succeeded()) {
                if (ar.result()==null) {
                    LOG.warn("Poller with is name exist. Poller name: " + body.getName());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(
                                            Buffer.buffer("Invaild request with service name: " + body.getName())))
                    );
                } else {
                    LOG.info("Poller successfully created: {}", ar.result());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(201)
                                    .setStatusMessage("Created")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                    .setPayload(ar.result().toJson().toBuffer()))
                    );
                }
            } else {
                if(ar.cause() instanceof JdbcSQLIntegrityConstraintViolationException){
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("Invaild request with service name: "
                                            + body.getName()))));
                } else {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(500)
                                    .setStatusMessage("Internal server error")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer(""))));
                }
            }
        });
    }
}
