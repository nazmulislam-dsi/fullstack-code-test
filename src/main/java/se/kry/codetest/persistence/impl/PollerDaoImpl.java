package se.kry.codetest.persistence.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import se.kry.codetest.model.Poller;
import se.kry.codetest.persistence.PollerDao;

import java.util.List;
import java.util.stream.Collectors;

public class PollerDaoImpl implements PollerDao {

    JDBCClient jdbcClient;

    public PollerDaoImpl(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }


    @Override
    public Future<Boolean> addPoller(Poller poller) {
        return null;
    }

    @Override
    public Future<List<Poller>> getPollerList() {
        return null;
    }

    @Override
    public Future<Poller> getPollerById(Long id) {
        return null;
    }

    @Override
    public Future<List<Poller>> getPollerListByName(String name) {
        return null;
    }

    @Override
    public Future<List<Poller>> getPollerListByUserId(String userId) {
        return null;
    }

    @Override
    public Future<List<Poller>> getPollerListByNameUserId(String name, String userId) {
        return null;
    }

    @Override
    public Future<Boolean> updatePollerName(long id, String name) {
        return null;
    }

    @Override
    public Future<Boolean> deleteAllPollerByUserId(String userId) {
        return null;
    }

    @Override
    public Future<Boolean> deleteAllPoller() {
        return null;
    }

    private Handler<AsyncResult<ResultSet>> generatePoolListHandler(Promise<List<Poller>> future) {
        return rs -> {
            if (rs.failed()) future.fail(rs.cause());
            rs.result().getRows().stream()
                    .map(Poller::new)
                    .collect(Collectors.toList());
        };
    }

    private void execute(SQLConnection conn, String sql, Handler<Void> done) {
        conn.execute(sql, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    private void query(SQLConnection conn, String sql, Handler<ResultSet> done) {
        conn.query(sql, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(res.result());
        });
    }

    private void startTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.setAutoCommit(false, res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

    private void endTx(SQLConnection conn, Handler<ResultSet> done) {
        conn.commit(res -> {
            if (res.failed()) {
                throw new RuntimeException(res.cause());
            }

            done.handle(null);
        });
    }

}
