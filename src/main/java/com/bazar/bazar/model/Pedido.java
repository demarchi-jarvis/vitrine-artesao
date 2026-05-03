package com.bazar.bazar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList; 
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY) 
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id", nullable = false)
    private Usuario vendedor;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "endereco_entrega", nullable = false)
    private Endereco enderecoEntrega;

    private Boolean remote;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataEntrega;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> itens = new ArrayList<>(); 

    public void addItem(ItemPedido item) {
        itens.add(item);
        item.setPedido(this);
    }

    public void removeItem(ItemPedido item) {
        itens.remove(item);
        item.setPedido(null);
    }

    public Pedido(Usuario cliente, Usuario vendedor) {
        this.cliente = cliente;
        this.vendedor = vendedor;
        this.dataCriacao = LocalDateTime.now();
        this.remote = false;
    }
}   