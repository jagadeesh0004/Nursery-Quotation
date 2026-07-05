ALTER TABLE invoices
  ALTER COLUMN invoice_date TYPE TIMESTAMP WITH TIME ZONE
  USING invoice_date::timestamp with time zone;

ALTER TABLE invoices
  ALTER COLUMN invoice_date SET DEFAULT now();
