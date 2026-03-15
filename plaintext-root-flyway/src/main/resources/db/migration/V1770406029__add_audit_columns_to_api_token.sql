-- Add missing audit columns to api_token table
ALTER TABLE api_token ADD COLUMN created_by VARCHAR(255);
ALTER TABLE api_token ADD COLUMN created_date TIMESTAMP;
ALTER TABLE api_token ADD COLUMN last_modified_by VARCHAR(255);
ALTER TABLE api_token ADD COLUMN last_modified_date TIMESTAMP;
