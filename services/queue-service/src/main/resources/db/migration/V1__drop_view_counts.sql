CREATE TABLE IF NOT EXISTS drop_view_counts (
    drop_id VARCHAR(255) PRIMARY KEY,
    view_count BIGINT NOT NULL DEFAULT 0
);
