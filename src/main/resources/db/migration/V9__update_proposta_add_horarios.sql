-- Adapta a tabela proposta ao spec UC-25
ALTER TABLE proposta RENAME COLUMN valor TO valor_oferecido;
ALTER TABLE proposta DROP COLUMN IF EXISTS tempo_estimado;
ALTER TABLE proposta DROP COLUMN IF EXISTS expira_em;

-- Horários disponíveis oferecidos pelo profissional (1:N com proposta)
CREATE TABLE proposta_horario (
    id          SERIAL PRIMARY KEY,
    proposta_id BIGINT    NOT NULL REFERENCES proposta(id) ON DELETE CASCADE,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim    TIMESTAMP NOT NULL,
    CONSTRAINT chk_proposta_horario_fim_after_inicio CHECK (data_hora_fim > data_hora_inicio)
);
