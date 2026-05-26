ALTER TABLE profissional ADD COLUMN IF NOT EXISTS profissao VARCHAR(255);
ALTER TABLE profissional ADD COLUMN IF NOT EXISTS cep VARCHAR(10);

UPDATE profissional SET status_verificacao = 'CADASTRO_INCOMPLETO'    WHERE status_verificacao = 'NAO_ENVIADO';
UPDATE profissional SET status_verificacao = 'AGUARDANDO_VERIFICACAO' WHERE status_verificacao = 'PENDENTE';
UPDATE profissional SET status_verificacao = 'VERIFICADO'             WHERE status_verificacao = 'APROVADO';
