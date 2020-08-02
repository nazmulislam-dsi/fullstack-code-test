package se.kry.codetest.persistence.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.model.User;
import se.kry.codetest.persistence.UserDao;
import se.kry.codetest.service.UserService;

import java.util.List;

public class UserDaoImpl implements UserDao {
    private static final Logger LOG = LoggerFactory.getLogger(UserDaoImpl.class);

    JDBCClient jdbcClient;

    public UserDaoImpl(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Future<Boolean> addUser(AuthCredentials user) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String sql = "INSERT INTO USERS (username, password) VALUES (?, ?)";
                connection.queryWithParams(sql,
                        new JsonArray().add(user.getUsername()).add(user.getPassword()),
                        result -> {
                            try {
                                if (result.failed()) {
                                    LOG.error("NILOG::", result.cause());
                                    promise.fail(result.cause());
                                } else {
                                    LOG.info("NILOG::Inserted into database.");
                                    promise.complete(true);
                                }
                            } catch (Exception ex) {
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
    public Future<Boolean> userInfoMatched(AuthCredentials user) {
        Promise<Boolean> promise = Promise.promise();
        jdbcClient.getConnection(res -> {
            if (res.succeeded()) {
                SQLConnection connection = res.result();
                String sql = "SELECT * FROM USERS WHERE username = ?";
                connection.queryWithParams(sql,
                        new JsonArray().add(user.getUsername()),
                        result -> {
                            try {
                                if (result.failed()) {
                                    throw new RuntimeException("Error occurred while executing the query.");
                                }
                                Boolean matched = false;
                                List<JsonObject> jsonObject = result.result().getRows();
                                LOG.info("NILOG::User From DB Size::" + jsonObject.size());
                                if (jsonObject.size() == 1) {
                                    User userFromDb = new User(jsonObject.get(0));
                                    LOG.info("NILOG::Password provided::" + user.getPassword());
                                    LOG.info("NILOG::Password from DB::" + userFromDb.getPassword());
                                    if (userFromDb.getPassword().equals(user.getPassword())) {
                                        matched = true;
                                    } else {
                                        LOG.info("NILOG::Could not find any user with given information.");
                                    }
                                } else {
                                    LOG.info("NILOG::Could not find any user with given information.");
                                }
                                promise.complete(matched);
                            } catch (Exception ex) {
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
