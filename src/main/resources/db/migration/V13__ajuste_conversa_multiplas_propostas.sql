-- Remove a restrição que impedia um pedido de ter vários chats
ALTER TABLE conversa DROP CONSTRAINT IF EXISTS conversa_pedido_id_key;

-- Cria índice para busca rápida de conversas atreladas a um pedido
CREATE INDEX idx_conversa_pedido_id ON conversa(pedido_id);