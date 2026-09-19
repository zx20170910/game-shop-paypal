CREATE TABLE IF NOT EXISTS outbox_events (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(128) NOT NULL,
    payload_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP(6) NULL,
    last_error VARCHAR(2000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    KEY idx_outbox_status (status, next_attempt_at, created_at),
    KEY idx_outbox_aggregate (aggregate_type, aggregate_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS fulfillment_attachments (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    fulfillment_id VARCHAR(32) NOT NULL,
    object_key VARCHAR(512) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_by VARCHAR(128) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_fulfillment_attachment_key (fulfillment_id, object_key),
    CONSTRAINT fk_attachment_fulfillment FOREIGN KEY (fulfillment_id) REFERENCES fulfillments (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE orders
    ADD COLUMN risk_status VARCHAR(32) NOT NULL DEFAULT 'CLEAR' AFTER fulfillment_status,
    ADD COLUMN risk_reason VARCHAR(512) NULL AFTER risk_status;
