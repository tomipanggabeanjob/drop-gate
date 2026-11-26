-- The outbox entity uses application-generated string ids, and the hold-expiry worker inserts rows without an id.
ALTER TABLE outbox ALTER COLUMN id DROP DEFAULT;
ALTER TABLE outbox ALTER COLUMN id TYPE VARCHAR(255) USING id::text;
ALTER TABLE outbox ALTER COLUMN id SET DEFAULT gen_random_uuid()::text;
DROP SEQUENCE IF EXISTS outbox_id_seq;
