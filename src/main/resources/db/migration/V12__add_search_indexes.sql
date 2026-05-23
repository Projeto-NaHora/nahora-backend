-- Tabela de categorias para listagem na tela de busca (UC-16)
CREATE TABLE categoria (
    id                 BIGSERIAL PRIMARY KEY,
    nome               VARCHAR(100) NOT NULL,
    icone              VARCHAR(255),
    categoria_servico  VARCHAR(50)  NOT NULL UNIQUE,
    valor_sugerido_min NUMERIC(10, 2),
    valor_sugerido_max NUMERIC(10, 2)
);

-- Seed das 5 categorias mapeadas ao enum CategoriaServico
INSERT INTO categoria (nome, icone, categoria_servico, valor_sugerido_min, valor_sugerido_max)
VALUES
    ('Elétrica',        'eletrica',        'ELETRICA',        80.00,  350.00),
    ('Pedreiro',        'pedreiro',        'PEDREIRO',        100.00, 500.00),
    ('Encanamento',     'encanamento',     'ENCANAMENTO',     90.00,  400.00),
    ('Pintura',         'pintura',         'PINTURA',         150.00, 800.00),
    ('Marcenaria',       'marcenaria',       'MARCENARIA',       120.00, 600.00);

-- Campos de localização textual e texto de especialidades no profissional
ALTER TABLE profissional
    ADD COLUMN cidade                  VARCHAR(100),
    ADD COLUMN estado                  VARCHAR(2),
    ADD COLUMN descricao_especialidades TEXT;

-- Índices de busca para profissional (UH-07)
CREATE INDEX IF NOT EXISTS idx_profissional_perfil_completo
    ON profissional (perfil_completo);

CREATE INDEX IF NOT EXISTS idx_profissional_nota_media
    ON profissional (nota_media);

CREATE INDEX IF NOT EXISTS idx_usuario_ativo
    ON usuario (ativo);

CREATE INDEX IF NOT EXISTS idx_profissional_categorias_valor
    ON profissional_categorias (categorias_atendidas);
