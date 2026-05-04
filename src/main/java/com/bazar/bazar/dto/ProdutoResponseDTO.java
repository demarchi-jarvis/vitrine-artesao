package com.bazar.bazar.dto;

import com.bazar.bazar.model.Categoria;
import com.bazar.bazar.model.Produto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProdutoResponseDTO {

    private UUID id;
    private String nome;
    private double preco;
    private int quantidade;
    private String imagem;
    private String icone;
    private Categoria categoria;
    private UsuarioDTO autor;
    private String descricao;
    private boolean ehAutor;

    public ProdutoResponseDTO(Produto produto, boolean isAutor) {
        this.id = produto.getId();
        this.nome = produto.getNome();
        this.preco = produto.getPreco();
        this.quantidade = produto.getQuantidade();
        this.imagem = produto.getImagem();
        this.descricao = produto.getDescricao();
        this.icone = produto.getIcone();
        this.categoria = produto.getCategoria();

        if (produto.getAutor() != null) {
            var a = produto.getAutor();
            this.autor = new UsuarioDTO(a.getId(), a.getNome(), a.getEmail(),
                    a.getCpf(), a.getCnpj(), a.getTelefone(), a.getFoto(), a.getPontos(), a.getLoja());
        }

        this.ehAutor = isAutor;
    }
}
