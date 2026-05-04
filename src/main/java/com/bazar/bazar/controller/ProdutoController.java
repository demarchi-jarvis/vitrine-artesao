package com.bazar.bazar.controller;

import com.bazar.bazar.dto.ProdutoResponseDTO;
import com.bazar.bazar.model.Categoria;
import com.bazar.bazar.model.Produto;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.request.ProdutoRequest;
import com.bazar.bazar.service.CategoriaService;
import com.bazar.bazar.service.ProdutoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoService produtoService;
    private final CategoriaService categoriaService;

    @Autowired
    public ProdutoController(ProdutoService produtoService, CategoriaService categoriaService) {
        this.produtoService = produtoService;
        this.categoriaService = categoriaService;
    }

    @PostMapping
    public ResponseEntity<Produto> createProduto(@RequestBody ProdutoRequest produtoRequest,
                                                 @AuthenticationPrincipal Usuario usuarioLogado) {
        Categoria categoria = categoriaService.getCategoriaById(produtoRequest.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria não encontrada com o ID informado."));

        Produto novoProduto = new Produto();
        novoProduto.setNome(produtoRequest.getNome());
        novoProduto.setPreco(produtoRequest.getPreco());
        novoProduto.setQuantidade(produtoRequest.getQuantidade());
        novoProduto.setImagem(produtoRequest.getImagem());
        novoProduto.setIcone(produtoRequest.getIcone());
        novoProduto.setDescricao(produtoRequest.getDescricao());
        novoProduto.setCategoria(categoria);
        novoProduto.setDataCriacao(LocalDateTime.now());
        novoProduto.setAutor(usuarioLogado);

        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.createProduto(novoProduto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoResponseDTO> getProdutoById(@PathVariable UUID id,
                                                             @AuthenticationPrincipal Usuario usuarioLogado) {
        return produtoService.getProdutoById(id)
                .map(produto -> {
                    boolean ehAutor = produto.getAutor() != null &&
                            produto.getAutor().getId().equals(usuarioLogado.getId());
                    return ResponseEntity.ok(new ProdutoResponseDTO(produto, ehAutor));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<Page<Produto>> getAllProdutos(
            @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.getAllProdutos(pageable));
    }

    @GetMapping("/filtro")
    public ResponseEntity<Page<Produto>> getAllProdutosFiltro(
            @RequestParam(required = false) UUID categoriaId,
            @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
        Page<Produto> page = categoriaId != null
                ? produtoService.getProdutosByCategoria(categoriaId, pageable)
                : produtoService.getAllProdutos(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Produto>> searchProdutosByName(@RequestParam String nome) {
        List<Produto> produtos = produtoService.getProdutosByNome(nome);
        return produtos.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(produtos);
    }

    @GetMapping("/meus-produtos")
    public ResponseEntity<List<Produto>> getMeusProdutos(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(produtoService.getMeusProdutos(usuarioLogado.getId()));
    }

    @GetMapping("/loja")
    public ResponseEntity<List<Produto>> getProdutosUsuario(@RequestParam String email) {
        return ResponseEntity.ok(produtoService.buscarProdutosUsuario(email));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Produto> updateProduto(@PathVariable UUID id,
                                                 @RequestBody ProdutoRequest produtoRequest,
                                                 @AuthenticationPrincipal Usuario usuarioLogado) {
        return produtoService.getProdutoById(id)
                .map(existente -> {
                    if (existente.getAutor() == null ||
                            !existente.getAutor().getId().equals(usuarioLogado.getId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).<Produto>build();
                    }
                    // Bug F fix: mapeia DTO → entidade para evitar mass assignment
                    Categoria categoria = categoriaService.getCategoriaById(produtoRequest.getCategoriaId())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));
                    Produto detalhes = new Produto();
                    detalhes.setNome(produtoRequest.getNome());
                    detalhes.setPreco(produtoRequest.getPreco());
                    detalhes.setQuantidade(produtoRequest.getQuantidade());
                    detalhes.setImagem(produtoRequest.getImagem());
                    detalhes.setIcone(produtoRequest.getIcone());
                    detalhes.setDescricao(produtoRequest.getDescricao());
                    detalhes.setCategoria(categoria);
                    return ResponseEntity.ok(produtoService.updateProduto(id, detalhes, usuarioLogado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduto(@PathVariable UUID id,
                                              @AuthenticationPrincipal Usuario usuarioLogado) {
        produtoService.getProdutoById(id).ifPresentOrElse(
                existente -> {
                    if (existente.getAutor() == null ||
                            !existente.getAutor().getId().equals(usuarioLogado.getId())) {
                        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Apenas o autor pode deletar este produto.");
                    }
                    produtoService.deleteProduto(id);
                },
                () -> { throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."); }
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<Categoria>> getCategoriasDeProdutos() {
        List<Categoria> categorias = produtoService.getCategoriasDosProdutos();
        return categorias.isEmpty() ? ResponseEntity.noContent().build() : ResponseEntity.ok(categorias);
    }

    @PatchMapping("/icone/{id}")
    public ResponseEntity<Produto> updateProdutoIcone(@PathVariable UUID id,
                                                      @RequestBody String icone) {
        return ResponseEntity.ok(produtoService.updateIconeProduto(id, icone));
    }
}
