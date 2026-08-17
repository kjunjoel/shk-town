CREATE TABLE IF NOT EXISTS users (
    uuid        BINARY(16) PRIMARY KEY,
    name        VARCHAR(16) NOT NULL,
    cash        DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
    last_login  DATETIME,
    first_login DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_user_name (name),
    CONSTRAINT chk_users_cash_non_negative CHECK (cash >= 0)
);

CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    owner_uuid     BINARY(16) NOT NULL,
    account_type   TINYINT NOT NULL,
    balance        DECIMAL(20, 2) NOT NULL DEFAULT 0.00,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    UNIQUE KEY uk_accounts_owner_type (owner_uuid, account_type),
    CONSTRAINT chk_accounts_balance_non_negative CHECK (balance >= 0)
);

CREATE TABLE IF NOT EXISTS transactions (
    uuid                BINARY(16) PRIMARY KEY,
    from_account_number VARCHAR(20),
    to_account_number   VARCHAR(20),
    amount              DECIMAL(20, 2) NOT NULL,
    reason              VARCHAR(20) NOT NULL,
    detail              TEXT,
    status              VARCHAR(20) NOT NULL,
    created_at          DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at        DATETIME(3),

    INDEX idx_transaction_from_time (from_account_number, created_at DESC),
    INDEX idx_transaction_to_time (to_account_number, created_at DESC),
    INDEX idx_transaction_status_time (status, created_at DESC),
    CONSTRAINT chk_transaction_amount_positive CHECK (amount > 0),
    CONSTRAINT chk_transaction_has_account CHECK (
        from_account_number IS NOT NULL OR to_account_number IS NOT NULL
    ),
    CONSTRAINT chk_transaction_accounts_differ CHECK (
        from_account_number IS NULL
        OR to_account_number IS NULL
        OR from_account_number <> to_account_number
    )
);

CREATE TABLE IF NOT EXISTS economy_logs (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_id BINARY(16),
    asset_type     TINYINT NOT NULL,
    target_id      BINARY(16) NOT NULL,
    account_number VARCHAR(20),
    sender_display VARCHAR(32),
    amount         DECIMAL(20, 2) NOT NULL,
    balance_after  DECIMAL(20, 2) NOT NULL,
    reason         VARCHAR(20) NOT NULL,
    detail         TEXT,
    created_at     DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_target_type_time (target_id, asset_type, created_at DESC),
    INDEX idx_economy_log_transaction (transaction_id),
    CONSTRAINT fk_economy_log_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions(uuid),
    CONSTRAINT chk_economy_log_amount_non_zero CHECK (amount <> 0),
    CONSTRAINT chk_economy_log_balance_non_negative CHECK (balance_after >= 0)
);

CREATE TABLE IF NOT EXISTS cash_logs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uuid     BINARY(16) NOT NULL,
    amount        DECIMAL(20, 2) NOT NULL,
    balance_after DECIMAL(20, 2) NOT NULL,
    reason        VARCHAR(20) NOT NULL,
    detail        TEXT,
    created_at    DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),

    INDEX idx_user_time (user_uuid, created_at DESC),
    CONSTRAINT chk_cash_log_amount_non_zero CHECK (amount <> 0),
    CONSTRAINT chk_cash_log_balance_non_negative CHECK (balance_after >= 0)
);

CREATE TABLE IF NOT EXISTS system_logs (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    log_type   VARCHAR(20) NOT NULL,
    message    TEXT,
    data_json  JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_type_time (log_type, created_at DESC)
);
