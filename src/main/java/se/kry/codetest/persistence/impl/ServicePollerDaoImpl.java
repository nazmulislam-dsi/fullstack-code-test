package se.kry.codetest.persistence.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.exception.BadRequestException;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.ServicePollerDao;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ServicePollerDaoImpl implements ServicePollerDao {
    private static final Logger LOG = LoggerFactory.getLogger(ServicePollerDaoImpl.class);

    JDBCClient jdbcClient;

    public ServicePollerDaoImpl(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public static String conditionIfHasParameter(Boolean hasParameter) {
        if (hasParameter) {
            return " AND ";
        } else {
            return " WHERE ";
        }

    }

    @Override
    public Future<Service> addService(String userId, ServicePostDTO servicePostDTO) {
        Promise<Service> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String currentTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                LOG.info("NILOG::currentTime::" + currentTime);
                String sql = "INSERT INTO SERVICE (name, url, userId, createdDate) VALUES (?, ?, ?, '" + currentTime + "')";
                connection.updateWithParams(sql,
                        new JsonArray().add(servicePostDTO.getName()).add(servicePostDTO.getUrl()).add(userId),
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::ID from DB::" + result.result().getKeys());
                                    Long newServiceId;
                                    if (result.result().getKeys().size() > 0) {
                                        newServiceId = result.result().getKeys().getLong(0);
                                    } else {
                                        newServiceId = 0l;
                                        promise.fail(new BadRequestException("Provided information does not matched"));
                                    }
                                    String sqlGetServiceWithId = "SELECT * FROM SERVICE WHERE id = ?";
                                    connection.queryWithParams(sqlGetServiceWithId,
                                            new JsonArray().add(newServiceId),
                                            serviceGetResult -> {
                                                if (serviceGetResult.failed()) {
                                                    LOG.error("NILOG::", serviceGetResult.cause());
                                                    promise.fail(serviceGetResult.cause());
                                                } else {
                                                    List<JsonObject> jsonObjectList =
                                                            serviceGetResult.result().getRows();
                                                    if (jsonObjectList != null && jsonObjectList.size() == 1) {
                                                        LOG.info("NILOG::Inserted into database.");
                                                        Service service = new Service(jsonObjectList.get(0));
                                                        promise.complete(service);
                                                    } else {
                                                        promise.fail(new RuntimeException("Inserted failed."));
                                                    }
                                                }
                                            }
                                    );
                                }
                            } catch (Exception ex) {
                                LOG.error("NILOG", ex);
                                promise.fail(ex);
                            } finally {
                                connection.close();
                            }
                        });
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<List<Service>> getServiceList(String userId, Integer serviceId, String serviceName) {
        Promise<List<Service>> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                StringBuffer sqlGetServiceWithFilter
                        = new StringBuffer().append("SELECT * FROM SERVICE ");
                JsonArray params = new JsonArray();
                if (StringUtils.isNotEmpty(userId)) {
                    sqlGetServiceWithFilter.append(conditionIfHasParameter(false)).append("userId = ?");
                    params.add(userId);
                }
                if (0 != (serviceId == null ? 0 : serviceId < 0 ? 0 : serviceId)) {
                    sqlGetServiceWithFilter.append(conditionIfHasParameter(true)).append("id = ?");
                    params.add(serviceId);
                }
                if (StringUtils.isNotEmpty(serviceName)) {
                    sqlGetServiceWithFilter.append(conditionIfHasParameter(true)).append("name = ?");
                    params.add(serviceName);
                }
                connection.queryWithParams(sqlGetServiceWithFilter.toString(),
                        params,
                        serviceGetResult -> {
                            if (serviceGetResult.failed()) {
                                LOG.error("NILOG::", serviceGetResult.cause());
                                promise.fail(serviceGetResult.cause());
                            } else {
                                List<Service> serviceList = serviceGetResult.map(rs ->
                                        rs.getRows().stream().map(Service::new).collect(Collectors.toList())).result();
                                promise.complete(serviceList);
                            }
                        }
                );
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Service> updateServiceName(String userId, Integer id, ServicePutDTO servicePutDTO) {
        Promise<Service> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String sql = "UPDATE SERVICE SET name=? WHERE id=? AND userId=?";
                connection.updateWithParams(sql,
                        new JsonArray().add(servicePutDTO.getName()).add(id).add(userId),
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::ID from DB::" + result.result().getKeys());
                                    Long newServiceId;
                                    if (result.result().getKeys().size() > 0) {
                                        newServiceId = result.result().getKeys().getLong(0);
                                    } else {
                                        newServiceId = 0l;
                                        promise.fail(new BadRequestException("Provided information does not matched"));
                                    }
                                    String sqlGetServiceWithId = "SELECT * FROM SERVICE WHERE id = ?";
                                    connection.queryWithParams(sqlGetServiceWithId,
                                            new JsonArray().add(newServiceId),
                                            serviceGetResult -> {
                                                if (serviceGetResult.failed()) {
                                                    LOG.error("NILOG::", serviceGetResult.cause());
                                                    promise.fail(serviceGetResult.cause());
                                                } else {
                                                    List<JsonObject> jsonObjectList =
                                                            serviceGetResult.result().getRows();
                                                    if (jsonObjectList != null && jsonObjectList.size() == 1) {
                                                        LOG.info("NILOG::Updated into database.");
                                                        Service service = new Service(jsonObjectList.get(0));
                                                        promise.complete(service);
                                                    } else {
                                                        promise.fail(new RuntimeException("Update failed."));
                                                    }
                                                }
                                            }
                                    );
                                }
                            } catch (Exception ex) {
                                LOG.error("NILOG", ex);
                                promise.fail(ex);
                            } finally {
                                connection.close();
                            }
                        });
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Boolean> deleteAllService(String userId) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                StringBuffer sql = new StringBuffer().append("DELETE FROM SERVICE ");
                if (StringUtils.isNotBlank(userId)) {
                    sql.append(conditionIfHasParameter(false)).append("userId = '" + userId + "'");
                }
                connection.update(sql.toString(),
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::Response from DB::" + result.result().getKeys());
                                    promise.complete(true);
                                }
                            } catch (Exception ex) {
                                LOG.error("NILOG", ex);
                                promise.fail(ex);
                            } finally {
                                connection.close();
                            }
                        });
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    @Override
    public Future<Boolean> deleteAllService() {
        return deleteAllService(null);
    }

    @Override
    public Future<Boolean> updateServiceStatus(Integer id, String status) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String sql = "UPDATE SERVICE SET status=? WHERE id=?";
                connection.updateWithParams(sql,
                        new JsonArray().add(status).add(id),
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::ID from DB::" + result.result().getKeys());
                                    if (result.result().getKeys().size() > 0) {
                                        promise.complete(true);
                                    } else {
                                        promise.fail(new BadRequestException("Provided information does not matched"));
                                    }
                                }
                            } catch (Exception ex) {
                                LOG.error("NILOG", ex);
                                promise.fail(ex);
                            } finally {
                                connection.close();
                            }
                        });
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

}
