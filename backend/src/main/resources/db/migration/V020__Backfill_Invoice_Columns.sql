ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(128),
  ADD COLUMN IF NOT EXISTS sequence INTEGER,
  ADD COLUMN IF NOT EXISTS year INTEGER,
  ADD COLUMN IF NOT EXISTS customer_name VARCHAR(255),
  ADD COLUMN IF NOT EXISTS customer_phone VARCHAR(64),
  ADD COLUMN IF NOT EXISTS customer_address TEXT,
  ADD COLUMN IF NOT EXISTS finalized_at TIMESTAMP WITH TIME ZONE,
  ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_invoice_number
  ON invoices(invoice_number);

CREATE INDEX IF NOT EXISTS idx_invoices_userid_year
  ON invoices(user_id, year);
