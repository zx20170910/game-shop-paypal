CREATE TABLE games (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_games_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE products (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    sku VARCHAR(128) NOT NULL,
    name VARCHAR(160) NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    platform VARCHAR(64) NOT NULL,
    server_region VARCHAR(64) NOT NULL,
    delivery_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_products_sku (sku),
    KEY idx_products_game_status (game_id, status),
    CONSTRAINT fk_products_game FOREIGN KEY (game_id) REFERENCES games (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    buyer_id VARCHAR(64) NULL,
    product_id VARCHAR(32) NOT NULL,
    game_id VARCHAR(32) NOT NULL,
    player_uid VARCHAR(160) NOT NULL,
    quantity INT NOT NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    country CHAR(2) NOT NULL,
    access_token_hash VARCHAR(128) NOT NULL,
    order_status VARCHAR(32) NOT NULL,
    payment_status VARCHAR(32) NOT NULL,
    fulfillment_status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_orders_order_no (order_no),
    KEY idx_orders_status (order_status, payment_status, fulfillment_status),
    CONSTRAINT fk_orders_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_orders_game FOREIGN KEY (game_id) REFERENCES games (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE checkout_idempotency_keys (
    idempotency_key VARCHAR(128) NOT NULL PRIMARY KEY,
    request_fingerprint CHAR(64) NOT NULL,
    order_id VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_checkout_idempotency_order (order_id),
    CONSTRAINT fk_checkout_idempotency_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE payment_attempts (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_order_id VARCHAR(64) NULL,
    provider_capture_id VARCHAR(64) NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    create_idempotency_key VARCHAR(128) NOT NULL,
    capture_idempotency_key VARCHAR(128) NULL,
    raw_response JSON NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_payment_provider_order (provider, provider_order_id),
    UNIQUE KEY uk_payment_provider_capture (provider, provider_capture_id),
    UNIQUE KEY uk_payment_create_idempotency (create_idempotency_key),
    KEY idx_payment_order (order_id),
    CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE fulfillments (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    target_player_uid VARCHAR(160) NOT NULL,
    operator_id VARCHAR(64) NULL,
    claimed_at TIMESTAMP(6) NULL,
    proof_object_key VARCHAR(512) NULL,
    delivery_note VARCHAR(2000) NULL,
    reviewed_at TIMESTAMP(6) NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    delivered_at TIMESTAMP(6) NULL,
    last_error VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_fulfillment_order (order_id),
    KEY idx_fulfillment_queue (status, claimed_at),
    CONSTRAINT fk_fulfillment_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE webhook_events (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    raw_body JSON NOT NULL,
    signature_verified BOOLEAN NOT NULL,
    process_status VARCHAR(32) NOT NULL,
    retry_count INT NOT NULL DEFAULT 0,
    received_at TIMESTAMP(6) NOT NULL,
    processed_at TIMESTAMP(6) NULL,
    UNIQUE KEY uk_webhook_provider_event (provider, provider_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE refunds (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    order_id VARCHAR(32) NOT NULL,
    provider_refund_id VARCHAR(64) NULL,
    amount_minor BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(256) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_refund_idempotency (idempotency_key),
    UNIQUE KEY uk_refund_provider_id (provider_refund_id),
    KEY idx_refund_order (order_id),
    CONSTRAINT fk_refund_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
