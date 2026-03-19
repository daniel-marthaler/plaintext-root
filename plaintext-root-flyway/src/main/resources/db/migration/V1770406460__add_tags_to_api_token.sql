-- Add missing tags column to api_token table
ALTER TABLE api_token ADD COLUMN tags VARCHAR(5000);
