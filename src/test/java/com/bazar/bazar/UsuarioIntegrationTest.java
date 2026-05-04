package com.bazar.bazar;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsuarioIntegrationTest extends BaseIntegrationTest {

    @Test
    void getLogado_retornaDadosDoUsuario() throws Exception {
        String token = registrarEObterToken("Carlos Silva", "carlos@vitrine.com", "senha123");

        mockMvc.perform(comToken(get("/api/usuarios/logado"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Silva"))
                .andExpect(jsonPath("$.email").value("carlos@vitrine.com"))
                .andExpect(jsonPath("$.loja").value(false))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void alterarPerfil_atualizaNome() throws Exception {
        String token = registrarEObterToken("Nome Antigo", "alterar@vitrine.com", "senha123");

        mockMvc.perform(comToken(patch("/api/usuarios/alterar"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Nome Novo","telefone":"(24) 99999-9999"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.telefone").value("(24) 99999-9999"));
    }

    @Test
    void ativarLoja_muda_lojaParaTrue() throws Exception {
        String token = registrarEObterToken("Artesã", "artesa@vitrine.com", "senha123");

        mockMvc.perform(comToken(patch("/api/usuarios/loja/status"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loja").value(true));
    }

    @Test
    void desativarLoja_muda_lojaParaFalse() throws Exception {
        String token = registrarEObterToken("Artesã2", "artesa2@vitrine.com", "senha123");
        ativarLoja(token);

        mockMvc.perform(comToken(patch("/api/usuarios/loja/status"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loja").value(false));
    }

    @Test
    void listarPerfis_retornaSomenteLojasAtivas() throws Exception {
        String tokenComprador = registrarEObterToken("Comprador", "comprador@vitrine.com", "senha123");
        String tokenArtesao1 = registrarEObterToken("Artesão A", "artesao-a@vitrine.com", "senha123");
        String tokenArtesao2 = registrarEObterToken("Artesão B", "artesao-b@vitrine.com", "senha123");

        ativarLoja(tokenArtesao1);
        ativarLoja(tokenArtesao2);
        // comprador não ativa loja

        mockMvc.perform(comToken(get("/api/usuarios/perfis"), tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[*].loja", everyItem(is(true))))
                .andExpect(jsonPath("$.content[*].senha").doesNotExist());
    }

    @Test
    void listarPerfis_filtroNome_retornaApenasCorrespondentes() throws Exception {
        String tokenBuscador = registrarEObterToken("Buscador", "buscador@vitrine.com", "senha123");
        String tokenArtes1 = registrarEObterToken("Ceramista João", "ceramista@vitrine.com", "senha123");
        String tokenArtes2 = registrarEObterToken("Tecelã Maria", "tecela@vitrine.com", "senha123");
        ativarLoja(tokenArtes1);
        ativarLoja(tokenArtes2);

        mockMvc.perform(comToken(get("/api/usuarios/perfis").param("nome", "Ceram"), tokenBuscador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nome").value("Ceramista João"));
    }

    @Test
    void buscarDono_retornaArtesaoPorEmail() throws Exception {
        String tokenArtesao = registrarEObterToken("Artesão Dono", "dono@vitrine.com", "senha123");
        String tokenBuscador = registrarEObterToken("Buscador2", "buscador2@vitrine.com", "senha123");

        mockMvc.perform(comToken(get("/api/usuarios/dono").param("email", "dono@vitrine.com"), tokenBuscador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("dono@vitrine.com"))
                .andExpect(jsonPath("$.nome").value("Artesão Dono"));
    }

    @Test
    void getLogado_naoExposeSenha() throws Exception {
        String token = registrarEObterToken("Seguro", "seguro2@vitrine.com", "senha123");

        mockMvc.perform(comToken(get("/api/usuarios/logado"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }
}
