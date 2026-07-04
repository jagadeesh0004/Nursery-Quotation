-- Flyway migration to allow weight-based dimensions like 10 kg / 10 kgs
-- Keeps existing bag size and pot size formats valid.

ALTER TABLE dimensions DROP CONSTRAINT IF EXISTS chk_dimension_format;

ALTER TABLE dimensions ADD CONSTRAINT chk_dimension_format
  CHECK (
    dimension ~* '(^\d+\s*[xX]\s*\d+$)|(^pot$)|(^\d+"\s*pot$)|(^\d+\s*kgs?$)'
  );
