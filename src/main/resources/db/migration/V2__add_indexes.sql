-- V2: Índices de performance

CREATE UNIQUE INDEX IF NOT EXISTS idx_usuario_email       ON usuarios(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_endereco_usuario    ON endereco(usuario_id);

CREATE INDEX IF NOT EXISTS idx_produto_autor              ON produto(autor_id);
CREATE INDEX IF NOT EXISTS idx_produto_categoria          ON produto(categoria_id);
CREATE INDEX IF NOT EXISTS idx_produto_nome               ON produto(nome);
CREATE INDEX IF NOT EXISTS idx_produto_data_criacao       ON produto(data_criacao DESC);

CREATE INDEX IF NOT EXISTS idx_pedido_cliente             ON pedido(cliente_id);
CREATE INDEX IF NOT EXISTS idx_pedido_vendedor            ON pedido(vendedor_id);
CREATE INDEX IF NOT EXISTS idx_pedido_data_criacao        ON pedido(data_criacao DESC);

CREATE INDEX IF NOT EXISTS idx_item_pedido_pedido         ON item_pedido(pedido_id);
CREATE INDEX IF NOT EXISTS idx_item_pedido_vendedor       ON item_pedido(vendedor_id);
CREATE INDEX IF NOT EXISTS idx_item_pedido_comprador      ON item_pedido(comprador_id);
CREATE INDEX IF NOT EXISTS idx_item_pedido_produto        ON item_pedido(produto_id);
