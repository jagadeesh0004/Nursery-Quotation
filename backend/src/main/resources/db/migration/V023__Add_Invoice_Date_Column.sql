ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS invoice_date TIMESTAMP WITH TIME ZONE;

UPDATE invoices
SET invoice_date = COALESCE(invoice_date, created_at, finalized_at, now())
WHERE invoice_date IS NULL;

ALTER TABLE invoices
  ALTER COLUMN invoice_date SET DEFAULT now(),
  ALTER COLUMN invoice_date SET NOT NULL;
