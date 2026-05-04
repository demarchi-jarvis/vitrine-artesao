// src/main/java/com/bazar/bazar/controller/PedidoController.java
package com.bazar.bazar.controller;

import com.bazar.bazar.model.Pedido;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.request.ItemPedidoRequest;
import com.bazar.bazar.request.PedidoRequest;
import com.bazar.bazar.response.PedidoResponse;
import com.bazar.bazar.service.PedidoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    @Autowired
    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criarPedido(@RequestBody PedidoRequest pedidoRequest,
                                                       @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido novoPedido = pedidoService.criarPedido(pedidoRequest, usuarioLogado);
        return new ResponseEntity<>(new PedidoResponse(novoPedido), HttpStatus.CREATED);
    }

    // GET http://localhost:8081/api/pedidos/{id}
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPedidoPorId(@PathVariable UUID id,
                                                             @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido pedido = pedidoService.buscarPedidoPorId(id);
        // Bug E fix: apenas cliente ou vendedor do pedido pode visualizá-lo
        boolean isOwner = pedido.getCliente().getId().equals(usuarioLogado.getId())
                || pedido.getVendedor().getId().equals(usuarioLogado.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        return new ResponseEntity<>(new PedidoResponse(pedido), HttpStatus.OK);
    }

    // GET http://localhost:8081/api/pedidos
    @GetMapping
    public ResponseEntity<List<PedidoResponse>> listarTodosPedidos(@AuthenticationPrincipal Usuario usuarioLogado) {
        // Bug D fix: retorna apenas pedidos do usuário logado (como cliente ou vendedor)
        List<PedidoResponse> pedidos = pedidoService.listarPedidosPorUsuario(usuarioLogado.getId()).stream()
                .map(PedidoResponse::new)
                .collect(Collectors.toList());
        return new ResponseEntity<>(pedidos, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PedidoResponse> atualizarPedido(@PathVariable UUID id,
                                                           @RequestBody PedidoRequest pedidoRequest,
                                                           @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido existente = pedidoService.buscarPedidoPorId(id);
        boolean isOwner = existente.getCliente().getId().equals(usuarioLogado.getId())
                || existente.getVendedor().getId().equals(usuarioLogado.getId());
        if (!isOwner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        }
        return new ResponseEntity<>(new PedidoResponse(pedidoService.atualizarPedido(id, pedidoRequest)), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarPedido(@PathVariable UUID id,
                                               @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido existente = pedidoService.buscarPedidoPorId(id);
        if (!existente.getCliente().getId().equals(usuarioLogado.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Apenas o comprador pode cancelar o pedido.");
        }
        pedidoService.deletarPedido(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/{pedidoId}/items")
    public ResponseEntity<PedidoResponse> addItemToPedido(@PathVariable UUID pedidoId,
                                                          @RequestBody ItemPedidoRequest itemRequest,
                                                          @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido pedido = pedidoService.buscarPedidoPorId(pedidoId);
        boolean isOwner = pedido.getCliente().getId().equals(usuarioLogado.getId())
                || pedido.getVendedor().getId().equals(usuarioLogado.getId());
        if (!isOwner) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        return new ResponseEntity<>(new PedidoResponse(pedidoService.addItemToPedido(pedidoId, itemRequest)), HttpStatus.OK);
    }

    @DeleteMapping("/{pedidoId}/items/{itemId}")
    public ResponseEntity<PedidoResponse> removeItemFromPedido(@PathVariable UUID pedidoId,
                                                               @PathVariable UUID itemId,
                                                               @AuthenticationPrincipal Usuario usuarioLogado) {
        Pedido pedido = pedidoService.buscarPedidoPorId(pedidoId);
        boolean isOwner = pedido.getCliente().getId().equals(usuarioLogado.getId())
                || pedido.getVendedor().getId().equals(usuarioLogado.getId());
        if (!isOwner) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso negado.");
        return new ResponseEntity<>(new PedidoResponse(pedidoService.removeItemFromPedido(pedidoId, itemId)), HttpStatus.OK);
    }
}