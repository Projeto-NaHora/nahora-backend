-- Adicionar o profissional atribuído na tabela Pedido
ALTER TABLE pedido
ADD COLUMN profissional_atribuido_id BIGINT;

ALTER TABLE pedido
ADD CONSTRAINT fk_pedido_profissional_atribuido
FOREIGN KEY (profissional_atribuido_id) REFERENCES profissional(id);

-- Adicionar campos de avaliação específicos do profissional
ALTER TABLE profissional
ADD COLUMN nota_media NUMERIC(3,2) DEFAULT 5.00,
ADD COLUMN numero_avaliacoes INTEGER DEFAULT 0;
