CREATE TABLE IF NOT EXISTS admin_users (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    username VARCHAR(128) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_admin_users_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_user_roles (
    admin_id VARCHAR(32) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    PRIMARY KEY (admin_id, role_code),
    CONSTRAINT fk_admin_role_user FOREIGN KEY (admin_id) REFERENCES admin_users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    operator_id VARCHAR(32) NOT NULL,
    operator_username VARCHAR(128) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(128) NOT NULL,
    detail_json JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,
    KEY idx_audit_logs_resource (resource_type, resource_id),
    KEY idx_audit_logs_operator (operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
