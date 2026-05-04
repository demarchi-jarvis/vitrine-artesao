package com.bazar.bazar.response;

import com.bazar.bazar.model.Pedido;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Setter
@Getter
public class PedidoResponse {
    private UUID id;
    private UUID clienteId;
    private String clienteNome;
    private UUID vendedorId;
    private String vendedorNome;
    private UUID enderecoEntregaId;
    private Boolean remote;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEntrega;
    private List<ItemPedidoResponse> itens;

    public PedidoResponse(Pedido pedido) {
        this.id = pedido.getId();
        this.clienteId = pedido.getCliente().getId();
        this.clienteNome = pedido.getCliente().getNome();
        this.vendedorId = pedido.getVendedor().getId();
        this.vendedorNome = pedido.getVendedor().getNome();
        this.enderecoEntregaId = pedido.getEnderecoEntrega().getId();
        this.remote = pedido.getRemote();
        this.dataCriacao = pedido.getDataCriacao();
        this.dataEntrega = pedido.getDataEntrega();
        this.itens = pedido.getItens().stream()
                .map(ItemPedidoResponse::new)
                .collect(Collectors.toList());
    }
}
