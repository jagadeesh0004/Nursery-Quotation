-- Create invoices and invoice_items tables

CREATE TABLE invoices (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID NOT NULL,
  status VARCHAR(32) NOT NULL,
  invoice_number VARCHAR(128),
  sequence INTEGER,
  year INTEGER,
  customer_name VARCHAR(255),
  customer_phone VARCHAR(64),
  customer_address TEXT,
  total_amount NUMERIC(19,2) DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
  finalized_at TIMESTAMP WITH TIME ZONE,
  deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE UNIQUE INDEX ux_invoices_invoice_number ON invoices(invoice_number);
CREATE INDEX idx_invoices_userid_year ON invoices(user_id, year);

CREATE TABLE invoice_items (
  id BIGSERIAL PRIMARY KEY,
  invoice_id BIGINT NOT NULL REFERENCES invoices(id) ON DELETE CASCADE,
  plant_name VARCHAR(255),
  dimension VARCHAR(255),
  quantity INTEGER DEFAULT 0,
  price NUMERIC(19,2) DEFAULT 0,
  item_total NUMERIC(19,2) DEFAULT 0
);

CREATE INDEX idx_invoice_items_invoice_id ON invoice_items(invoice_id);
