package com.bazar.bazar.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.bazar.bazar.model.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(Usuario usuario) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret); // Use a variável secret injetada
            String token = JWT.create()
                    .withIssuer("login-auth-api")
                    .withSubject(usuario.getEmail()) // Define o subject com o username ou outro identificador
                    .withExpiresAt(this.generateExpirationDate())
                    .sign(algorithm);
            return token;
        } catch (JWTCreationException e) {
            log.error("Falha ao gerar token JWT", e);
            throw new RuntimeException("Erro: Falha na Autenticação", e);
        }
    }
    public String validateToken(String token){
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("login-auth-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return null;
        }
    }

    private Instant generateExpirationDate() {
        return Instant.now().plus(2, ChronoUnit.HOURS);
    }
}