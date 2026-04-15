-- Migration: Fix quotation_number constraint from GLOBAL to PER-USER
-- Date: 2026-04-15
-- Purpose: Drop the old global unique constraint and ensure composite constraint exists

-- Drop the old GLOBAL unique constraint if it exists
ALTER TABLE public.quotations
DROP CONSTRAINT IF EXISTS uk_9kbnjdxcf5d7qxwy80ple68bh CASCADE;

-- Add the new COMPOSITE unique constraint (user_id, quotation_number)
-- Only if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'quotations'
        AND constraint_name = 'uk_user_quotation_number'
    ) THEN
        ALTER TABLE public.quotations
        ADD CONSTRAINT uk_user_quotation_number
        UNIQUE (user_id, quotation_number);
    END IF;
END $$;

-- Log the migration
COMMENT ON CONSTRAINT uk_user_quotation_number ON public.quotations
IS 'Composite unique constraint: allows same quotation_number for different users';

