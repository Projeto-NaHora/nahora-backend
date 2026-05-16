-- 1. Adicionar o profissional atribuído na tabela Pedido
ALTER TABLE pedido
ADD COLUMN profissional_atribuido_id BIGINT;

ALTER TABLE pedido
ADD CONSTRAINT fk_pedido_profissional_atribuido
FOREIGN KEY (profissional_atribuido_id) REFERENCES profissional(id);

-- 2. Adicionar campos de avaliação na tabela Profissional
ALTER TABLE profissional
ADD COLUMN nota_media NUMERIC(3,2) DEFAULT 5.00,
ADD COLUMN numero_avaliacoes INTEGER DEFAULT 0;

-- 3. Criar a tabela de horários da Proposta (ElementCollection)
CREATE TABLE proposta_horarios (
    proposta_id BIGINT NOT NULL,
    inicio TIMESTAMP NOT NULL,
    fim TIMESTAMP NOT NULL,
    CONSTRAINT fk_proposta_horarios_proposta
        FOREIGN KEY (proposta_id) REFERENCES proposta(id) ON DELETE CASCADE
);