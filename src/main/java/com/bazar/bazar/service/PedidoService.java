// src/main/java/com/bazar/bazar/service/PedidoService.java
package com.bazar.bazar.service;

import com.bazar.bazar.model.Endereco;
import com.bazar.bazar.model.ItemPedido;
import com.bazar.bazar.model.Pedido;
import com.bazar.bazar.model.Produto;
import com.bazar.bazar.model.Usuario; // Usando Usuario em vez de Cliente
import com.bazar.bazar.repositories.EnderecoRepository;
import com.bazar.bazar.repositories.ItemPedidoRepository;
import com.bazar.bazar.repositories.PedidoRepository;
import com.bazar.bazar.repositories.ProdutoRepository;
import com.bazar.bazar.repositories.UsuarioRepository; // Repositório para Usuario
import com.bazar.bazar.request.ItemPedidoRequest;
import com.bazar.bazar.request.PedidoRequest;
import com.bazar.bazar.response.ItensUsuarioResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private final EnderecoRepository enderecoRepository;

    private final PedidoRepository pedidoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    @Autowired
    public PedidoService(PedidoRepository pedidoRepository,
                         EnderecoRepository enderecoRepository, 
                         UsuarioRepository usuarioRepository,
                         ProdutoRepository produtoRepository,
                         ItemPedidoRepository itemPedidoRepository
                         ) {
        this.pedidoRepository = pedidoRepository;
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
        this.produtoRepository = produtoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    // --- Métodos de CRUD ---

    @Transactional
    public Pedido criarPedido(PedidoRequest pedidoRequest, Usuario usuarioLogado) {
        Usuario cliente = usuarioRepository.findById(usuarioLogado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado com ID: " + usuarioLogado.getId()));
        Endereco enderecoEntrega = enderecoRepository.findByUsuarioId(usuarioLogado.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço de entrega não encontrado para o cliente com ID: " + cliente.getId()));

        Usuario vendedor = usuarioRepository.findById(pedidoRequest.getVendedorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor não encontrado com ID: " + pedidoRequest.getVendedorId()));
        Pedido novoPedido = new Pedido(cliente, vendedor);
        novoPedido.setEnderecoEntrega(enderecoEntrega);
        novoPedido.setDataCriacao(LocalDateTime.now());
        novoPedido.setDataEntrega(pedidoRequest.getDataEntrega() != null ? pedidoRequest.getDataEntrega() : LocalDateTime.now());
        novoPedido.setRemote(pedidoRequest.getRemote());

        if (pedidoRequest.getItens() != null && !pedidoRequest.getItens().isEmpty()) {
            for (ItemPedidoRequest itemRequest : pedidoRequest.getItens()) {
                Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado com ID: " + itemRequest.getProdutoId()));

                if (itemRequest.getQuantidade() <= 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A quantidade do item deve ser positiva.");
                }

                if (produto.getQuantidade() < itemRequest.getQuantidade()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque insuficiente para o produto: " + produto.getNome());
                }

                produto.setQuantidade(produto.getQuantidade() - itemRequest.getQuantidade());
                produtoRepository.save(produto);

                ItemPedido itemPedido = new ItemPedido(novoPedido, produto, itemRequest.getQuantidade(), produto.getAutor(), usuarioLogado);
                novoPedido.addItem(itemPedido);
            }
        }
        return pedidoRepository.save(novoPedido);
    }
    
    public List<Pedido> listarTodosPedidos() {
        return pedidoRepository.findAll();
    }

    public Pedido buscarPedidoPorId(UUID id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID: " + id));
    }
    


    @Transactional
    public Pedido atualizarPedido(UUID id, PedidoRequest pedidoRequest) {
        Pedido pedidoExistente = buscarPedidoPorId(id);
        Usuario usuario = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Usuario usuarioLogado = usuarioRepository.findById(usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado com ID: " + usuario.getId()));
        if (usuarioLogado.getId() != null && !pedidoExistente.getCliente().getId().equals(usuarioLogado.getId())) {
            Usuario novoCliente = usuarioRepository.findById(usuarioLogado.getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Novo cliente não encontrado com ID: " + usuarioLogado.getId()));
            pedidoExistente.setCliente(novoCliente);
        }
        
        if (pedidoRequest.getVendedorId() != null && !pedidoExistente.getVendedor().getId().equals(pedidoRequest.getVendedorId())) {
            Usuario novoVendedor = usuarioRepository.findById(pedidoRequest.getVendedorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Novo vendedor não encontrado com ID: " + pedidoRequest.getVendedorId()));
            pedidoExistente.setVendedor(novoVendedor);
        }

        pedidoExistente.setRemote(pedidoRequest.getRemote());

        if (pedidoRequest.getItens() != null) {
            pedidoExistente.getItens().clear();

            for (ItemPedidoRequest itemRequest : pedidoRequest.getItens()) {
                Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado com ID: " + itemRequest.getProdutoId()));

                ItemPedido novoItem = new ItemPedido(pedidoExistente, produto, itemRequest.getQuantidade(), produto.getAutor(), usuarioLogado);
                pedidoExistente.addItem(novoItem);
            }
        }
        return pedidoRepository.save(pedidoExistente);
    }
    
    @Transactional
    public void deletarPedido(UUID id) {
        if (!pedidoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado com ID: " + id);
        }
        pedidoRepository.deleteById(id);
    }

    // CORREÇÃO: Recebendo UUID em vez de String para o ID do pedido
    @Transactional
    public Pedido addItemToPedido(UUID pedidoId, ItemPedidoRequest itemRequest) {
        Pedido pedido = buscarPedidoPorId(pedidoId);
        Produto produto = produtoRepository.findById(itemRequest.getProdutoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado com ID: " + itemRequest.getProdutoId()));

        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (itemRequest.getQuantidade() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantidade do item deve ser positiva.");
        }
        if (produto.getQuantidade() < itemRequest.getQuantidade()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estoque insuficiente para o produto: " + produto.getNome());
        }

        ItemPedido newItem = new ItemPedido(pedido, produto, itemRequest.getQuantidade(), produto.getAutor(), usuarioLogado);
        pedido.addItem(newItem);

        return pedidoRepository.save(pedido);
    }
    
    // CORREÇÃO: Adicionando o método para remover item, que estava faltando no seu PedidoService.
    @Transactional
    public Pedido removeItemFromPedido(UUID pedidoId, UUID itemId) {
        Pedido pedido = buscarPedidoPorId(pedidoId);
        ItemPedido itemToRemove = itemPedidoRepository.findById(itemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item do pedido não encontrado com ID: " + itemId));

        if (!itemToRemove.getPedido().getId().equals(pedidoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Item não pertence a este pedido.");
        }

        pedido.removeItem(itemToRemove);
        return pedidoRepository.save(pedido);
    }
}