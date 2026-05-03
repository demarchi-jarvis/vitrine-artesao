# Domínio — Vitrine Virtual

## Conceito do negócio

A Vitrine Virtual conecta **artesãos** (vendedores) a **compradores** dentro do Vale do Café. Um mesmo usuário pode ser artesão e comprador simultaneamente — o sistema suporta múltiplos perfis por conta.

---

## Entidades principais

### Usuario
Entidade central. Representa qualquer pessoa no sistema — artesão, comprador ou ambos.

```java
@Entity @Table(name = "usuarios")
class Usuario {
    UUID id
    String nome
    String email          // único, usado como login
    String senha          // hash BCrypt
    String foto           // URL da foto de perfil
    String telefone
    Long pontos           // programa de fidelidade (futuro)
    String cpf
    String cnpj           // preenchido se for MEI/empresa
    Boolean loja          // TRUE = é artesão com loja ativa
}
```

**Regras:**
- `email` é único no sistema
- `loja = false` por padrão ao registrar
- `foto` recebe URL padrão se não informada no registro
- Um usuário com `loja = true` aparece nos resultados de `/api/usuarios/perfis`
- Artesão pode ter `cnpj` (MEI) ou `cpf` (pessoa física)

---

### Produto
Pertence a um artesão (`autor`) e a uma `Categoria`.

```java
@Entity @Table(name = "produto")
class Produto {
    UUID id
    String nome
    double preco
    Categoria categoria   // ManyToOne
    Usuario autor         // ManyToOne — artesão dono do produto
    int quantidade        // estoque disponível
    String imagem         // URL da imagem principal
    String descricao
    String icone          // URL do ícone/thumbnail
    LocalDateTime dataCriacao
}
```

**Regras:**
- Apenas o `autor` pode editar ou deletar o produto (verificado no Controller)
- `quantidade` é decrementada ao criar pedido e deve ser >= quantidade solicitada
- `dataCriacao` é setada automaticamente no Controller ao criar
- Produto sem estoque (`quantidade = 0`) ainda aparece na listagem

---

### Categoria
Taxonomia para organizar produtos.

```java
@Entity @Table(name = "categoria")
class Categoria {
    UUID id
    String nome       // único (validado no CategoriaService)
    String descricao
    String icone
}
```

**Regras:**
- Nome de categoria não pode se repetir (case-insensitive)
- Categorias sem produtos ainda existem no sistema
- A query `findDistinctCategoriasByProduto` retorna apenas categorias que têm ao menos 1 produto

---

### Pedido
Representa uma transação entre comprador e artesão.

```java
@Entity @Table(name = "pedido")
class Pedido {
    UUID id
    Usuario cliente         // comprador (ManyToOne LAZY)
    Usuario vendedor        // artesão (ManyToOne LAZY)
    Endereco enderecoEntrega // OneToOne EAGER
    Boolean remote          // entrega remota (frete) ou presencial
    LocalDateTime dataCriacao
    LocalDateTime dataEntrega
    List<ItemPedido> itens  // OneToMany CASCADE ALL
}
```

**Regras:**
- `cliente` = usuário logado que fez o pedido
- `vendedor` = artesão informado no `PedidoRequest.vendedorId`
- `enderecoEntrega` = endereço cadastrado do comprador (buscado automaticamente)
- Um pedido pode ter múltiplos itens, potencialmente de produtos diferentes
- Deletar o pedido remove os itens em cascata (`CASCADE ALL + orphanRemoval`)
- `dataEntrega` = valor do request; se não informado, assume data atual

---

### ItemPedido
Linha de um pedido: liga produto, quantidade, comprador e vendedor.

```java
@Entity @Table(name = "item_pedido")
class ItemPedido {
    UUID id
    Pedido pedido       // ManyToOne (pedido pai)
    Produto produto     // ManyToOne EAGER
    Usuario vendedor    // artesão dono do produto no momento da compra
    Usuario comprador   // quem comprou (= cliente do Pedido)
    int quantidade
}
```

**Regras:**
- `vendedor` = `produto.getAutor()` no momento da criação — snapshot do vendedor
- `comprador` = usuário logado (cliente do pedido)
- `quantidade > 0` obrigatório
- Estoque (`produto.quantidade`) é decrementado ao adicionar item

---

### Endereco
Um por usuário. Usado como endereço de entrega nos pedidos.

```java
@Entity @Table(name = "endereco")
class Endereco {
    UUID id
    String cidade
    String estado
    String cep
    String rua
    Long numero
    String adicional
    String bairro
    String complemento
    Usuario usuario   // ManyToOne — dono do endereço
}
```

**Regras:**
- Cada usuário tem no máximo 1 endereço (EnderecoRepository retorna `Optional`)
- O `usuario` do endereço não pode ser alterado pelo endpoint de update (prevenção de security gap)
- Ao criar pedido, o endereço é buscado automaticamente pelo ID do comprador

---

## Relacionamentos (diagrama)

```
Usuario ──< Produto         (1 artesão tem N produtos)
Usuario ──< Pedido.cliente  (1 comprador faz N pedidos)
Usuario ──< Pedido.vendedor (1 artesão recebe N pedidos)
Pedido ──< ItemPedido       (1 pedido tem N itens, CASCADE ALL)
Produto ──< ItemPedido      (1 produto aparece em N itens)
Categoria ──< Produto       (1 categoria tem N produtos)
Usuario ──1 Endereco        (1 usuário tem 1 endereço)
```

---

## Entidades secundárias / legado

| Entidade | Status | Observação |
|---|---|---|
| `Cliente` | Legado | Substituído por `Usuario`. Mantido para compatibilidade |
| `Vendas` | Placeholder | Entidade de auditoria sem fluxo implementado |
| `Evento` | Placeholder | Pensado para notificações de pedido; sem fluxo ativo |

---

## Múltiplos perfis — como funciona

O campo `loja: Boolean` é o pivô. Um usuário pode:

| loja | Pode fazer | Não pode fazer |
|---|---|---|
| `false` | Comprar produtos, criar pedidos, ter endereço | Criar/editar produtos, aparecer em /perfis |
| `true` | Tudo acima + criar/editar produtos, ter loja visível | — |

A ativação/desativação da loja é feita via `PATCH /api/usuarios/loja/status` com `{"status": true/false}`.
