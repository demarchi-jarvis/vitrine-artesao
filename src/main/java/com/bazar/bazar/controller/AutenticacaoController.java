package com.bazar.bazar.controller;

import com.bazar.bazar.dto.LoginRequestDTO;
import com.bazar.bazar.dto.RegisterRequestDTO;
import com.bazar.bazar.dto.ResponseDTO;
import com.bazar.bazar.service.UsuarioService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/autenticacao")
@RequiredArgsConstructor
public class AutenticacaoController {
    private final UsuarioService usuarioService;
    

    @PostMapping("/login")
    public ResponseEntity<ResponseDTO> login(@RequestBody LoginRequestDTO body) {
        Optional<ResponseDTO> responseDTO = this.usuarioService.fazerLogin(body);

        return responseDTO.map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/registrar")
    public ResponseEntity<ResponseDTO> register(@RequestBody RegisterRequestDTO body) {
        Optional<ResponseDTO> responseDTO = this.usuarioService.registrarUsuario(body);

        return responseDTO.map(ResponseEntity::ok)
                          .orElseGet(() -> ResponseEntity.status(409).build());
    }
}
