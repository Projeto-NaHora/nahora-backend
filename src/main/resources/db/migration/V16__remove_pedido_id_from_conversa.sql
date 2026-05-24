-- Remove o índice criado na V14
DROP INDEX IF EXISTS idx_conversa_pedido_id;

-- Remove a coluna pedido_id (a FK é mantida via proposta_id -> proposta -> pedido_id)
ALTER TABLE conversa DROP COLUMN IF EXISTS pedido_id;
