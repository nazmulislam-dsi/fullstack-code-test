package se.kry.codetest.persistence;

import io.vertx.core.Future;
import io.vertx.ext.jdbc.JDBCClient;
import se.kry.codetest.dto.ServicePostDTO;
import se.kry.codetest.dto.ServicePutDTO;
import se.kry.codetest.model.Service;
import se.kry.codetest.persistence.impl.ServicePollerDaoImpl;

import java.util.List;

public interface ServicePollerDao {

  Future<Service> addService(String userId, ServicePostDTO servicePostDTO);
  Future<List<Service>> getServiceList(String userId, Integer serviceId, String serviceName);
  Future<Service> updateServiceName(String userId, Integer id, ServicePutDTO servicePutDTO);
  Future<Boolean> deleteAllService(String userId);

  static ServicePollerDao create(JDBCClient jdbcClient) {
    return new ServicePollerDaoImpl(jdbcClient);
  }

}
