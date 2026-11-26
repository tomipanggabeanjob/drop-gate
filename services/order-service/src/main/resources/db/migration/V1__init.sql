CREATE TABLE IF NOT EXISTS drops (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_stock INTEGER NOT NULL,
    price_cents BIGINT NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    strategy VARCHAR(64) NOT NULL,
    config JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS accounts (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS devices (
    id BIGSERIAL PRIMARY KEY,
    fingerprint VARCHAR(255) NOT NULL,
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS account_devices (
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    device_id BIGINT NOT NULL REFERENCES devices(id),
    first_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (account_id, device_id)
);

CREATE TABLE IF NOT EXISTS payment_methods (
    id BIGSERIAL PRIMARY KEY,
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    card_hash VARCHAR(255) NOT NULL,
    billing_address_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS holds (
    id BIGSERIAL PRIMARY KEY,
    drop_id BIGINT NOT NULL REFERENCES drops(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    status VARCHAR(32) NOT NULL CHECK (status IN ('ACTIVE', 'CONVERTED', 'EXPIRED', 'RELEASED')),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    drop_id BIGINT NOT NULL REFERENCES drops(id),
    account_id BIGINT NOT NULL REFERENCES accounts(id),
    hold_id BIGINT REFERENCES holds(id),
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    total_cents BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    attempt_meta JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE TABLE IF NOT EXISTS outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    published_at TIMESTAMPTZ NULL
);

CREATE INDEX IF NOT EXISTS idx_holds_active_expires_at
    ON holds (expires_at)
    WHERE status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_orders_drop_created_at
    ON orders (drop_id, created_at)
    INCLUDE (account_id, status, total_cents);

CREATE INDEX IF NOT EXISTS idx_orders_attempt_meta_gin
    ON orders USING GIN (attempt_meta);

CREATE INDEX IF NOT EXISTS idx_drops_config_gin
    ON drops USING GIN (config);

CREATE UNIQUE INDEX IF NOT EXISTS idx_orders_idempotency_key
    ON orders (idempotency_key);

CREATE UNIQUE INDEX IF NOT EXISTS idx_accounts_email_unique
    ON accounts (email);

CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_fingerprint_unique
    ON devices (fingerprint);
