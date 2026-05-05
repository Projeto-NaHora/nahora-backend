-- Tabela de Propostas
CREATE TABLE proposta (
                          id SERIAL PRIMARY KEY,
                          pedido_id BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
                          profissional_id BIGINT NOT NULL REFERENCES profissional(id) ON DELETE CASCADE,
                          valor DECIMAL(10, 2) NOT NULL,
                          descricao TEXT,
                          tempo_estimado VARCHAR(100),
                          status VARCHAR(50) NOT NULL,
                          expira_em TIMESTAMP,
                          criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          atualizado_em TIMESTAMP
);

-- Tabela de Pagamentos (Relação 1:1 com Proposta)
CREATE TABLE pagamento (
                           id SERIAL PRIMARY KEY,
                           proposta_id BIGINT NOT NULL UNIQUE REFERENCES proposta(id) ON DELETE CASCADE,
                           valor_bruto DECIMAL(10, 2) NOT NULL,
                           taxa_plataforma DECIMAL(10, 2) NOT NULL,
                           valor_liquido DECIMAL(10, 2) NOT NULL,
                           status VARCHAR(50) NOT NULL,
                           metodo VARCHAR(50) NOT NULL,
                           criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           atualizado_em TIMESTAMP
);

-- Tabela de Conversas (Atrelada ao Pedido e à Proposta Aceita)
CREATE TABLE conversa (
                          id SERIAL PRIMARY KEY,
                          pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedido(id) ON DELETE CASCADE,
                          proposta_id BIGINT NOT NULL UNIQUE REFERENCES proposta(id) ON DELETE CASCADE,
                          status VARCHAR(50) NOT NULL,
                          criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          atualizado_em TIMESTAMP
);

-- Tabela de Mensagens do Chat
CREATE TABLE mensagem (
                          id SERIAL PRIMARY KEY,
                          conversa_id BIGINT NOT NULL REFERENCES conversa(id) ON DELETE CASCADE,
                          remetente_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          conteudo TEXT NOT NULL,
                          anexo_url VARCHAR(255),
                          lida BOOLEAN DEFAULT FALSE,
                          bloqueada_ia BOOLEAN DEFAULT FALSE,
                          motivo_bloqueio VARCHAR(255),
                          criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);