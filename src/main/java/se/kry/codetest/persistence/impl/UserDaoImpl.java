package se.kry.codetest.persistence.impl;

import io.vertx.core.Future;
import io.vertx.ext.jdbc.JDBCClient;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.UserDao;

public class UserDaoImpl implements UserDao {
    JDBCClient jdbcClient;

    public UserDaoImpl(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Future<Boolean> addUser(AuthCredentials user) {
        return null;
    }

    @Override
    public Future<Boolean> userExists(AuthCredentials user) {
        return null;
    }
}
