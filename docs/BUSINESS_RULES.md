# Regras de Negócio — Vitrine Virtual

## Perfis de usuário

### Regra 1 — Conta única, múltiplos papéis
Um mesmo usuário pode ser comprador e artesão simultaneamente. O campo `loja: Boolean` é o pivô.

- `loja = false` (padrão): pode comprar, não pode criar produtos
- `loja = true`: pode criar/editar produtos e aparece na vitrine de artesãos

**Ativação:** `PATCH /api/usuarios/loja/status` com `{"status": true}`

**Implicação de design:** Ao criar um pedido, o sistema distingue `cliente` (comprador) e `vendedor` (artesão) dentro da mesma tabela `usuarios`.

---

### Regra 2 — Email como identidade
O email é o identificador único do usuário no sistema. Não pode ser alterado após o cadastro. O endpoint `PATCH /api/usuarios/alterar` não expõe campo de email.

---

### Regra 3 — Propriedade de produto
Somente o artesão que criou o produto (`autor`) pode editá-lo ou deletá-lo. Verificado no Controller via:
```
produto.getAutor().getId().equals(usuarioLogado.getId())
```
Retorna 403 Forbidden se o usuário logado não for o autor.

---

## Fluxo de compra

### Regra 4 — Pré-requisito de endereço
Um comprador só pode criar um pedido se tiver um endereço cadastrado. O sistema busca automaticamente o endereço pelo `usuario.id`. Se não existir, retorna `404`.

**Consequência:** O frontend deve garantir que o usuário cadastrou endereço antes de exibir o botão de checkout.

---

### Regra 5 — Validação de estoque
Ao criar um pedido, para cada item:
1. `quantidade solicitada > 0` (obrigatório)
2. `produto.quantidade >= quantidade solicitada` (estoque suficiente)

Se qualquer item falhar, o pedido não é criado (transação é revertida).

---

### Regra 6 — Decremento de estoque
Ao criar o pedido com sucesso, o estoque de cada produto é decrementado imediatamente:
```
produto.quantidade -= itemRequest.quantidade
```

**Atenção:** Não há incremento de estoque ao deletar pedido. Se o pedido for cancelado, o estoque deve ser reajustado manualmente (funcionalidade ainda não implementada).

---

### Regra 7 — Snapshot do vendedor no ItemPedido
O `vendedor` no `ItemPedido` é definido como `produto.getAutor()` no momento da compra. Se o artesão transferir o produto (mudar o `autor`), os itens históricos mantêm referência ao vendedor original.

---

### Regra 8 — Endereço de entrega fixo no pedido
O `enderecoEntrega` do pedido é o endereço cadastrado do comprador no momento da compra. Se o comprador atualizar o endereço depois, o pedido existente não é afetado.

---

### Regra 9 — Pedido pode ter produtos de um único artesão
O campo `vendedor` do Pedido é único — um pedido é sempre com um artesão específico (`vendedorId` no request). Para comprar de dois artesãos diferentes, o comprador cria dois pedidos separados.

---

## Vitrine e busca

### Regra 10 — Vitrine pública
O endpoint `GET /api/produtos/filtro` é público (sem JWT). Qualquer visitante pode navegar os produtos sem estar logado.

Os demais endpoints de produto requerem autenticação.

---

### Regra 11 — Busca de artesãos
`GET /api/usuarios/perfis` retorna apenas usuários com `loja = true`. Se o parâmetro `nome` não for informado, retorna todos os artesãos paginados.

---

### Regra 12 — Produto sem estoque
Produtos com `quantidade = 0` continuam visíveis na listagem. A tentativa de compra é bloqueada na criação do pedido (Regra 5), não na exibição.

---

## Categorias

### Regra 13 — Nome único de categoria
O `CategoriaService` valida que não existe outra categoria com o mesmo nome (case-insensitive) antes de criar. Lança `RuntimeException` se duplicado.

---

### Regra 14 — Categorias ativas
`GET /api/produtos/categorias` retorna apenas categorias que têm ao menos um produto associado (via query JPQL `DISTINCT`). Categorias vazias ficam disponíveis no CRUD de categorias mas não aparecem na vitrine.

---

## Endereço

### Regra 15 — Um endereço por usuário
O sistema suporta apenas um endereço por usuário (`EnderecoRepository.findByUsuarioId` retorna `Optional`). Criar um segundo endereço para o mesmo usuário resulta em comportamento indefinido (o sistema salvará ambos, mas apenas um será encontrado nas buscas).

**Evolução sugerida:** Adicionar constraint `UNIQUE (usuario_id)` no banco e tratar o endpoint de criação como "criar ou atualizar".

---

### Regra 16 — Imutabilidade do dono do endereço
O endpoint `PUT /api/endereco` ignora o campo `usuario` do body. O endereço sempre pertence ao usuário logado. Isso previne que um usuário mal-intencionado transfira seu endereço para outro cadastro via API.

---

## Programa de fidelidade

### Regra 17 — Pontos (placeholder)
O campo `pontos` existe na entidade `Usuario` mas não há lógica de acúmulo ou resgate implementada. A infra está pronta para implementação futura.

---

## Fluxos de erro

| Cenário | HTTP Status | Mensagem |
|---|---|---|
| Login com senha errada | 401 | (sem body) |
| Registro com email duplicado | 409 | (sem body) |
| Pedido sem endereço | 404 | "Endereço de entrega não encontrado..." |
| Pedido com estoque insuficiente | 400 | "Estoque insuficiente para o produto: {nome}" |
| Pedido com quantidade ≤ 0 | 400 | "A quantidade do item deve ser positiva." |
| Editar produto de outro artesão | 403 | (sem body) |
| Produto não encontrado | 404 | "Produto não encontrado com ID: {id}" |
| Usuário não encontrado | 404 | "Usuário não encontrado com o ID: {id}" |
| Categoria duplicada | 500 | "Já existe uma categoria com este nome: {nome}" |

---

## Restrições do contexto Vassouras Tec

- A API deve suportar artesãos com pouca familiaridade digital — fluxos devem ser simples e ter respostas de erro compreensíveis
- O campo `remote: Boolean` nos pedidos permite distinguir entregas com frete (remote=true) de retiradas presenciais (remote=false), adequado à realidade local
- O campo `cnpj` permite que MEIs se cadastrem com CNPJ para emissão de nota fiscal futura
- A identidade cultural dos artesãos é refletida nas categorias — criar categorias como "Cestaria", "Tecelagem", "Cerâmica" antes do go-live
