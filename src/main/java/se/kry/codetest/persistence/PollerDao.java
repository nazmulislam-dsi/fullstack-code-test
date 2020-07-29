package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import se.kry.codetest.model.Poller;
import se.kry.codetest.persistence.impl.PollerDaoImpl;

import java.util.List;

public interface PollerDao {

  Future<Boolean> addPoller(Poller poller);
  Future<List<Poller>> getPollerList();
  Future<Poller> getPollerById(Long id);
  Future<List<Poller>> getPollerListByName(String name);
  Future<List<Poller>> getPollerListByUserId(String userId);
  Future<List<Poller>> getPollerListByNameUserId(String name, String userId);
  Future<Boolean> updatePollerName(long id, String name);
  Future<Boolean> deleteAllPollerByUserId(String userId);
  Future<Boolean> deleteAllPoller();

  static PollerDao create(JDBCClient jdbcClient) {
    return new PollerDaoImpl(jdbcClient);
  }

}
