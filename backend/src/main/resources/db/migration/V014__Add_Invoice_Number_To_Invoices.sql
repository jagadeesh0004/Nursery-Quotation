ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS invoice_number VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS ux_invoices_invoice_number
  ON invoices(invoice_number);
