-- Flyway migration to update dimension format constraint
-- Version: 010
-- Description: Update check constraint to allow both bag size (number x number) and pot size (number" Pot) formats

-- Drop the old constraint that only allows "number x number" format
ALTER TABLE dimensions DROP CONSTRAINT chk_dimension_format;

-- Add new constraint that allows both formats:
-- 1. Bag size: number x number (e.g., 5x5, 8x10, 13 x 13)
-- 2. Pot size: number" Pot (e.g., 12" Pot, 6" Pot)
ALTER TABLE dimensions ADD CONSTRAINT chk_dimension_format
  CHECK (dimension ~ '(^\d+\s*[xX]\s*\d+$)|(^\d+"\s*Pot$)');

