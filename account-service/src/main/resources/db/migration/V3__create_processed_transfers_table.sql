CREATE TABLE processed_transfers (
    transfer_id VARCHAR(36) PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
