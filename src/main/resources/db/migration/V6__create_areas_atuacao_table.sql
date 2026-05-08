-- Remove a coluna antiga que não suportava listas
ALTER TABLE profissional DROP COLUMN area_atuacao;

-- Cria a tabela auxiliar correta para os bairros/áreas
CREATE TABLE profissional_areas_atuacao (
                                            profissional_id BIGINT NOT NULL,
                                            bairro_ou_area VARCHAR(255) NOT NULL,
                                            CONSTRAINT fk_profissional_areas FOREIGN KEY (profissional_id) REFERENCES profissional (id) ON DELETE CASCADE
);

-- Índice para deixar a filtragem por bairro mais rápida
CREATE INDEX idx_areas_atuacao_bairro ON profissional_areas_atuacao(bairro_ou_area);