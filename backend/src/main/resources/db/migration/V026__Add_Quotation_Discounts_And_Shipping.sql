ALTER TABLE quotations
  ADD COLUMN IF NOT EXISTS discount_type VARCHAR(32),
  ADD COLUMN IF NOT EXISTS discount_percentage NUMERIC(10,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS shipping_charges NUMERIC(19,2) NOT NULL DEFAULT 0,
  ADD COLUMN IF NOT EXISTS grand_total NUMERIC(19,2) NOT NULL DEFAULT 0;

UPDATE quotations
SET discount_percentage = COALESCE(discount_percentage, 0),
    discount_amount = COALESCE(discount_amount, 0),
    shipping_charges = COALESCE(shipping_charges, 0),
    grand_total = COALESCE(total_amount, 0) + COALESCE(shipping_charges, 0) - COALESCE(discount_amount, 0);
