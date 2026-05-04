package com.bazar.bazar.controller;

import com.bazar.bazar.dto.UsuarioDTO;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.request.StatusRequest;
import com.bazar.bazar.request.UsuarioUpdateRequest;
import com.bazar.bazar.response.PaginaResponse;
import com.bazar.bazar.service.UsuarioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @Autowired
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/logado")
    public ResponseEntity<UsuarioDTO> getUsuarioLogado(@AuthenticationPrincipal Usuario usuarioLogado) {
        return ResponseEntity.ok(toDTO(usuarioLogado));
    }

    @PatchMapping("/alterar")
    public ResponseEntity<UsuarioDTO> updateUsuario(@RequestBody UsuarioUpdateRequest request,
                                                    @AuthenticationPrincipal Usuario usuarioLogado) {
        Usuario atualizado = usuarioService.atualizarUsuario(usuarioLogado.getId(), request);
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @PatchMapping("/loja/status")
    public ResponseEntity<UsuarioDTO> updateLoja(@RequestBody StatusRequest status,
                                                 @AuthenticationPrincipal Usuario usuarioLogado) {
        Usuario atualizado = usuarioService.alterarUsuarioLoja(usuarioLogado.getId(), status.status());
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @GetMapping("/perfis")
    public ResponseEntity<PaginaResponse<UsuarioDTO>> getPerfis(
            @RequestParam(required = false) String nome,
            @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
        Page<UsuarioDTO> page = usuarioService.buscarUsuarioPorNome(nome, pageable);
        PaginaResponse<UsuarioDTO> resposta = new PaginaResponse<>();
        resposta.setContent(page.getContent());
        resposta.setPage(page.getNumber());
        resposta.setSize(page.getSize());
        resposta.setTotalElements(page.getTotalElements());
        resposta.setTotalPages(page.getTotalPages());
        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/dono")
    public ResponseEntity<UsuarioDTO> getDonoLoja(@RequestParam String email) {
        Usuario dono = usuarioService.buscarDonoLoja(email);
        return ResponseEntity.ok(toDTO(dono));
    }

    private UsuarioDTO toDTO(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNome(), u.getEmail(),
                u.getCpf(), u.getCnpj(), u.getTelefone(),
                u.getFoto(), u.getPontos(), u.getLoja());
    }
}
