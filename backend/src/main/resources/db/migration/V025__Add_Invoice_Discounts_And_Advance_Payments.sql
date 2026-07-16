ALTER TABLE invoices
  ADD COLUMN IF NOT EXISTS discount_type VARCHAR(32),
  ADD COLUMN IF NOT EXISTS discount_percentage NUMERIC(10,2),
  ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS shipping_charges NUMERIC(19,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS grand_total NUMERIC(19,2) NOT NULL DEFAULT 0;

UPDATE invoices
SET discount_amount = COALESCE(discount_amount, 0),
    shipping_charges = COALESCE(shipping_charges, 0),
    grand_total = COALESCE(total_amount, 0) + COALESCE(shipping_charges, 0) - COALESCE(discount_amount, 0);

CREATE TABLE IF NOT EXISTS advance_payment (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  payment_date DATE NOT NULL,
  amount NUMERIC(19,2) NOT NULL,
  payment_mode VARCHAR(64) NOT NULL,
  remarks TEXT,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_advance_payment_invoice_id ON advance_payment(invoice_id);
