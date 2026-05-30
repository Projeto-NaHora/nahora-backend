-- Remove a tabela join simples criada em V1 (sem id nem criado_em)
DROP TABLE IF EXISTS cliente_favoritos;

-- Tabela de favoritos com metadados para ordenação por data
CREATE TABLE favorito (
    id          BIGSERIAL PRIMARY KEY,
    cliente_id      BIGINT NOT NULL REFERENCES cliente(id) ON DELETE CASCADE,
    profissional_id BIGINT NOT NULL REFERENCES profissional(id) ON DELETE CASCADE,
    criado_em       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_favorito_cliente_profissional UNIQUE (cliente_id, profissional_id)
);

CREATE INDEX idx_favorito_cliente_id ON favorito(cliente_id);
