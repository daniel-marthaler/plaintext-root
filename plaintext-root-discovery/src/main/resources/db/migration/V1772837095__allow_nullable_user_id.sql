-- Allow user_id to be NULL for remote user sessions where we don't know the local ID
ALTER TABLE discovery_user_session ALTER COLUMN user_id DROP NOT NULL;
