package com.bazar.bazar.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table(name = "endereco")
@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Endereco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String cidade;
    private String estado;
    private String cep;
    private String rua;
    private Long numero;
    private String adicional;
    private String bairro;
    private String complemento;

    @ManyToOne
    @JoinColumn(name = "usuario_id", referencedColumnName = "id")
    private Usuario usuario; 
}