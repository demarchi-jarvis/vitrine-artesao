package com.bazar.bazar.repositories;

import com.bazar.bazar.model.ItemPedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemPedidoRepository extends JpaRepository<ItemPedido, UUID> {

    @Query(value = "SELECT ip FROM ItemPedido ip " +
            "JOIN FETCH ip.pedido p " +
            "JOIN FETCH p.cliente " +
            "JOIN FETCH p.vendedor " +
            "JOIN FETCH p.enderecoEntrega " +
            "JOIN FETCH ip.produto " +
            "WHERE p.vendedor.id = :vendedorId",
            countQuery = "SELECT COUNT(ip) FROM ItemPedido ip WHERE ip.pedido.vendedor.id = :vendedorId")
    Page<ItemPedido> buscarPorVendedorId(@Param("vendedorId") UUID vendedorId, Pageable pageable);

    @Query(value = "SELECT ip FROM ItemPedido ip " +
            "JOIN FETCH ip.pedido p " +
            "JOIN FETCH p.cliente " +
            "JOIN FETCH p.vendedor " +
            "JOIN FETCH p.enderecoEntrega " +
            "JOIN FETCH ip.produto " +
            "WHERE p.cliente.id = :clienteId",
            countQuery = "SELECT COUNT(ip) FROM ItemPedido ip WHERE ip.pedido.cliente.id = :clienteId")
    Page<ItemPedido> buscarPorCompradorId(@Param("clienteId") UUID clienteId, Pageable pageable);
}
