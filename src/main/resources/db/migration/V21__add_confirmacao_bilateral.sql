ALTER TABLE pedido ADD COLUMN concluido_em TIMESTAMP NULL;
ALTER TABLE pedido ADD COLUMN foto_conclusao_url VARCHAR(512) NULL;
ALTER TABLE pedido ADD COLUMN auto_confirmado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_pedido_status_concluido_em ON pedido (status, concluido_em);
