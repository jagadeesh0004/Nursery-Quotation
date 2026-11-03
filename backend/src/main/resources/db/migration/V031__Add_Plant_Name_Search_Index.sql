CREATE INDEX IF NOT EXISTS idx_plants_name_search
    ON plants (name)
    WHERE deleted_at IS NULL;
