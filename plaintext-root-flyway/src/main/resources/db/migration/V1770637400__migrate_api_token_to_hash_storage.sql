-- Migrate API token storage from plain JWT to SHA-256 hash only.
-- Existing tokens are invalidated (they store JWT strings, not hashes).
-- Users will need to create new tokens after this migration.

-- Add token_hash column (SHA-256 hex string = 64 chars)
ALTER TABLE api_token ADD COLUMN token_hash VARCHAR(64);

-- Add invalidated flag for soft-invalidation (separate from deleted)
ALTER TABLE api_token ADD COLUMN invalidated BOOLEAN DEFAULT FALSE;

-- Invalidate and soft-delete all existing tokens (they contain JWT strings, not hashes)
UPDATE api_token SET invalidated = TRUE, deleted = TRUE;

-- Set placeholder hash values for existing rows (required for NOT NULL constraint)
UPDATE api_token SET token_hash = 'legacy-' || CAST(id AS VARCHAR(50)) || '-invalidated';

-- Drop old unique constraint on token column
ALTER TABLE api_token DROP CONSTRAINT uk_api_token_token;

-- Drop old index on token column
DROP INDEX idx_api_token_token;

-- Drop the old token column (was TEXT, stored full JWTs)
ALTER TABLE api_token DROP COLUMN token;

-- Make token_hash NOT NULL
ALTER TABLE api_token ALTER COLUMN token_hash SET NOT NULL;

-- Add unique constraint and index on token_hash
ALTER TABLE api_token ADD CONSTRAINT uk_api_token_hash UNIQUE (token_hash);
CREATE INDEX idx_api_token_hash ON api_token (token_hash);
