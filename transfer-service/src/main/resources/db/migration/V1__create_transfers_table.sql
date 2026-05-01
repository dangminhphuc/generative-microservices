CREATE TABLE transfers (
    id UUID PRIMARY KEY,
    source_account_number VARCHAR(10) NOT NULL,
    destination_account_number VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    description VARCHAR(200),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

CREATE INDEX idx_transfers_source_date ON transfers(source_account_number, created_at);
CREATE INDEX idx_transfers_user_id ON transfers(user_id);
CREATE INDEX idx_transfers_status ON transfers(status);
