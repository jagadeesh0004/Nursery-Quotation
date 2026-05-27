-- Flyway migration to fix dimension format constraint case sensitivity
-- Version: 011
-- Description: Update check constraint to be case-insensitive for pot size

-- Drop the old constraint
ALTER TABLE dimensions DROP CONSTRAINT chk_dimension_format;

-- Add new constraint that allows both formats (case-insensitive):
-- 1. Bag size: number x number (e.g., 5x5, 8X10, 13 x 13)
-- 2. Pot size: number" Pot (case-insensitive, e.g., 12" pot, 12" Pot, 12" POT)
-- Using ~* operator for case-insensitive regex matching in PostgreSQL
ALTER TABLE dimensions ADD CONSTRAINT chk_dimension_format
  CHECK (dimension ~* '(^\d+\s*[xX]\s*\d+$)|(^\d+"\s*pot$)');


