package com.bazar.bazar.service;


import com.bazar.bazar.dto.LoginRequestDTO;
import com.bazar.bazar.dto.RegisterRequestDTO;
import com.bazar.bazar.dto.ResponseDTO;
import com.bazar.bazar.dto.UsuarioDTO;
import com.bazar.bazar.model.Cliente;
import com.bazar.bazar.model.Produto;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.repositories.ClienteRepository;
import com.bazar.bazar.repositories.UsuarioRepository;
import com.bazar.bazar.request.UsuarioUpdateRequest;
import com.bazar.bazar.security.TokenService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Import para @Transactional
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;


@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }
    public Optional<ResponseDTO> fazerLogin(LoginRequestDTO requestDTO) {
        Optional<Usuario> userExistente = this.usuarioRepository.findByEmail(requestDTO.email());

        if (userExistente.isPresent()) {
            Usuario usuario = userExistente.get();
            if (passwordEncoder.matches(requestDTO.senha(), usuario.getSenha())) {
                String token = this.tokenService.generateToken(usuario);
                return Optional.of(new ResponseDTO(usuario.getNome(), token));
            }
        }
        
        return Optional.empty();
    }

        public Optional<ResponseDTO> registrarUsuario(RegisterRequestDTO requestDTO) {
        Optional<Usuario> userExistente = this.usuarioRepository.findByEmail(requestDTO.email());

        if (userExistente.isEmpty()) {
            Usuario novoUsuario = new Usuario();
            novoUsuario.setSenha(passwordEncoder.encode(requestDTO.senha()));
            novoUsuario.setEmail(requestDTO.email());
            novoUsuario.setNome(requestDTO.nome());
            novoUsuario.setFoto("https://www.llt.at/wp-content/uploads/2021/11/blank-profile-picture-g77b5d6651-1280-705x705.png");
            novoUsuario.setLoja(false);
            novoUsuario.setPontos(0L);
            Usuario usuarioSalvo = this.usuarioRepository.save(novoUsuario);

            String token = this.tokenService.generateToken(usuarioSalvo);
            return Optional.of(new ResponseDTO(usuarioSalvo.getNome(), token));
        }

        return Optional.empty();
    }

    @Transactional
    public Usuario atualizarUsuario(UUID id, UsuarioUpdateRequest usuarioDTO) {
        // Encontra o usuário por UUID, lançando uma exceção se não for encontrado.
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id));

        if (usuarioDTO.getNome() != null && !usuarioDTO.getNome().isBlank()) {
            usuario.setNome(usuarioDTO.getNome());
        }
        //if (usuarioDTO.getEmail() != null && !usuarioDTO.getEmail().isBlank()) {
        //    usuario.setEmail(usuarioDTO.getEmail());
        //}
        if (usuarioDTO.getFoto() != null && !usuarioDTO.getFoto().isBlank()) {
            usuario.setFoto(usuarioDTO.getFoto());
        }
        if (usuarioDTO.getTelefone() != null && !usuarioDTO.getTelefone().isBlank()) {
            usuario.setTelefone(usuarioDTO.getTelefone());
        }
        if (usuarioDTO.getCpf() != null && !usuarioDTO.getCpf().isBlank()) {
            usuario.setCpf(usuarioDTO.getCpf());
        }
        //if (usuarioDTO.getCnpj() != null && !usuarioDTO.getCnpj().isBlank()) {
        //    usuario.setCnpj(usuarioDTO.getCnpj());
        //}
        //if (usuarioDTO.getPontos() != null) {
        //    usuario.setPontos(usuarioDTO.getPontos());
        //}

        return usuarioRepository.save(usuario);
    }
    @Transactional
    public Usuario alterarUsuarioLoja(UUID id, Boolean status) {
        // Encontra o usuário por UUID, lançando uma exceção se não for encontrado.
        Usuario usuario = usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id));

        usuario.setLoja(status);

        return usuarioRepository.save(usuario);
    }
    // Método para criar um novo usuário
    @Transactional
    public Usuario criarUsuario(UsuarioDTO usuarioDTO) {
        // Verificação se o e-mail já existe
        if (usuarioRepository.findByEmail(usuarioDTO.getEmail()).isPresent()) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(usuarioDTO.getNome());
        novoUsuario.setEmail(usuarioDTO.getEmail());
        novoUsuario.setFoto(usuarioDTO.getFoto());
        novoUsuario.setTelefone(usuarioDTO.getTelefone());
        novoUsuario.setCpf(usuarioDTO.getCpf());
        novoUsuario.setPontos(usuarioDTO.getPontos());
        novoUsuario.setCnpj(usuarioDTO.getCnpj());

        return usuarioRepository.save(novoUsuario);
    }
    
    // Método para buscar um usuário por UUID
    public Usuario buscarUsuarioPorId(UUID id) {
        return usuarioRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id));
    }
    public Usuario buscarDonoLoja(String email) {
        return usuarioRepository.findByEmail(email).orElseThrow(() -> new NoSuchElementException("Usuário não encontrado com email: " + email));
    }
    // Método para listar todos os usuários
    public List<Usuario> listarTodosUsuarios() {
        return usuarioRepository.findAll();
    }
    public Page<UsuarioDTO> buscarUsuarioPorNome(String nome, Pageable pageable) {
        if(nome== null || nome.isBlank()) {
            Page<Usuario> usuarios = usuarioRepository.findByLojaTrue(pageable);
            return usuarios.map(usuario -> new UsuarioDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getCpf(),
                usuario.getCnpj(),
                usuario.getTelefone(),
                usuario.getFoto(),
                usuario.getPontos(),
                usuario.getLoja()
            ));
    }else{
        Page<Usuario> usuarios = usuarioRepository.findByNomeContainingIgnoreCaseAndLojaTrue(nome, pageable);
        return usuarios.map(usuario -> new UsuarioDTO(
            usuario.getId(),
            usuario.getNome(),
            usuario.getEmail(),
            usuario.getCpf(),
            usuario.getCnpj(),
            usuario.getTelefone(),
            usuario.getFoto(),
            usuario.getPontos(),
            usuario.getLoja()
        ));
        }
    }
    // Método para deletar um usuário
    @Transactional
    public void deletarUsuario(UUID id) {
        if (!usuarioRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id);
        }
        usuarioRepository.deleteById(id);
    }
}