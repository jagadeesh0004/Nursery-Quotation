-- Add quotation-level height toggle and optional item height

ALTER TABLE quotations
    ADD COLUMN IF NOT EXISTS height_enabled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE quotation_items
    ADD COLUMN IF NOT EXISTS height VARCHAR(20);