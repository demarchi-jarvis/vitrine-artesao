package com.bazar.bazar.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bazar.bazar.dto.UsuarioDTO;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.repositories.UsuarioRepository;
import com.bazar.bazar.request.StatusRequest;
import com.bazar.bazar.request.UsuarioUpdateRequest;
import com.bazar.bazar.response.PaginaResponse;
import com.bazar.bazar.service.UsuarioService;


@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioService usuarioService, UsuarioRepository usuarioRepository) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/logado")
    public ResponseEntity<UsuarioDTO> getUsuarioLogado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Usuario) {
            Usuario usuarioLogado = (Usuario) principal;
            UsuarioDTO usuarioDto = new UsuarioDTO(usuarioLogado.getId(), usuarioLogado.getNome(), usuarioLogado.getEmail(), usuarioLogado.getCpf(), usuarioLogado.getCnpj(), usuarioLogado.getTelefone(), usuarioLogado.getFoto(), usuarioLogado.getPontos(), usuarioLogado.getLoja());
            return ResponseEntity.ok(usuarioDto);
        }
                return ResponseEntity.notFound().build();
    }
    
    @GetMapping
    public ResponseEntity<String> getUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).body("Não autorizado.");
        }
        
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof Usuario) {
            Usuario usuarioLogado = (Usuario) principal;
            return ResponseEntity.ok("Sucesso! O usuário logado é: " + usuarioLogado.getNome() + " com ID: " + usuarioLogado.getId());
        }
        
        return ResponseEntity.badRequest().body("Principal não é uma instância de Usuario.");
    }
    @PatchMapping("/alterar")
    public ResponseEntity<UsuarioDTO> updateUsuario(@RequestBody UsuarioUpdateRequest usuarioDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Usuario) {
            Usuario usuarioLogado = (Usuario) principal;
            Usuario usuarioAtualizado = usuarioService.atualizarUsuario(usuarioLogado.getId(), usuarioDTO);
            UsuarioDTO usuarioDto = new UsuarioDTO(
                usuarioAtualizado.getId(),
                usuarioAtualizado.getNome(),
                usuarioAtualizado.getEmail(),
                usuarioAtualizado.getCpf(),
                usuarioAtualizado.getCnpj(),
                usuarioAtualizado.getTelefone(),
                usuarioAtualizado.getFoto(),
                usuarioAtualizado.getPontos(),
                usuarioAtualizado.getLoja()
            );
           
            return ResponseEntity.ok(usuarioDto);
        }

        return ResponseEntity.status(404).build();
    }
    @PatchMapping("/loja/status")
    public ResponseEntity<UsuarioDTO> updateLoja(@RequestBody StatusRequest status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401).build();
        }
        Object principal = authentication.getPrincipal();

        if (principal instanceof Usuario) {
            Usuario usuarioLogado = (Usuario) principal;
            Usuario usuarioAtualizado = usuarioService.alterarUsuarioLoja(usuarioLogado.getId(), status.status());

            UsuarioDTO usuarioDto = new UsuarioDTO(
                usuarioAtualizado.getId(),
                usuarioAtualizado.getNome(),
                usuarioAtualizado.getEmail(),
                usuarioAtualizado.getCpf(),
                usuarioAtualizado.getCnpj(),
                usuarioAtualizado.getTelefone(),
                usuarioAtualizado.getFoto(),
                usuarioAtualizado.getPontos(),
                usuarioAtualizado.getLoja()
            );

            return ResponseEntity.ok(usuarioDto);
        }

        return ResponseEntity.status(404).build();
    }
    @GetMapping("perfis")
    public ResponseEntity<PaginaResponse<UsuarioDTO>> getAllUsuarios(@RequestParam(required = false) String nome,
        @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
        Page<UsuarioDTO> usuariosPage = usuarioService.buscarUsuarioPorNome(nome, pageable);
        
        PaginaResponse<UsuarioDTO> resposta = new PaginaResponse<>();
            resposta.setContent(usuariosPage.getContent());
            resposta.setPage(usuariosPage.getNumber());
            resposta.setSize(usuariosPage.getSize());
            resposta.setTotalElements(usuariosPage.getTotalElements());
            resposta.setTotalPages(usuariosPage.getTotalPages());
                
        return new ResponseEntity<>(resposta, HttpStatus.OK);
    }

    @GetMapping("/dono")
    public ResponseEntity<Usuario> getDonoLoja(@RequestParam String email) {
        Usuario usuarioDonoLoja = usuarioService.buscarDonoLoja(email);
        return ResponseEntity.ok(usuarioDonoLoja);
    }
}
/*
    @GetMapping
    public ResponseEntity<Page<Produto>> getAllProdutos(
        @PageableDefault(page = 0, size = 12, sort = "nome") Pageable pageable) {
        
        Page<Produto> produtosPage = produtoService.getAllProdutos(pageable);
        return new ResponseEntity<>(produtosPage, HttpStatus.OK);

    } */