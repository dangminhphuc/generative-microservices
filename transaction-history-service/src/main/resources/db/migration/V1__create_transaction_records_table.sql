CREATE TABLE transaction_records (
    id UUID PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    account_number VARCHAR(10) NOT NULL,
    type VARCHAR(10) NOT NULL,
    amount BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,
    counterparty_account_number VARCHAR(10) NOT NULL,
    description VARCHAR(200),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_txrecords_account_date ON transaction_records(account_number, created_at DESC);
CREATE INDEX idx_txrecords_transaction_id ON transaction_records(transaction_id);
