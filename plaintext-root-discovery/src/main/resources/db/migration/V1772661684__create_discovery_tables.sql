-- Discovery Multi-Instance Navigation Tables
-- Creates tables for PKI-encrypted MQTT-based cross-app navigation

-- Table for discovered remote application instances
CREATE TABLE IF NOT EXISTS discovery_app (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(100) NOT NULL,
    app_name VARCHAR(200) NOT NULL,
    app_url VARCHAR(500) NOT NULL,
    environment VARCHAR(20) NOT NULL,
    public_key TEXT,
    last_seen_at TIMESTAMP NOT NULL,
    version VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- SuperModel fields (audit + multi-tenant)
    deleted BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255), 
    last_modified_date TIMESTAMP,
    mandat VARCHAR(255),
    tags VARCHAR(5000)
);

-- Table for user sessions across different app instances
CREATE TABLE IF NOT EXISTS discovery_user_session (
    id BIGSERIAL PRIMARY KEY,
    app_id BIGINT NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    user_name VARCHAR(200),
    logged_in_at TIMESTAMP NOT NULL,
    last_activity_at TIMESTAMP NOT NULL,
    session_active BOOLEAN NOT NULL DEFAULT TRUE,
    login_token VARCHAR(500),
    token_expires_at TIMESTAMP,
    token_used BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- SuperModel fields (audit + multi-tenant) 
    deleted BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(255),
    created_date TIMESTAMP,
    last_modified_by VARCHAR(255),
    last_modified_date TIMESTAMP,
    mandat VARCHAR(255),
    tags VARCHAR(5000),
    
    -- Foreign key to discovery_app
    CONSTRAINT fk_discovery_user_session_app 
        FOREIGN KEY (app_id) REFERENCES discovery_app(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_discovery_app_app_id ON discovery_app(app_id);
CREATE INDEX IF NOT EXISTS idx_discovery_app_active ON discovery_app(active);
CREATE INDEX IF NOT EXISTS idx_discovery_app_environment ON discovery_app(environment);
CREATE INDEX IF NOT EXISTS idx_discovery_app_last_seen ON discovery_app(last_seen_at);
CREATE INDEX IF NOT EXISTS idx_discovery_app_mandat ON discovery_app(mandat);

CREATE INDEX IF NOT EXISTS idx_discovery_user_session_app_id ON discovery_user_session(app_id);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_user_email ON discovery_user_session(user_email);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_active ON discovery_user_session(session_active);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_token ON discovery_user_session(login_token);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_mandat ON discovery_user_session(mandat);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_last_activity ON discovery_user_session(last_activity_at);

-- Composite index for common queries
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_email_active 
    ON discovery_user_session(user_email, session_active);
CREATE INDEX IF NOT EXISTS idx_discovery_user_session_app_email_active 
    ON discovery_user_session(app_id, user_email, session_active);