package com.bazar.bazar;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AutenticacaoIntegrationTest extends BaseIntegrationTest {

    @Test
    void registrar_sucesso_retornaTokenENome() throws Exception {
        mockMvc.perform(post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Maria Artesã","email":"maria@vitrine.com","senha":"senha123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("Maria Artesã"));
    }

    @Test
    void registrar_emailDuplicado_retorna409() throws Exception {
        registrarEObterToken("Maria", "maria@vitrine.com", "senha123");

        mockMvc.perform(post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outra Maria","email":"maria@vitrine.com","senha":"outrasenha"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void login_credenciaisCorretas_retornaToken() throws Exception {
        registrarEObterToken("João", "joao@vitrine.com", "minhasenha");

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"joao@vitrine.com","senha":"minhasenha"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.nome").value("João"));
    }

    @Test
    void login_senhaErrada_retorna401() throws Exception {
        registrarEObterToken("Ana", "ana@vitrine.com", "correta123");

        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@vitrine.com","senha":"errada999"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_emailInexistente_retorna401() throws Exception {
        mockMvc.perform(post("/api/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nao-existe@vitrine.com","senha":"qualquer"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rotaProtegida_semToken_retorna403() throws Exception {
        mockMvc.perform(get("/api/usuarios/logado"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotaProtegida_tokenInvalido_retorna403() throws Exception {
        mockMvc.perform(get("/api/usuarios/logado")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registrar_naoExposeSenhaNoRetorno() throws Exception {
        mockMvc.perform(post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Seguro","email":"seguro@vitrine.com","senha":"secreto123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }
}
