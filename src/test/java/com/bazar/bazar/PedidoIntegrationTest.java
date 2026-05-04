package com.bazar.bazar;

import com.bazar.bazar.repositories.ItemPedidoRepository;
import com.bazar.bazar.repositories.ProdutoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PedidoIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    @Test
    void criarPedido_decrementaEstoqueDoProduto() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor", "vendedor@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador", "comprador@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed");
        UUID prodId = criarProduto(tokenVendedor, catId, "Produto Pedido", 50.0, 10);
        criarEndereco(tokenComprador);

        criarPedido(tokenComprador, vendedorId, prodId, 3);

        int estoque = produtoRepository.findById(prodId)
                .map(p -> p.getQuantidade())
                .orElseThrow();
        assertThat(estoque).isEqualTo(7);
    }

    @Test
    void criarPedido_estoqueInsuficiente_retorna400() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor2", "vendedor2@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador2", "comprador2@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed2");
        UUID prodId = criarProduto(tokenVendedor, catId, "Produto Escasso", 50.0, 2);
        criarEndereco(tokenComprador);

        String body = """
                {"vendedorId":"%s","remote":false,"itens":[{"produtoId":"%s","quantidade":5}]}
                """.formatted(vendedorId, prodId);

        mockMvc.perform(comToken(post("/api/pedidos"), tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void criarPedido_compradorSemEndereco_retorna404() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor3", "vendedor3@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador3", "comprador3@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed3");
        UUID prodId = criarProduto(tokenVendedor, catId, "Produto Sem End", 50.0, 5);
        // Comprador NÃO cria endereço

        String body = """
                {"vendedorId":"%s","remote":false,"itens":[{"produtoId":"%s","quantidade":1}]}
                """.formatted(vendedorId, prodId);

        mockMvc.perform(comToken(post("/api/pedidos"), tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void removerItemPedido_restauraEstoqueDoProduto() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor4", "vendedor4@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador4", "comprador4@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed4");
        UUID prodId = criarProduto(tokenVendedor, catId, "Produto Remover", 50.0, 10);
        criarEndereco(tokenComprador);
        UUID pedidoId = criarPedido(tokenComprador, vendedorId, prodId, 4);

        // Estoque deve ser 6 após pedido
        assertThat(produtoRepository.findById(prodId).map(p -> p.getQuantidade()).orElseThrow())
                .isEqualTo(6);

        // Busca o ID do item do pedido
        var resultado = mockMvc.perform(comToken(get("/api/pedidos/{id}", pedidoId), tokenComprador))
                .andExpect(status().isOk())
                .andReturn();
        String itemId = objectMapper.readTree(resultado.getResponse().getContentAsString())
                .get("itens").get(0).get("id").asText();

        // Remove o item
        mockMvc.perform(comToken(delete("/api/pedidos/{pedidoId}/items/{itemId}", pedidoId, itemId), tokenComprador))
                .andExpect(status().isOk());

        // Estoque restaurado para 10
        assertThat(produtoRepository.findById(prodId).map(p -> p.getQuantidade()).orElseThrow())
                .isEqualTo(10);
    }

    @Test
    void buscarPedido_naoRelacionado_retorna403() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor5", "vendedor5@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador5", "comprador5@ped.com", "senha123");
        String tokenTerceiro = registrarEObterToken("Terceiro", "terceiro@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed5");
        UUID prodId = criarProduto(tokenVendedor, catId, "Prod5", 50.0, 10);
        criarEndereco(tokenComprador);
        UUID pedidoId = criarPedido(tokenComprador, vendedorId, prodId, 1);

        mockMvc.perform(comToken(get("/api/pedidos/{id}", pedidoId), tokenTerceiro))
                .andExpect(status().isForbidden());
    }

    @Test
    void listarPedidos_retornaApenasDoUsuarioLogado() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor6", "vendedor6@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador6", "comprador6@ped.com", "senha123");
        String tokenNaoRelacionado = registrarEObterToken("NaoRel", "naorel@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed6");
        UUID prodId = criarProduto(tokenVendedor, catId, "Prod6", 50.0, 10);
        criarEndereco(tokenComprador);
        criarPedido(tokenComprador, vendedorId, prodId, 1);

        // Comprador vê o pedido
        mockMvc.perform(comToken(get("/api/pedidos"), tokenComprador))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Vendedor vê o pedido (como vendedor)
        mockMvc.perform(comToken(get("/api/pedidos"), tokenVendedor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // Não relacionado não vê nada
        mockMvc.perform(comToken(get("/api/pedidos"), tokenNaoRelacionado))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void deletarPedido_removeCascataOsItens() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor7", "vendedor7@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador7", "comprador7@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed7");
        UUID prodId = criarProduto(tokenVendedor, catId, "Prod7", 50.0, 10);
        criarEndereco(tokenComprador);
        UUID pedidoId = criarPedido(tokenComprador, vendedorId, prodId, 2);

        long itensAntes = itemPedidoRepository.count();
        assertThat(itensAntes).isEqualTo(1);

        mockMvc.perform(comToken(delete("/api/pedidos/{id}", pedidoId), tokenComprador))
                .andExpect(status().isNoContent());

        long itensDepois = itemPedidoRepository.count();
        assertThat(itensDepois).isEqualTo(0);
    }

    @Test
    void criarPedido_multiplosItens_decrementaEstoqueDeTodos() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor8", "vendedor8@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador8", "comprador8@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed8");
        UUID prodId1 = criarProduto(tokenVendedor, catId, "Prod8A", 50.0, 10);
        UUID prodId2 = criarProduto(tokenVendedor, catId, "Prod8B", 30.0, 8);
        criarEndereco(tokenComprador);

        String body = """
                {"vendedorId":"%s","remote":true,"itens":[
                    {"produtoId":"%s","quantidade":3},
                    {"produtoId":"%s","quantidade":2}
                ]}
                """.formatted(vendedorId, prodId1, prodId2);

        mockMvc.perform(comToken(post("/api/pedidos"), tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itens", hasSize(2)));

        assertThat(produtoRepository.findById(prodId1).map(p -> p.getQuantidade()).orElseThrow())
                .isEqualTo(7);
        assertThat(produtoRepository.findById(prodId2).map(p -> p.getQuantidade()).orElseThrow())
                .isEqualTo(6);
    }

    @Test
    void adicionarItemEmPedidoExistente_decrementaEstoque() throws Exception {
        String tokenVendedor = registrarEObterToken("Vendedor9", "vendedor9@ped.com", "senha123");
        String tokenComprador = registrarEObterToken("Comprador9", "comprador9@ped.com", "senha123");
        ativarLoja(tokenVendedor);
        UUID vendedorId = obterIdUsuarioLogado(tokenVendedor);
        UUID catId = criarCategoria(tokenVendedor, "CatPed9");
        UUID prodId = criarProduto(tokenVendedor, catId, "Prod9", 50.0, 10);
        criarEndereco(tokenComprador);
        UUID pedidoId = criarPedido(tokenComprador, vendedorId, prodId, 1);

        // Adiciona mais 2 unidades do mesmo produto no pedido
        String body = """
                {"produtoId":"%s","quantidade":2}
                """.formatted(prodId);
        mockMvc.perform(comToken(post("/api/pedidos/{id}/items", pedidoId), tokenComprador)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(2)));

        assertThat(produtoRepository.findById(prodId).map(p -> p.getQuantidade()).orElseThrow())
                .isEqualTo(7);
    }
}
