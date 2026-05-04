package com.bazar.bazar.service;

import com.bazar.bazar.model.Endereco;
import com.bazar.bazar.model.Usuario;
import com.bazar.bazar.repositories.EnderecoRepository;
import com.bazar.bazar.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

@Service
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;
    private final UsuarioRepository usuarioRepository;

    @Autowired
    public EnderecoService(EnderecoRepository enderecoRepository, UsuarioRepository usuarioRepository) {
        this.enderecoRepository = enderecoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public Endereco salvar(Endereco endereco, Usuario usuario) {
        enderecoRepository.findByUsuarioId(usuario.getId()).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Usuário já possui um endereço cadastrado. Use PUT /api/endereco para atualizar.");
        });
        endereco.setUsuario(usuario);
        return enderecoRepository.save(endereco);
    }

    @Transactional
    public Endereco atualizar(UUID usuarioId, Endereco dados) {
        Endereco existente = enderecoRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Endereço não encontrado para o usuário informado."));
        existente.setCidade(dados.getCidade());
        existente.setEstado(dados.getEstado());
        existente.setCep(dados.getCep());
        existente.setRua(dados.getRua());
        existente.setNumero(dados.getNumero());
        existente.setAdicional(dados.getAdicional());
        existente.setBairro(dados.getBairro());
        existente.setComplemento(dados.getComplemento());
        return enderecoRepository.save(existente);
    }

    public Optional<Endereco> buscarPorId(UUID id) {
        return enderecoRepository.findById(id);
    }

    public Optional<Endereco> buscarEnderecoPorUsuarioId(UUID usuarioId) {
        return enderecoRepository.findByUsuarioId(usuarioId);
    }

    public Optional<Endereco> buscarEnderecoPorEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado com email: " + email));
        return enderecoRepository.findByUsuarioId(usuario.getId());
    }

    @Transactional
    public void deletar(UUID id, UUID usuarioId) {
        Endereco endereco = enderecoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endereço não encontrado com ID: " + id));
        if (!endereco.getUsuario().getId().equals(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não pode excluir o endereço de outro usuário.");
        }
        enderecoRepository.deleteById(id);
    }
}
