package com.bazar.bazar.service;

import com.bazar.bazar.model.Categoria;
import com.bazar.bazar.model.Produto;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.repositories.ProdutoRepository;
import com.bazar.bazar.repositories.UsuarioRepository;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service 
public class ProdutoService {

    private final UsuarioRepository usuarioRepository;

    private final ProdutoRepository produtoRepository;

    @Autowired 
    public ProdutoService(ProdutoRepository produtoRepository, UsuarioRepository usuarioRepository) {
        this.produtoRepository = produtoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Produto createProduto(Produto produto) {
        if (produto.getDataCriacao() == null) {
            produto.setDataCriacao(LocalDateTime.now());
        }
        return produtoRepository.save(produto);
    }

    public Optional<Produto> getProdutoById(UUID id) {
        return produtoRepository.findById(id);
    }

     public Page<Produto> getAllProdutos(Pageable pageable) {
        return produtoRepository.findAll(pageable);
    }
    public List<Produto> getMeusProdutos(UUID autorId) {
        return produtoRepository.findByAutorId(autorId);
    }
    public List<Produto> buscarProdutosUsuario(String email) {
        Usuario usuarioLoja = usuarioRepository.findByEmail(email).orElseThrow(() -> new NoSuchElementException("Usuário não encontrado com email: " + email)); 

        return produtoRepository.findByAutorId(usuarioLoja.getId());
    }
    public Page<Produto> getProdutosByCategoria(UUID categoriaId, Pageable pageable) {
        return produtoRepository.findByCategoriaId(categoriaId, pageable);
    } 
    
    /*
     *     public List<Produto> getMeusProdutos() {
        // 1. Obtém o objeto de autenticação do contexto de segurança
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. Extrai o objeto do usuário que o SecurityFilter colocou lá
        Usuario usuarioLogado = (Usuario) authentication.getPrincipal();

        // 3. Usa o método do repositório para buscar produtos do usuário logado
        return produtoRepository.findByUsuario(usuarioLogado);
    }
     */
    public List<Produto> getProdutosByNome(String nome) {
        return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }
    public List<Produto> getCategoriaPorId(String nome) {
    return produtoRepository.findByNomeContainingIgnoreCase(nome);
    }
    //@Transactional(readOnly = true)
    public List<Categoria> getCategoriasDosProdutos() { // <-- Retorna List<Categoria>
        return produtoRepository.findDistinctCategoriasByProduto();
    }

    public Produto updateProduto(UUID id, Produto produtoDetails, Usuario autor) {
        return produtoRepository.findById(id)
                .map(produtoExistente -> {
                    produtoExistente.setNome(produtoDetails.getNome());
                    produtoExistente.setPreco(produtoDetails.getPreco());
                    produtoExistente.setDescricao(produtoDetails.getDescricao());
                    produtoExistente.setQuantidade(produtoDetails.getQuantidade());
                    produtoExistente.setCategoria(produtoDetails.getCategoria());
                    produtoExistente.setImagem(produtoDetails.getImagem());
                    produtoExistente.setIcone(produtoDetails.getIcone());
                    produtoExistente.setAutor(autor); 
                    
                    return produtoRepository.save(produtoExistente);
                })
                .orElseThrow(() -> new RuntimeException("Produto não encontrado com ID: " + id));
    }

    @Transactional
    public Produto updateIconeProduto(UUID id, String icone) {
        return produtoRepository.findById(id)
                .map(produto -> {
                    produto.setIcone(icone);
                    return produtoRepository.save(produto);
                })
                .orElseThrow(() -> new NoSuchElementException("Produto não encontrado com ID: " + id));
    }


    public void deleteProduto(UUID id) {
        if (produtoRepository.existsById(id)) {
            produtoRepository.deleteById(id);
        } else {
            throw new RuntimeException("Produto não encontrado com ID: " + id);
        }
    }


    
}