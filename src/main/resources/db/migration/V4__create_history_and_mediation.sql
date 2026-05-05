-- Tabela de Avaliação (Permite no máximo 1 avaliação do mesmo avaliador para o mesmo pedido)
CREATE TABLE avaliacao (
                           id SERIAL PRIMARY KEY,
                           pedido_id BIGINT NOT NULL REFERENCES pedido(id) ON DELETE CASCADE,
                           avaliador_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                           avaliado_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                           nota_geral INTEGER NOT NULL CHECK (nota_geral BETWEEN 1 AND 5),
                           pontualidade INTEGER CHECK (pontualidade BETWEEN 1 AND 5),
                           qualidade INTEGER CHECK (qualidade BETWEEN 1 AND 5),
                           comentario TEXT,
                           criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                           CONSTRAINT uk_avaliacao_pedido_avaliador UNIQUE (pedido_id, avaliador_id)
);

-- Tabela de Disputa (1:1 com Pedido)
CREATE TABLE disputa (
                         id SERIAL PRIMARY KEY,
                         pedido_id BIGINT NOT NULL UNIQUE REFERENCES pedido(id) ON DELETE CASCADE,
                         aberta_por_id BIGINT NOT NULL REFERENCES usuario(id),
                         motivo TEXT NOT NULL,
                         status VARCHAR(50) NOT NULL,
                         resolvida_por_id BIGINT REFERENCES usuario(id),
                         resolucao TEXT,
                         criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         atualizado_em TIMESTAMP
);

-- Tabela auxiliar para Evidências da Disputa
CREATE TABLE disputa_evidencias (
                                    disputa_id BIGINT REFERENCES disputa(id) ON DELETE CASCADE,
                                    evidencias VARCHAR(255)
);

-- Tabela de Denúncias
CREATE TABLE denuncia (
                          id SERIAL PRIMARY KEY,
                          denunciante_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          denunciado_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                          pedido_id BIGINT REFERENCES pedido(id) ON DELETE SET NULL,
                          motivo VARCHAR(50) NOT NULL,
                          descricao TEXT NOT NULL,
                          status VARCHAR(50) NOT NULL,
                          criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          atualizado_em TIMESTAMP
);

-- Tabela de Notificações In-App
CREATE TABLE notificacao (
                             id SERIAL PRIMARY KEY,
                             destinatario_id BIGINT NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
                             tipo VARCHAR(50) NOT NULL,
                             titulo VARCHAR(255) NOT NULL,
                             mensagem TEXT NOT NULL,
                             entidade_relacionada_id BIGINT,
                             entidade_relacionada_tipo VARCHAR(50),
                             lida BOOLEAN DEFAULT FALSE,
                             criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);