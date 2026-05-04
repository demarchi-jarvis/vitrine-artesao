package com.bazar.bazar;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void limparBanco() {
        jdbc.execute("TRUNCATE TABLE item_pedido, pedido, produto, endereco, categoria, usuarios CASCADE");
    }

    protected String registrarEObterToken(String nome, String email, String senha) throws Exception {
        String body = """
                {"nome":"%s","email":"%s","senha":"%s"}
                """.formatted(nome, email, senha);
        MvcResult result = mockMvc.perform(
                post("/api/autenticacao/registrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token").asText();
    }

    protected MockHttpServletRequestBuilder comToken(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token);
    }

    protected UUID obterIdUsuarioLogado(String token) throws Exception {
        MvcResult result = mockMvc.perform(
                comToken(get("/api/usuarios/logado"), token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected UUID criarCategoria(String token, String nome) throws Exception {
        String body = """
                {"nome":"%s","descricao":"Descrição de %s","icone":"icone.png"}
                """.formatted(nome, nome);
        MvcResult result = mockMvc.perform(
                comToken(post("/api/categorias"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected UUID criarProduto(String token, UUID categoriaId, String nome, double preco, int quantidade) throws Exception {
        String body = """
                {"nome":"%s","preco":%s,"quantidade":%d,"imagem":"img.jpg","icone":"icon.jpg","descricao":"desc","categoriaId":"%s"}
                """.formatted(nome, preco, quantidade, categoriaId);
        MvcResult result = mockMvc.perform(
                comToken(post("/api/produtos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected void criarEndereco(String token) throws Exception {
        String body = """
                {"rua":"Rua das Flores","numero":42,"bairro":"Centro","cidade":"Vassouras","estado":"RJ","cep":"27700-000","complemento":"","adicional":""}
                """;
        mockMvc.perform(
                comToken(post("/api/endereco"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    protected UUID criarPedido(String compradorToken, UUID vendedorId, UUID produtoId, int quantidade) throws Exception {
        String body = """
                {"vendedorId":"%s","remote":false,"itens":[{"produtoId":"%s","quantidade":%d}]}
                """.formatted(vendedorId, produtoId, quantidade);
        MvcResult result = mockMvc.perform(
                comToken(post("/api/pedidos"), compradorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    protected void ativarLoja(String token) throws Exception {
        mockMvc.perform(
                comToken(patch("/api/usuarios/loja/status"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":true}"))
                .andExpect(status().isOk());
    }
}
