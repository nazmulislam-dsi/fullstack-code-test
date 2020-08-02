package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.ext.jdbc.JDBCClient;
import se.kry.codetest.model.AuthCredentials;
import se.kry.codetest.persistence.impl.UserDaoImpl;

public interface UserDao {

    static UserDao create(JDBCClient jdbcClient) {
        return new UserDaoImpl(jdbcClient);
    }

    Future<Boolean> addUser(AuthCredentials user);

    Future<Boolean> userInfoMatched(AuthCredentials user);

}
