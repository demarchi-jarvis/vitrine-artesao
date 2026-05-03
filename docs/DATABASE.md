# Banco de Dados — independencia-backend

## Configuração

| Parâmetro | Valor padrão (dev) | Variável de ambiente |
|---|---|---|
| URL | `jdbc:postgresql://localhost:5432/bazar` | `DB_URL` |
| Usuário | `postgres` | `DB_USERNAME` |
| Senha | `password` | `DB_PASSWORD` |
| Dialeto | PostgreSQL (inferido pelo driver) | — |

---

## Estratégia de migrações

**Desenvolvimento:** `spring.jpa.hibernate.ddl-auto=update`
O Hibernate cria e atualiza tabelas automaticamente. Usar apenas em ambiente local.

**Produção:** Flyway gerencia todas as migrações.
Scripts em: `src/main/resources/db/migration/`
Convenção de nomes Flyway: `V{versao}__{descricao}.sql`

```
db/migration/
├── V1__create_tables.sql
├── V2__add_indexes.sql
├── V3__alter_produto_add_descricao.sql
└── ...
```

**Importante:** `spring.flyway.enabled=true` com `ddl-auto=none` em produção para evitar conflito entre Flyway e Hibernate.

---

## Tabelas

### usuarios

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK, gerado pelo Hibernate |
| nome | VARCHAR | NULL | |
| email | VARCHAR | NULL | Único no negócio (sem constraint DB ainda) |
| senha | VARCHAR | NULL | Hash BCrypt |
| foto | VARCHAR | NULL | URL |
| telefone | VARCHAR | NULL | |
| pontos | BIGINT | NULL | Programa de fidelidade |
| cpf | VARCHAR | NULL | |
| cnpj | VARCHAR | NULL | Preenchido se MEI/empresa |
| loja | BOOLEAN | NULL | true = artesão ativo |

---

### produto

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK |
| nome | VARCHAR | NULL | |
| preco | DOUBLE | NOT NULL | |
| categoria_id | UUID | NULL | FK → categoria.id |
| autor_id | UUID | NULL | FK → usuarios.id |
| quantidade | INTEGER | NOT NULL | Estoque |
| imagem | VARCHAR | NULL | URL |
| descricao | VARCHAR | NULL | |
| icone | VARCHAR | NULL | URL thumbnail |
| data_criacao | TIMESTAMP | NULL | |

---

### categoria

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK |
| nome | VARCHAR | NULL | Validado único no service |
| descricao | VARCHAR | NULL | |
| icone | VARCHAR | NULL | |

---

### pedido

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK |
| cliente_id | UUID | NOT NULL | FK → usuarios.id |
| vendedor_id | UUID | NOT NULL | FK → usuarios.id |
| endereco_entrega | UUID | NOT NULL | FK → endereco.id (OneToOne) |
| remote | BOOLEAN | NULL | Entrega remota/presencial |
| data_criacao | TIMESTAMP | NULL | |
| data_entrega | TIMESTAMP | NULL | |

---

### item_pedido

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK |
| pedido_id | UUID | NOT NULL | FK → pedido.id (cascade delete) |
| produto_id | UUID | NOT NULL | FK → produto.id |
| vendedor_id | UUID | NOT NULL | FK → usuarios.id (snapshot do artesão) |
| comprador_id | UUID | NOT NULL | FK → usuarios.id |
| quantidade | INTEGER | NOT NULL | |

---

### endereco

| Coluna | Tipo | Nullable | Observação |
|---|---|---|---|
| id | UUID | NOT NULL | PK |
| cidade | VARCHAR | NULL | |
| estado | VARCHAR | NULL | |
| cep | VARCHAR | NULL | |
| rua | VARCHAR | NULL | |
| numero | BIGINT | NULL | |
| adicional | VARCHAR | NULL | |
| bairro | VARCHAR | NULL | |
| complemento | VARCHAR | NULL | |
| usuario_id | UUID | NULL | FK → usuarios.id (1:1 no negócio) |

---

### cliente (legado)

| Coluna | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| nome | VARCHAR | |
| email | VARCHAR | |
| data_criacao | TIMESTAMP | |

Tabela legada. Não usar em novas features — usar `usuarios`.

---

### vendas

| Coluna | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| vendedor_id | VARCHAR | |
| email | VARCHAR | |
| data_criacao | TIMESTAMP | |

Placeholder de auditoria. Sem fluxo implementado.

---

### evento

| Coluna | Tipo | Observação |
|---|---|---|
| id | UUID | PK |
| titulo | VARCHAR | |
| descricao | VARCHAR | |
| pedido_id | UUID | |
| data_criacao | TIMESTAMP | |
| remote | BOOLEAN | |
| evento_url | VARCHAR | |

Placeholder para notificações de pedido.

---

## Script de criação para Flyway (V1)

Quando migrar de `ddl-auto=update` para Flyway em produção, criar:

`src/main/resources/db/migration/V1__create_tables.sql`

```sql
CREATE TABLE IF NOT EXISTS usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255),
    email VARCHAR(255) UNIQUE NOT NULL,
    senha VARCHAR(255),
    foto VARCHAR(500),
    telefone VARCHAR(20),
    pontos BIGINT DEFAULT 0,
    cpf VARCHAR(14),
    cnpj VARCHAR(18),
    loja BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255) NOT NULL,
    descricao VARCHAR(500),
    icone VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS produto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome VARCHAR(255),
    preco DOUBLE PRECISION NOT NULL,
    categoria_id UUID REFERENCES categoria(id),
    autor_id UUID REFERENCES usuarios(id),
    quantidade INTEGER NOT NULL DEFAULT 0,
    imagem VARCHAR(500),
    descricao TEXT,
    icone VARCHAR(500),
    data_criacao TIMESTAMP
);

CREATE TABLE IF NOT EXISTS endereco (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cidade VARCHAR(255),
    estado VARCHAR(2),
    cep VARCHAR(9),
    rua VARCHAR(255),
    numero BIGINT,
    adicional VARCHAR(255),
    bairro VARCHAR(255),
    complemento VARCHAR(255),
    usuario_id UUID REFERENCES usuarios(id)
);

CREATE TABLE IF NOT EXISTS pedido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id UUID NOT NULL REFERENCES usuarios(id),
    vendedor_id UUID NOT NULL REFERENCES usuarios(id),
    endereco_entrega UUID NOT NULL REFERENCES endereco(id),
    remote BOOLEAN DEFAULT FALSE,
    data_criacao TIMESTAMP,
    data_entrega TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item_pedido (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id UUID NOT NULL REFERENCES produto(id),
    vendedor_id UUID NOT NULL REFERENCES usuarios(id),
    comprador_id UUID NOT NULL REFERENCES usuarios(id),
    quantidade INTEGER NOT NULL
);
```

---

## Índices recomendados

```sql
-- V2__add_indexes.sql
CREATE INDEX idx_produto_autor ON produto(autor_id);
CREATE INDEX idx_produto_categoria ON produto(categoria_id);
CREATE INDEX idx_produto_nome ON produto(nome);
CREATE INDEX idx_pedido_cliente ON pedido(cliente_id);
CREATE INDEX idx_pedido_vendedor ON pedido(vendedor_id);
CREATE INDEX idx_item_pedido_pedido ON item_pedido(pedido_id);
CREATE INDEX idx_item_pedido_vendedor ON item_pedido(vendedor_id);
CREATE INDEX idx_item_pedido_comprador ON item_pedido(comprador_id);
CREATE UNIQUE INDEX idx_endereco_usuario ON endereco(usuario_id);
CREATE UNIQUE INDEX idx_usuario_email ON usuarios(email);
```

---

## Considerações de produção

- Usar PostgreSQL 15+ para suporte completo a UUID nativo
- Habilitar `pg_stat_statements` para monitorar queries lentas
- Backup automático via pg_dump ou RDS automated backups (se AWS)
- Separar banco de dev e prod — nunca compartilhar credenciais
