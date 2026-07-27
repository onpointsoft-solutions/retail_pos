CREATE TABLE IF NOT EXISTS businesses (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_business_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_plans (
    code VARCHAR(30) NOT NULL PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(255) NULL,
    monthly_price DECIMAL(12,2) NOT NULL,
    annual_price DECIMAL(12,2) NOT NULL,
    max_devices INT NOT NULL DEFAULT 1,
    features JSON NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    display_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS licenses (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    business_id VARCHAR(36) NULL,
    license_key_hash CHAR(64) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    plan_code VARCHAR(30) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(190) NULL,
    customer_phone VARCHAR(40) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_devices INT NOT NULL DEFAULT 1,
    issued_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    created_by VARCHAR(36) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_license_hash (license_key_hash),
    INDEX idx_license_business (business_id),
    INDEX idx_license_plan (plan_code),
    INDEX idx_license_status (status),
    INDEX idx_license_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_activations (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    license_id VARCHAR(36) NOT NULL,
    machine_id CHAR(64) NOT NULL,
    device_name VARCHAR(150) NULL,
    store_name VARCHAR(150) NULL,
    app_version VARCHAR(30) NULL,
    activated_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    revoked_at DATETIME NULL,
    UNIQUE KEY uq_license_machine (license_id, machine_id),
    INDEX idx_activation_machine (machine_id),
    CONSTRAINT fk_activation_license FOREIGN KEY (license_id) REFERENCES licenses(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_trials (
    machine_id CHAR(64) NOT NULL PRIMARY KEY,
    started_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    INDEX idx_trial_expiry (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS license_orders (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    reference VARCHAR(80) NOT NULL,
    plan_code VARCHAR(30) NOT NULL,
    billing_period VARCHAR(10) NOT NULL,
    customer_name VARCHAR(150) NOT NULL,
    customer_email VARCHAR(190) NOT NULL,
    customer_phone VARCHAR(40) NULL,
    amount_subunit BIGINT NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'KES',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    paystack_transaction_id VARCHAR(30) NULL,
    paystack_channel VARCHAR(40) NULL,
    license_id VARCHAR(36) NULL,
    business_id VARCHAR(36) NULL,
    activation_file VARCHAR(255) NULL,
    paid_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_license_order_reference (reference),
    INDEX idx_license_order_email (customer_email),
    INDEX idx_license_order_status (status),
    INDEX idx_license_order_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
