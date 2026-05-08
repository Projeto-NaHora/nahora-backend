-- Adicionando as novas colunas na tabela profissional existente
ALTER TABLE profissional ADD COLUMN cpf VARCHAR(14) UNIQUE;
ALTER TABLE profissional ADD COLUMN area_atuacao VARCHAR(255);
ALTER TABLE profissional ADD COLUMN anos_experiencia INTEGER;
ALTER TABLE profissional ADD COLUMN valor_inicial DECIMAL(10, 2);
ALTER TABLE profissional ADD COLUMN perfil_completo BOOLEAN DEFAULT FALSE;

-- Criando a tabela para a lista de especialidades (@ElementCollection)
CREATE TABLE profissional_especialidades (
                                             profissional_id BIGINT REFERENCES profissional(id) ON DELETE CASCADE,
                                             especialidade VARCHAR(255)
);