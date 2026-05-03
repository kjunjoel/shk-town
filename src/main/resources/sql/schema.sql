CREATE TABLE IF NOT EXISTS users (
    uuid        CHAR(36) PRIMARY KEY,
    name        VARCHAR(16) NOT NULL,
    cash        DECIMAL(20, 2) DEFAULT 0.0,
    town_id     BIGINT DEFAULT -1,
    nation_id   BIGINT DEFAULT -1,
    last_logion DATETIME,
    first_login DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_name (name),
    INDEX idx_user_town (town_id),
    INDEX idx_user_nation (nation_id)
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    owner_uuid     CHAR(36) NOT NULL,
    account_type   TINYINT NOT NULL,
    balance        DECIMAL(20, 2) DEFAULT 0.0,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_owner_type (owner_uuid, account_type)
);

CREATE TABLE IF NOT EXISTS towns (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid       CHAR(36) NOT NULL UNIQUE,
    name       VARCHAR(32) NOT NULL,
    mayor_uuid CHAR(36) NOT NULL,
    nation_id  BIGINT DEFAULT -1,

    INDEX idx_town_uuid (town_uuid),
    INDEX idx_town_mayor (mayor_uuid)
);

CREATE TABLE IF NOT EXISTS nations (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid        CHAR(36) NOT NULL UNIQUE,
    name        VARCHAR(32) NOT NULL,
    king_uuid   CHAR(36) NOT NULL,

    INDEX idx_nation_uuid (nation_uuid)
);

CREATE TABLE IF NOT EXISTS economy_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_type     TINYINT NOT NULL,
    target_id      VARCHAR(36) NOT NULL,
    account_number VARCHAR(20),
    amount         DECIMAL(20, 2) NOT NULL,
    balance_after  DECIMAL(20, 2) NOT NULL,
    reason         VARCHAR(20) NOT NULL,
    detail         TEXT,
    created_at     DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_target_type_time (target_id, asset_type, created_at DESC)
);

CREATE TABLE IF NOT EXISTS system_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_type VARCHAR(20) NOT NULL,
    message TEXT,
    data_json JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_type_time (log_type, created_at DESC)
);