DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE table_schema = 'public'
          AND table_name = 'nursery_profiles'
          AND constraint_name = 'uq_nursery_profiles_email'
    ) THEN
        ALTER TABLE nursery_profiles
        DROP CONSTRAINT uq_nursery_profiles_email;
    END IF;
END $$;
