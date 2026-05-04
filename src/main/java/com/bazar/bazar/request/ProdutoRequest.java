package com.bazar.bazar.request;

import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProdutoRequest {
    private String nome;
    private double preco;
    private int quantidade;
    private String imagem;
    private String icone;
    private String descricao;
    private UUID categoriaId;
}
