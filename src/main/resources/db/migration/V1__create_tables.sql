-- V1: Schema inicial — Vitrine Virtual

CREATE TABLE IF NOT EXISTS usuarios (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(255),
    email       VARCHAR(255) UNIQUE NOT NULL,
    senha       VARCHAR(255) NOT NULL,
    foto        VARCHAR(500),
    telefone    VARCHAR(20),
    pontos      BIGINT DEFAULT 0,
    cpf         VARCHAR(14),
    cnpj        VARCHAR(18),
    loja        BOOLEAN DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS categoria (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        VARCHAR(255) NOT NULL,
    descricao   VARCHAR(500),
    icone       VARCHAR(500)
);

CREATE TABLE IF NOT EXISTS produto (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome         VARCHAR(255),
    preco        DOUBLE PRECISION NOT NULL CHECK (preco >= 0),
    categoria_id UUID REFERENCES categoria(id),
    autor_id     UUID REFERENCES usuarios(id),
    quantidade   INTEGER NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    imagem       VARCHAR(500),
    descricao    TEXT,
    icone        VARCHAR(500),
    data_criacao TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS endereco (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cidade      VARCHAR(255),
    estado      CHAR(2),
    cep         VARCHAR(9),
    rua         VARCHAR(255),
    numero      BIGINT,
    adicional   VARCHAR(255),
    bairro      VARCHAR(255),
    complemento VARCHAR(255),
    usuario_id  UUID REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS pedido (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cliente_id        UUID NOT NULL REFERENCES usuarios(id),
    vendedor_id       UUID NOT NULL REFERENCES usuarios(id),
    endereco_entrega  UUID NOT NULL REFERENCES endereco(id),
    remote            BOOLEAN DEFAULT FALSE,
    data_criacao      TIMESTAMP DEFAULT NOW(),
    data_entrega      TIMESTAMP
);

CREATE TABLE IF NOT EXISTS item_pedido (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pedido_id    UUID NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
    produto_id   UUID NOT NULL REFERENCES produto(id),
    vendedor_id  UUID NOT NULL REFERENCES usuarios(id),
    comprador_id UUID NOT NULL REFERENCES usuarios(id),
    quantidade   INTEGER NOT NULL CHECK (quantidade > 0)
);

-- Tabelas legadas mantidas para compatibilidade
CREATE TABLE IF NOT EXISTS cliente (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nome         VARCHAR(255),
    email        VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS vendas (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    vendedor_id  VARCHAR(255),
    email        VARCHAR(255),
    data_criacao TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS evento (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    titulo      VARCHAR(255),
    descricao   TEXT,
    pedido_id   UUID,
    data_criacao TIMESTAMP DEFAULT NOW(),
    remote      BOOLEAN,
    evento_url  VARCHAR(500)
);
