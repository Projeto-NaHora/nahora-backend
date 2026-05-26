CREATE TABLE IF NOT EXISTS admin (
    id BIGINT PRIMARY KEY REFERENCES usuario(id) ON DELETE CASCADE
);

INSERT INTO usuario (nome, email, telefone, senha, ativo, criado_em, atualizado_em)
VALUES (
    'Admin NaHora',
    'admin@nahora.com',
    '00000000000',
    '$2a$10$sQ.gyr7E7K0zVXZD/SiOQ.2j7DFZDqje.Acn9Ku7mXDq40FjKwWdi',
    true,
    NOW(),
    NOW()
);

INSERT INTO admin (id)
SELECT id FROM usuario WHERE email = 'admin@nahora.com';
