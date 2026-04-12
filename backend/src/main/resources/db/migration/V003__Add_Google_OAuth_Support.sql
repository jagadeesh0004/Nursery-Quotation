-- Flyway migration: V003__Add_Google_OAuth_Support.sql
-- Add Google OAuth columns to users table

DO $$
BEGIN
	IF to_regclass('public.users') IS NOT NULL THEN
		ALTER TABLE public.users
			ADD COLUMN IF NOT EXISTS email VARCHAR(100),
			ADD COLUMN IF NOT EXISTS google_id VARCHAR(255),
			ADD COLUMN IF NOT EXISTS auth_method VARCHAR(20);

		UPDATE public.users
		SET auth_method = 'OTP'
		WHERE auth_method IS NULL;

		IF NOT EXISTS (
			SELECT 1
			FROM information_schema.table_constraints
			WHERE table_schema = 'public'
			  AND table_name = 'users'
			  AND constraint_name = 'chk_auth_method'
		) THEN
			ALTER TABLE public.users
				ADD CONSTRAINT chk_auth_method CHECK (auth_method IN ('OTP', 'GOOGLE'));
		END IF;

		ALTER TABLE public.users
			ALTER COLUMN auth_method SET NOT NULL;

		ALTER TABLE public.users
			ALTER COLUMN phone_number DROP NOT NULL;

		IF NOT EXISTS (
			SELECT 1
			FROM information_schema.table_constraints
			WHERE table_schema = 'public'
			  AND table_name = 'users'
			  AND constraint_name = 'uk_users_google_id'
		) THEN
			ALTER TABLE public.users
				ADD CONSTRAINT uk_users_google_id UNIQUE (google_id);
		END IF;

		IF NOT EXISTS (
			SELECT 1
			FROM information_schema.table_constraints
			WHERE table_schema = 'public'
			  AND table_name = 'users'
			  AND constraint_name = 'uk_users_email'
		) THEN
			ALTER TABLE public.users
				ADD CONSTRAINT uk_users_email UNIQUE (email);
		END IF;
	END IF;

	IF to_regclass('public.nursery_profiles') IS NOT NULL THEN
		ALTER TABLE public.nursery_profiles
			ALTER COLUMN phone DROP NOT NULL;
	END IF;
END $$;

