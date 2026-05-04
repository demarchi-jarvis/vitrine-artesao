package com.bazar.bazar;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProdutoIntegrationTest extends BaseIntegrationTest {

    @Test
    void criarProduto_retorna201ComDados() throws Exception {
        String token = registrarEObterToken("Artesão", "artesao@prod.com", "senha123");
        ativarLoja(token);
        UUID catId = criarCategoria(token, "Cestaria");

        mockMvc.perform(comToken(post("/api/produtos"), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Cesto de Bambu","preco":89.90,"quantidade":10,
                                 "imagem":"img.jpg","icone":"icon.jpg","descricao":"Feito à mão",
                                 "categoriaId":"%s"}
                                """.formatted(catId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("Cesto de Bambu"))
                .andExpect(jsonPath("$.preco").value(89.90))
                .andExpect(jsonPath("$.quantidade").value(10))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void listarProdutosFiltro_semAutenticacao_retorna200() throws Exception {
        mockMvc.perform(get("/api/produtos/filtro"))
                .andExpect(status().isOk());
    }

    @Test
    void buscarPorId_ehAutorTrue_paraDonoDoProduto() throws Exception {
        String token = registrarEObterToken("Dono", "dono@prod.com", "senha123");
        UUID catId = criarCategoria(token, "Artesanato");
        UUID prodId = criarProduto(token, catId, "Vaso", 45.0, 5);

        mockMvc.perform(comToken(get("/api/produtos/{id}", prodId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ehAutor").value(true))
                .andExpect(jsonPath("$.autor.senha").doesNotExist());
    }

    @Test
    void buscarPorId_ehAutorFalse_paraOutroUsuario() throws Exception {
        String tokenDono = registrarEObterToken("Dono2", "dono2@prod.com", "senha123");
        String tokenOutro = registrarEObterToken("Outro", "outro@prod.com", "senha123");
        UUID catId = criarCategoria(tokenDono, "Artesanato2");
        UUID prodId = criarProduto(tokenDono, catId, "Tapete", 120.0, 3);

        mockMvc.perform(comToken(get("/api/produtos/{id}", prodId), tokenOutro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ehAutor").value(false));
    }

    @Test
    void atualizarProduto_comoAutor_retorna200() throws Exception {
        String token = registrarEObterToken("Artesão Edit", "edit@prod.com", "senha123");
        UUID catId = criarCategoria(token, "Categoria Edit");
        UUID prodId = criarProduto(token, catId, "Produto Original", 50.0, 10);

        mockMvc.perform(comToken(patch("/api/produtos/{id}", prodId), token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Produto Atualizado","preco":75.0,"quantidade":8,
                                 "imagem":"nova.jpg","icone":"novo-icon.jpg","descricao":"nova desc",
                                 "categoriaId":"%s"}
                                """.formatted(catId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Produto Atualizado"))
                .andExpect(jsonPath("$.preco").value(75.0));
    }

    @Test
    void atualizarProduto_naoAutor_retorna403() throws Exception {
        String tokenDono = registrarEObterToken("Dono3", "dono3@prod.com", "senha123");
        String tokenInvasor = registrarEObterToken("Invasor", "invasor@prod.com", "senha123");
        UUID catId = criarCategoria(tokenDono, "Cat3");
        UUID prodId = criarProduto(tokenDono, catId, "Produto Alheio", 60.0, 5);

        mockMvc.perform(comToken(patch("/api/produtos/{id}", prodId), tokenInvasor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Hackeado","preco":1.0,"quantidade":999,
                                 "imagem":"x","icone":"x","descricao":"x",
                                 "categoriaId":"%s"}
                                """.formatted(catId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletarProduto_comoAutor_retorna204() throws Exception {
        String token = registrarEObterToken("Artesão Del", "del@prod.com", "senha123");
        UUID catId = criarCategoria(token, "Cat Del");
        UUID prodId = criarProduto(token, catId, "Para Deletar", 30.0, 2);

        mockMvc.perform(comToken(delete("/api/produtos/{id}", prodId), token))
                .andExpect(status().isNoContent());

        mockMvc.perform(comToken(get("/api/produtos/{id}", prodId), token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletarProduto_naoAutor_retorna403() throws Exception {
        String tokenDono = registrarEObterToken("Dono4", "dono4@prod.com", "senha123");
        String tokenOutro = registrarEObterToken("Outro2", "outro2@prod.com", "senha123");
        UUID catId = criarCategoria(tokenDono, "Cat4");
        UUID prodId = criarProduto(tokenDono, catId, "Produto Protegido", 90.0, 1);

        mockMvc.perform(comToken(delete("/api/produtos/{id}", prodId), tokenOutro))
                .andExpect(status().isForbidden());
    }

    @Test
    void meusProdutos_retornaApenasDoAutor() throws Exception {
        String tokenA = registrarEObterToken("Artesão A", "aa@prod.com", "senha123");
        String tokenB = registrarEObterToken("Artesão B", "bb@prod.com", "senha123");
        UUID catA = criarCategoria(tokenA, "CatA");
        UUID catB = criarCategoria(tokenB, "CatB");

        criarProduto(tokenA, catA, "Produto A1", 10.0, 5);
        criarProduto(tokenA, catA, "Produto A2", 20.0, 3);
        criarProduto(tokenB, catB, "Produto B1", 30.0, 2);

        mockMvc.perform(comToken(get("/api/produtos/meus-produtos"), tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].nome", containsInAnyOrder("Produto A1", "Produto A2")));
    }

    @Test
    void filtrarPorCategoria_retornaApenasDestaCategoria() throws Exception {
        String token = registrarEObterToken("Artesão F", "ff@prod.com", "senha123");
        UUID catCestaria = criarCategoria(token, "Cestaria F");
        UUID catTecidos = criarCategoria(token, "Tecidos F");

        criarProduto(token, catCestaria, "Cesto", 40.0, 5);
        criarProduto(token, catCestaria, "Chapéu de Palha", 25.0, 8);
        criarProduto(token, catTecidos, "Tapete", 80.0, 3);

        mockMvc.perform(get("/api/produtos/filtro").param("categoriaId", catCestaria.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void buscarPorNome_retornaCorrespondentes() throws Exception {
        String token = registrarEObterToken("Artesão S", "ss@prod.com", "senha123");
        UUID catId = criarCategoria(token, "CatS");
        criarProduto(token, catId, "Cesto Artesanal", 50.0, 10);
        criarProduto(token, catId, "Tapete Artesanal", 100.0, 5);
        criarProduto(token, catId, "Vaso de Barro", 35.0, 7);

        mockMvc.perform(comToken(get("/api/produtos/search").param("nome", "Artesanal"), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void produtoNaoExposeSenhaDeDono() throws Exception {
        String token = registrarEObterToken("Dono Seg", "donoseg@prod.com", "senha123");
        UUID catId = criarCategoria(token, "CatSeg");
        UUID prodId = criarProduto(token, catId, "Produto Seg", 10.0, 1);

        mockMvc.perform(get("/api/produtos/filtro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].autor.senha").doesNotExist());
    }
}
