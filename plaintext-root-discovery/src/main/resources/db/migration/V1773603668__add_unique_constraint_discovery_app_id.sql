-- Prevent duplicate app_id entries (Blue+Green containers register same app)
-- First remove duplicates keeping lowest ID
DELETE FROM discovery_user_session WHERE app_id IN (
    SELECT id FROM discovery_app WHERE id NOT IN (
        SELECT MIN(id) FROM discovery_app GROUP BY app_id
    )
);
DELETE FROM discovery_app WHERE id NOT IN (
    SELECT MIN(id) FROM discovery_app GROUP BY app_id
);

-- Add unique constraint
ALTER TABLE discovery_app ADD CONSTRAINT uk_discovery_app_app_id UNIQUE (app_id);
