ALTER TABLE mensagem ADD COLUMN status VARCHAR(20) DEFAULT 'ENVIADA' NOT NULL;

ALTER TABLE mensagem DROP COLUMN IF EXISTS lida;

-- índice para deixar a busca do histórico de mensagens mais rápida
CREATE INDEX idx_mensagem_conversa_id ON mensagem(conversa_id);