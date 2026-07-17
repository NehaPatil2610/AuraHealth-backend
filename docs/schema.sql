-- Database Relational Schema DDL for AuraHealth (PostgreSQL / MySQL compatible)

-- 1. Unified users account table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL, -- Email address
    password VARCHAR(255) NOT NULL,        -- Secure BCrypt hash or sentinel "OAUTH2_NO_PASSWORD"
    name VARCHAR(255) NOT NULL,            -- User display name
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL', -- 'LOCAL' or 'GOOGLE'
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. User Roles mapping table (ManyToMany / ElementCollection representation)
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 3. Isolated doctors profile table
CREATE TABLE IF NOT EXISTS doctors (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    specialization VARCHAR(255),
    license_number VARCHAR(100),
    approval_status BOOLEAN NOT NULL DEFAULT FALSE,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 4. Isolated patients profile table
CREATE TABLE IF NOT EXISTS patients (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    medical_history TEXT,
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'free', -- 'free', 'priority', 'personal', 'wellness'
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
