package com.umc.pediupatrao.repository;

import com.umc.pediupatrao.entity.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
    List<Pedido> findByClienteId(String clienteId);
    List<Pedido> findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThan(Instant inicio, Instant fimExclusivo);
}
