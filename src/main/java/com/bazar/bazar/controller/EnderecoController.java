package com.bazar.bazar.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bazar.bazar.model.Endereco;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.service.EnderecoService;

@RestController
@RequestMapping("/api/endereco")
public class EnderecoController {

    private final EnderecoService enderecoService;

    @Autowired
    public EnderecoController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @PostMapping
    public ResponseEntity<Endereco> criar(@RequestBody Endereco endereco) {

        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Endereco novoEndereco = enderecoService.salvar(endereco, usuarioLogado);
        
        return new ResponseEntity<>(novoEndereco, HttpStatus.CREATED);
    }

    @GetMapping
    public List<Endereco> buscarTodos() {
        return enderecoService.buscarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Endereco> buscarPorId(@PathVariable UUID id) {
        return enderecoService.buscarPorId(id)
                .map(endereco -> new ResponseEntity<>(endereco, HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/usuario")
    public ResponseEntity<Endereco> getEnderecoPorUsuario() {

        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID usuarioId = usuarioLogado.getId();

        Optional<Endereco> endereco = enderecoService.buscarEnderecoPorUsuarioId(usuarioId);

        if (endereco.isPresent()) {
            return ResponseEntity.ok(endereco.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @GetMapping("/email")
    public ResponseEntity<Endereco> getEnderecoPorEmail(@RequestParam String email) {

        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID usuarioId = usuarioLogado.getId();

        Optional<Endereco> endereco = enderecoService.buscarEnderecoPorEmail(email);

        if (endereco.isPresent()) {
            return ResponseEntity.ok(endereco.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @PutMapping
    public ResponseEntity<Endereco> atualizar(@RequestBody Endereco enderecoAtualizado) {
        Usuario usuarioLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return enderecoService.buscarEnderecoPorUsuarioId(usuarioLogado.getId())
                .map(endereco -> {

                    endereco.setCidade(enderecoAtualizado.getCidade());
                    endereco.setEstado(enderecoAtualizado.getEstado());
                    endereco.setCep(enderecoAtualizado.getCep());
                    endereco.setRua(enderecoAtualizado.getRua());
                    endereco.setNumero(enderecoAtualizado.getNumero());
                    endereco.setAdicional(enderecoAtualizado.getAdicional());
                    endereco.setBairro(enderecoAtualizado.getBairro());
                    endereco.setComplemento(enderecoAtualizado.getComplemento());
                    
                    Endereco salvo = enderecoService.salvar(endereco,usuarioLogado);
                    return new ResponseEntity<>(salvo, HttpStatus.OK);
                })
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        if (enderecoService.buscarPorId(id).isPresent()) {
            enderecoService.deletar(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}