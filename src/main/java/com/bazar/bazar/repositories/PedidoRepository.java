package com.bazar.bazar.repositories;

import com.bazar.bazar.model.ItemPedido;
import com.bazar.bazar.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, UUID> {
    List<Pedido> findByClienteIdOrVendedorId(UUID clienteId, UUID vendedorId);
}