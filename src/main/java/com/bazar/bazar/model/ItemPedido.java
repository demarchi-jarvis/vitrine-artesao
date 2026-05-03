package com.bazar.bazar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.UUID;

@Entity
@Table(name = "item_pedido")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pedido_id", nullable = false)
    @JsonIgnore
  //  @NotNull(message = "O pedido não pode ser nulo")
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "produto_id", nullable = false)
    @JsonIgnore
//    @NotNull(message = "O produto não pode ser nulo")
    private Produto produto;

    @ManyToOne   //  @NotNull(message = "O vendedor não pode ser nulo")
    @JoinColumn(name = "vendedor_id", nullable = false) //     @Column(name = "vendedor_id")
    @JsonIgnore
    private Usuario vendedor;


    @ManyToOne  //  @NotNull(message = "O comprador não pode ser nulo")
    @JoinColumn(name = "comprador_id", nullable = false)//    @Column(name = "comprador_id")
    @JsonIgnore
    private Usuario comprador;

    @Column(nullable = false)
  //  @Positive(message = "A quantidade deve ser maior que zero")
    private int quantidade;

    public ItemPedido(Pedido pedido, Produto produto, int quantidade, Usuario vendedor, Usuario comprador) {
        this.pedido = pedido;
        this.produto = produto;
        this.quantidade = quantidade;
        this.vendedor = vendedor;
        this.comprador = comprador;
    }
}