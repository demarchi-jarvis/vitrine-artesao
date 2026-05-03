package com.bazar.bazar.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Table(name= "produto")
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Produto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    private String nome;
    private double preco;
    @ManyToOne
    @JoinColumn(name = "categoria_id")//    @Column(name = "categoria_id")
    private Categoria categoria;
    @ManyToOne
    @JoinColumn(name = "autor_id")//@Column(name = "autor_id")
    private Usuario autor;
    private int quantidade;
    private String imagem;
    private String descricao;
    private String icone;
    private LocalDateTime dataCriacao;

}