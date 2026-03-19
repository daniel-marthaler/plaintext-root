-- Add token name for multiple named tokens per user
ALTER TABLE api_token ADD COLUMN token_name VARCHAR(100);

-- Add user email (included in JWT claims)
ALTER TABLE api_token ADD COLUMN user_email VARCHAR(255);

-- Create index for faster lookup by user and name
CREATE INDEX idx_api_token_user_name ON api_token(user_id, mandat, token_name);
