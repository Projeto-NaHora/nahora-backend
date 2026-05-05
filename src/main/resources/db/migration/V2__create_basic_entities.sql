-- Tabela base de Usuários
CREATE TABLE usuario (
                         id SERIAL PRIMARY KEY,
                         nome VARCHAR(255) NOT NULL,
                         email VARCHAR(255) NOT NULL UNIQUE,
                         telefone VARCHAR(20) NOT NULL UNIQUE,
                         senha VARCHAR(255) NOT NULL,
                         foto VARCHAR(255),
                         nota_media DOUBLE PRECISION DEFAULT 0.0,
                         total_avaliacoes INTEGER DEFAULT 0,
                         token_fcm VARCHAR(255),
                         ativo BOOLEAN NOT NULL DEFAULT TRUE,
                         criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                         atualizado_em TIMESTAMP
);

-- Tabela de Clientes
CREATE TABLE cliente (
                         id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE
);

-- Tabela de Endereços Salvos do Cliente
CREATE TABLE cliente_enderecos (
                                   cliente_id BIGINT REFERENCES cliente(id) ON DELETE CASCADE,
                                   logradouro VARCHAR(255),
                                   numero VARCHAR(50),
                                   complemento VARCHAR(255),
                                   bairro VARCHAR(255),
                                   cidade VARCHAR(255),
                                   estado VARCHAR(2),
                                   cep VARCHAR(20),
                                   coordenadas geometry(Point, 4326)
);

-- Tabela de Profissionais
CREATE TABLE profissional (
                              id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE,
                              bio TEXT,
                              localizacao geometry(Point, 4326),
                              raio_atuacao DOUBLE PRECISION,
                              total_servicos_executados INTEGER DEFAULT 0,
                              disponivel BOOLEAN DEFAULT FALSE,
                              documento_url VARCHAR(255),
                              status_verificacao VARCHAR(50),
                              plano_plus BOOLEAN DEFAULT FALSE
);

-- Tabela Auxiliar de Categorias do Profissional
CREATE TABLE profissional_categorias (
                                         profissional_id BIGINT REFERENCES profissional(id) ON DELETE CASCADE,
                                         categorias_atendidas VARCHAR(100)
);

-- Tabela Auxiliar de Portfólio do Profissional
CREATE TABLE profissional_portfolio (
                                        profissional_id BIGINT REFERENCES profissional(id) ON DELETE CASCADE,
                                        portfolio VARCHAR(255)
);

-- Tabela de Favoritos
CREATE TABLE cliente_favoritos (
                                   cliente_id BIGINT REFERENCES cliente(id) ON DELETE CASCADE,
                                   profissional_id BIGINT REFERENCES profissional(id) ON DELETE CASCADE,
                                   PRIMARY KEY (cliente_id, profissional_id)
);

-- Tabela de Pedidos
CREATE TABLE pedido (
                        id SERIAL PRIMARY KEY,
                        cliente_id BIGINT NOT NULL REFERENCES cliente(id),
                        categoria VARCHAR(100) NOT NULL,
                        descricao TEXT NOT NULL,
                        urgencia VARCHAR(50),
                        orcamento_estimado DECIMAL(10, 2),
                        status VARCHAR(50) NOT NULL,

    -- Endereço embarcado
                        logradouro VARCHAR(255),
                        numero VARCHAR(50),
                        complemento VARCHAR(255),
                        bairro VARCHAR(255),
                        cidade VARCHAR(255),
                        estado VARCHAR(2),
                        cep VARCHAR(20),
                        coordenadas geometry(Point, 4326),

                        criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabela Auxiliar de Fotos do Pedido
CREATE TABLE pedido_fotos (
                              pedido_id BIGINT REFERENCES pedido(id) ON DELETE CASCADE,
                              fotos VARCHAR(255)
);

-- Índices Espaciais (GiST) para buscas de raio de atuação do PostGIS
CREATE INDEX idx_profissional_localizacao ON profissional USING GIST(localizacao);
CREATE INDEX idx_pedido_coordenadas ON pedido USING GIST(coordenadas);