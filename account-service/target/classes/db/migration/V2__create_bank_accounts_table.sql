CREATE TABLE bank_accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(10) NOT NULL,
    user_id UUID NOT NULL REFERENCES users(id),
    balance BIGINT NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_accounts_number UNIQUE (account_number)
);

CREATE INDEX idx_bank_accounts_user_id ON bank_accounts(user_id);
