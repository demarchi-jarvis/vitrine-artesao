package com.bazar.bazar.controller;

import com.bazar.bazar.model.Endereco;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.service.EnderecoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/endereco")
public class EnderecoController {

    private final EnderecoService enderecoService;

    @Autowired
    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @PostMapping
    public ResponseEntity<Endereco> criar(@RequestBody Endereco endereco,
                                          @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enderecoService.salvar(endereco, usuarioLogado));
    }

    @GetMapping("/usuario")
    public ResponseEntity<Endereco> getEnderecoPorUsuario(@AuthenticationPrincipal Usuario usuarioLogado) {
        return enderecoService.buscarEnderecoPorUsuarioId(usuarioLogado.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/email")
    public ResponseEntity<Endereco> getEnderecoPorEmail(@RequestParam String email) {
        return enderecoService.buscarEnderecoPorEmail(email)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping
    public ResponseEntity<Endereco> atualizar(@RequestBody Endereco dados,
                                              @AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(enderecoService.atualizar(usuarioLogado.getId(), dados));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id, @AuthenticationPrincipal Usuario usuarioLogado) {
        enderecoService.deletar(id, usuarioLogado.getId());
        return ResponseEntity.noContent().build();
    }
}
