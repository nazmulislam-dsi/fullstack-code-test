package se.kry.codetest.service.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.api.OperationRequest;
import io.vertx.ext.web.api.OperationResponse;
import org.apache.commons.validator.routines.UrlValidator;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.exception.BadRequestException;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.PollerDao;
import se.kry.codetest.service.PollerService;

import java.util.stream.Collectors;

public class PollerServiceImpl implements PollerService {
    private static final Logger LOG = LoggerFactory.getLogger(PollerService.class);

    PollerDao pollerDao;

    public PollerServiceImpl(PollerDao pollerDao) {
        this.pollerDao = pollerDao;
    }

    @Override
    public void getServiceList(Integer serviceId, String serviceName,
                               OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::getServiceList has called.");
        pollerDao.getServiceList(context.getUser().getString("userId"), serviceId, serviceName)
                .onComplete(event -> {
                    if (event.succeeded()) {
                        if (event.result().size() > 0) {
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
                                            .setStatusCode(204)
                                            .setStatusMessage("No Content")
                                            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                            .setPayload(Buffer.buffer("")))
                            );
                        }
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
        pollerDao.deleteAllService(context.getUser().getString("userId")).onComplete(ar -> {
            if (ar.succeeded()) {
                resultHandler.handle(Future.succeededFuture(
                        new OperationResponse()
                                .setStatusCode(204)
                                .setStatusMessage("No Content")
                                .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                .setPayload(Buffer.buffer(""))));
            } else {
                if (ar.cause() instanceof BadRequestException) {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer(ar.cause().getMessage()))));
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
    public void createService(ServicePostDTO body,
                              OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::createService has called.");
        LOG.info("NILOG::servicePostDTO" + body.toString());
        UrlValidator urlValidator = new UrlValidator();
        if (urlValidator.isValid(body.getUrl())) {
            pollerDao.addService(context.getUser().getString("userId"), body).onComplete(ar -> {
                if (ar.succeeded()) {
                    if (ar.result() == null) {
                        LOG.warn("ServicePoller with is name exist. ServicePoller name: " + body.getName());
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(400)
                                        .setStatusMessage("Bad Request")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                        .setPayload(
                                                Buffer.buffer("Invalid request with service name: " + body.getName())))
                        );
                    } else {
                        LOG.info("ServicePoller successfully created: {}", ar.result());
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(201)
                                        .setStatusMessage("Created")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                        .setPayload(ar.result().toJson().toBuffer()))
                        );
                    }
                } else {
                    if (ar.cause() instanceof BadRequestException) {
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(400)
                                        .setStatusMessage("Bad Request")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                        .setPayload(Buffer.buffer(ar.cause().getMessage()))));
                    } else if (ar.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                        resultHandler.handle(Future.succeededFuture(
                                new OperationResponse()
                                        .setStatusCode(400)
                                        .setStatusMessage("Bad Request")
                                        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                        .setPayload(Buffer.buffer("Invalid request with service name: "
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
        } else {
            resultHandler.handle(Future.succeededFuture(
                    new OperationResponse()
                            .setStatusCode(400)
                            .setStatusMessage("Bad Request")
                            .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                            .setPayload(Buffer.buffer("Invalid request with url: "
                                    + body.getUrl()))));
        }
    }

    @Override
    public void updateService(Integer serviceId, ServicePutDTO body,
                              OperationRequest context, Handler<AsyncResult<OperationResponse>> resultHandler) {
        LOG.info("NILOG::updateService has called.");
        LOG.info("NILOG::servicePutDTO" + body.toString());
        LOG.info("NILOG::userId::" + context.getUser().getString("userId"));
        pollerDao.updateServiceName(context.getUser().getString("userId"), serviceId, body).onComplete(ar -> {
            if (ar.succeeded()) {
                if (ar.result() == null) {
                    LOG.warn("ServicePoller with is name exist. ServicePoller name: " + body.getName());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(
                                            Buffer.buffer("Invalid request with service name: " + body.getName())))
                    );
                } else {
                    LOG.info("ServicePoller successfully updated: {}", ar.result());
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(200)
                                    .setStatusMessage("OK")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
                                    .setPayload(ar.result().toJson().toBuffer()))
                    );
                }
            } else {
                if (ar.cause() instanceof BadRequestException) {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer(ar.cause().getMessage()))));
                } else if (ar.cause() instanceof JdbcSQLIntegrityConstraintViolationException) {
                    resultHandler.handle(Future.succeededFuture(
                            new OperationResponse()
                                    .setStatusCode(400)
                                    .setStatusMessage("Bad Request")
                                    .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "text/plain")
                                    .setPayload(Buffer.buffer("Invalid request with service name: "
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
