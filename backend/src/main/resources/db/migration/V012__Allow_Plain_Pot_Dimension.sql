-- Flyway migration to allow plain "pot" dimensions alongside sized pot dimensions
-- Version: 012
-- Description: Update dimension constraint so existing "pot" rows remain valid

ALTER TABLE dimensions DROP CONSTRAINT IF EXISTS chk_dimension_format;

ALTER TABLE dimensions ADD CONSTRAINT chk_dimension_format
  CHECK (dimension ~* '(^\d+\s*[xX]\s*\d+$)|(^pot$)|(^\d+"\s*pot$)');
