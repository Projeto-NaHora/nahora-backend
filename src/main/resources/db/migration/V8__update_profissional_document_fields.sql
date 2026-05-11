ALTER TABLE profissional ADD COLUMN rg_frente_url VARCHAR(512);
ALTER TABLE profissional ADD COLUMN rg_verso_url VARCHAR(512);
ALTER TABLE profissional ADD COLUMN selfie_url VARCHAR(512);
ALTER TABLE profissional DROP COLUMN IF EXISTS documento_url;
ALTER TABLE profissional DROP COLUMN IF EXISTS valor_inicial;
