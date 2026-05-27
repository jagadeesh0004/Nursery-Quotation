-- Performance Tuning: Add Indexes for search optimization
CREATE INDEX IF NOT EXISTS idx_quotations_customer_name_search ON quotations (customer_name) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_plants_name_search ON plants (name) WHERE deleted_at IS NULL;

