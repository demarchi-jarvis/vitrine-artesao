# API Reference — independencia-backend

Base URL: `http://localhost:8081`

Autenticação: `Authorization: Bearer <token>` em todos os endpoints marcados com **[AUTH]**.

---

## Autenticação

### POST /api/autenticacao/registrar
Cria nova conta. Retorna token JWT.

**Body:**
```json
{
  "nome": "Maria Artesã",
  "email": "maria@email.com",
  "senha": "senha123"
}
```

**Response 200:**
```json
{
  "nome": "Maria Artesã",
  "token": "eyJhbGci..."
}
```

| Status | Significado |
|---|---|
| 200 | Cadastro realizado, token gerado |
| 409 | Email já cadastrado |

---

### POST /api/autenticacao/login
Autentica usuário existente.

**Body:**
```json
{
  "email": "maria@email.com",
  "senha": "senha123"
}
```

**Response 200:**
```json
{
  "nome": "Maria Artesã",
  "token": "eyJhbGci..."
}
```

| Status | Significado |
|---|---|
| 200 | Login OK, token válido por 2h |
| 401 | Credenciais inválidas |

---

## Usuários

### GET /api/usuarios/logado [AUTH]
Retorna dados do usuário autenticado.

**Response 200:**
```json
{
  "id": "uuid",
  "nome": "Maria Artesã",
  "email": "maria@email.com",
  "cpf": "123.456.789-00",
  "cnpj": null,
  "telefone": "(24) 99999-9999",
  "foto": "https://...",
  "pontos": 0,
  "loja": false
}
```

---

### PATCH /api/usuarios/alterar [AUTH]
Atualiza dados do perfil do usuário logado. Campos não enviados não são alterados.

**Body (todos opcionais):**
```json
{
  "nome": "Novo Nome",
  "foto": "https://nova-foto.jpg",
  "telefone": "(24) 99999-9999",
  "cpf": "123.456.789-00"
}
```

**Response 200:** UsuarioDTO atualizado.

---

### PATCH /api/usuarios/loja/status [AUTH]
Ativa ou desativa a loja do usuário logado.

**Body:**
```json
{ "status": true }
```

**Response 200:** UsuarioDTO com `loja` atualizado.

---

### GET /api/usuarios/perfis [AUTH]
Lista artesãos com loja ativa. Suporta busca por nome e paginação.

**Query params:**
- `nome` (opcional) — filtro parcial, case-insensitive
- `page` (padrão 0)
- `size` (padrão 12)
- `sort` (padrão `nome`)

**Response 200:**
```json
{
  "content": [ { ...UsuarioDTO } ],
  "page": 0,
  "size": 12,
  "totalElements": 5,
  "totalPages": 1
}
```

---

### GET /api/usuarios/dono?email=xxx [AUTH]
Busca o artesão dono de uma loja pelo email.

**Query param:** `email`

**Response 200:** Objeto `Usuario` completo.

---

## Produtos

### POST /api/produtos [AUTH]
Cria novo produto. O autor é o usuário logado.

**Body:**
```json
{
  "nome": "Cesto de Bambu",
  "preco": 89.90,
  "quantidade": 10,
  "imagem": "https://...",
  "icone": "https://...",
  "categoriaId": "uuid-da-categoria"
}
```

**Response 201:** Objeto `Produto` criado.

---

### GET /api/produtos/{id} [AUTH]
Busca produto por ID. Retorna flag `ehAutor` indicando se o usuário logado é o dono.

**Response 200:**
```json
{
  "id": "uuid",
  "nome": "Cesto de Bambu",
  "preco": 89.90,
  "quantidade": 10,
  "ehAutor": true,
  ...
}
```

---

### GET /api/produtos [AUTH]
Lista todos os produtos paginado.

**Query params:** `page`, `size`, `sort` (padrão: `page=0 size=12 sort=nome`)

**Response 200:** `Page<Produto>`

---

### GET /api/produtos/filtro
Lista produtos com filtro opcional de categoria. **Rota pública** (sem JWT).

**Query params:**
- `categoriaId` (UUID, opcional)
- `page`, `size`, `sort`

**Response 200:** `Page<Produto>`

---

### GET /api/produtos/search?nome=xxx [AUTH]
Busca produtos por nome (parcial, case-insensitive).

**Response 200:** `List<Produto>`
**Response 204:** Nenhum resultado encontrado.

---

### GET /api/produtos/meus-produtos [AUTH]
Lista produtos do artesão logado.

**Response 200:** `List<Produto>`

---

### GET /api/produtos/loja?email=xxx [AUTH]
Lista produtos de um artesão específico pelo email.

**Response 200:** `List<Produto>`

---

### PATCH /api/produtos/{id} [AUTH]
Atualiza produto. Apenas o artesão autor pode atualizar.

**Body:** Objeto `Produto` com campos a atualizar.

| Status | Significado |
|---|---|
| 200 | Atualizado |
| 403 | Usuário não é o autor |
| 404 | Produto não encontrado |

---

### DELETE /api/produtos/{id} [AUTH]
Deleta produto.

**Response 204:** Deletado.

---

### GET /api/produtos/categorias [AUTH]
Retorna lista de categorias que possuem ao menos um produto.

**Response 200:** `List<Categoria>`

---

### PATCH /api/produtos/icone/{id} [AUTH]
Atualiza apenas o ícone de um produto.

**Body:** String com a URL do ícone.

**Response 200:** Produto atualizado.

---

## Categorias

### POST /api/categorias [AUTH]
**Body:**
```json
{
  "nome": "Cestaria",
  "descricao": "Produtos de palha e bambu",
  "icone": "https://..."
}
```
**Response 201:** Categoria criada.

| Status | Situação |
|---|---|
| 201 | Criada |
| 400 | Nome já existe |

---

### GET /api/categorias [AUTH]
Lista todas as categorias.

### GET /api/categorias/{id} [AUTH]
Busca por ID.

### GET /api/categorias/search?nome=xxx [AUTH]
Busca por nome.

### PUT /api/categorias/{id} [AUTH]
Atualiza categoria completa.

### DELETE /api/categorias/{id} [AUTH]
Deleta categoria.

---

## Pedidos

### POST /api/pedidos [AUTH]
Cria pedido. O comprador é o usuário logado. O endereço de entrega é buscado automaticamente.

**Body:**
```json
{
  "vendedorId": "uuid-do-artesao",
  "remote": true,
  "dataEntrega": "2025-06-01T10:00:00",
  "itens": [
    { "produtoId": "uuid-produto", "quantidade": 2 },
    { "produtoId": "uuid-produto2", "quantidade": 1 }
  ]
}
```

**Response 201:** `PedidoResponse`

| Status | Situação |
|---|---|
| 201 | Pedido criado, estoque decrementado |
| 400 | Quantidade inválida ou estoque insuficiente |
| 404 | Comprador sem endereço cadastrado / vendedor não encontrado / produto não encontrado |

---

### GET /api/pedidos [AUTH]
Lista todos os pedidos.

### GET /api/pedidos/{id} [AUTH]
Busca pedido por ID com itens.

### PUT /api/pedidos/{id} [AUTH]
Atualiza pedido existente.

### DELETE /api/pedidos/{id} [AUTH]
Deleta pedido e seus itens (cascade).

---

### POST /api/pedidos/{pedidoId}/items [AUTH]
Adiciona item a pedido existente. Valida estoque.

**Body:** `{ "produtoId": "uuid", "quantidade": 1 }`

### DELETE /api/pedidos/{pedidoId}/items/{itemId} [AUTH]
Remove item de pedido.

---

## Itens de pedido (visão do usuário)

### GET /api/item/comprador [AUTH]
Itens comprados pelo usuário logado, paginado.

**Query params:** `page` (padrão 0), `size` (padrão 10)

**Response 200:** `PaginaResponse<ItemPedidoDTO>` com dados de produto, pedido, endereço, vendedor e comprador.

---

### GET /api/item/vendedor [AUTH]
Itens vendidos pelo usuário logado (como artesão), paginado.

**Query params:** `page`, `size`

**Response 200:** `PaginaResponse<ItemPedidoDTO>`

---

## Endereço

### POST /api/endereco [AUTH]
Cria endereço para o usuário logado.

**Body:**
```json
{
  "rua": "Rua das Flores",
  "numero": 42,
  "complemento": "Apto 3",
  "bairro": "Centro",
  "cidade": "Vassouras",
  "estado": "RJ",
  "cep": "27700-000",
  "adicional": "Próximo à praça"
}
```

### GET /api/endereco/usuario [AUTH]
Retorna o endereço do usuário logado.

### GET /api/endereco/email?email=xxx [AUTH]
Retorna endereço de um usuário pelo email.

### PUT /api/endereco [AUTH]
Atualiza endereço do usuário logado. O campo `usuario` do body é ignorado (não pode trocar o dono).

### DELETE /api/endereco/{id} [AUTH]
Deleta endereço por ID.

---

## Estrutura de resposta padrão

### PaginaResponse<T>
```json
{
  "content": [ ... ],
  "page": 0,
  "size": 12,
  "totalElements": 100,
  "totalPages": 9
}
```

### PedidoResponse
```json
{
  "id": "uuid",
  "clienteId": "uuid",
  "vendedorId": "uuid",
  "enderecoEntregaId": "uuid",
  "remote": true,
  "dataCriacao": "2025-05-03T10:00:00",
  "dataEntrega": "2025-06-01T10:00:00",
  "itens": [
    {
      "id": "uuid",
      "produtoId": "uuid",
      "produtoNome": "Cesto de Bambu",
      "produtoPreco": 89.90,
      "quantidade": 2
    }
  ]
}
```
