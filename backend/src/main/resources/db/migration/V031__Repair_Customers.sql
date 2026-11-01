CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone VARCHAR(64),
    address TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_customers_user_id ON customers(user_id);

ALTER TABLE quotations ADD COLUMN IF NOT EXISTS customer_id BIGINT;
ALTER TABLE invoices ADD COLUMN IF NOT EXISTS customer_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_quotations_customer_id ON quotations(customer_id);
CREATE INDEX IF NOT EXISTS idx_invoices_customer_id ON invoices(customer_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_customers_user'
    ) THEN
        ALTER TABLE customers
            ADD CONSTRAINT fk_customers_user
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

UPDATE customers c
SET updated_at = NOW()
WHERE c.updated_at IS NULL;

INSERT INTO customers (user_id, name, phone, address)
SELECT DISTINCT q.user_id, trim(q.customer_name), NULLIF(trim(q.customer_phone), ''), q.customer_address
FROM quotations q
WHERE q.customer_name IS NOT NULL AND trim(q.customer_name) <> ''
  AND q.customer_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM customers c
    WHERE c.user_id = q.user_id
      AND lower(c.name) = lower(trim(q.customer_name))
      AND c.phone IS NOT DISTINCT FROM NULLIF(trim(q.customer_phone), '')
      AND c.address IS NOT DISTINCT FROM q.customer_address
  );

INSERT INTO customers (user_id, name, phone, address)
SELECT DISTINCT i.user_id, trim(i.customer_name), NULLIF(trim(i.customer_phone), ''), i.customer_address
FROM invoices i
WHERE i.customer_name IS NOT NULL AND trim(i.customer_name) <> ''
  AND i.customer_id IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM customers c
    WHERE c.user_id = i.user_id
      AND lower(c.name) = lower(trim(i.customer_name))
      AND c.phone IS NOT DISTINCT FROM NULLIF(trim(i.customer_phone), '')
      AND c.address IS NOT DISTINCT FROM i.customer_address
  );

UPDATE quotations q
SET customer_id = c.id
FROM customers c
WHERE q.customer_id IS NULL
  AND c.user_id = q.user_id
  AND lower(c.name) = lower(trim(q.customer_name))
  AND c.phone IS NOT DISTINCT FROM NULLIF(trim(q.customer_phone), '')
  AND c.address IS NOT DISTINCT FROM q.customer_address;

UPDATE invoices i
SET customer_id = c.id
FROM customers c
WHERE i.customer_id IS NULL
  AND c.user_id = i.user_id
  AND lower(c.name) = lower(trim(i.customer_name))
  AND c.phone IS NOT DISTINCT FROM NULLIF(trim(i.customer_phone), '')
  AND c.address IS NOT DISTINCT FROM i.customer_address;
