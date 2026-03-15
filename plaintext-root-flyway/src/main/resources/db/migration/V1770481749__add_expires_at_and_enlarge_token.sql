-- Add expiration timestamp for JWT tokens (90 days validity)
ALTER TABLE api_token ADD COLUMN expires_at TIMESTAMP;

-- Enlarge token column for JWT (RS256 signed JWTs are ~500-800 chars)
-- HSQLDB: Change from VARCHAR(64) to CLOB for long JWTs
ALTER TABLE api_token ALTER COLUMN token TYPE TEXT;
