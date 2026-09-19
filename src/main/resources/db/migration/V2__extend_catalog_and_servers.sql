ALTER TABLE games
    ADD COLUMN publisher VARCHAR(128) NULL AFTER name,
    ADD COLUMN supported_platforms VARCHAR(512) NULL AFTER publisher,
    ADD COLUMN delivery_type VARCHAR(32) NOT NULL DEFAULT 'MANUAL' AFTER supported_platforms;

CREATE TABLE servers (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    game_id VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    region VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_servers_game_code (game_id, code),
    KEY idx_servers_game_status (game_id, status),
    CONSTRAINT fk_servers_game FOREIGN KEY (game_id) REFERENCES games (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE webhook_events ADD COLUMN headers_json JSON NULL AFTER raw_body;
