DO $$
BEGIN
	-- Current schema uses 'dimensions'.
	IF to_regclass('public.dimensions') IS NOT NULL THEN
		IF NOT EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = 'public'
			  AND table_name = 'dimensions'
			  AND column_name = 'price'
		) THEN
			ALTER TABLE public.dimensions
				ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
		END IF;
	-- Some earlier local schemas used 'dimension'.
	ELSIF to_regclass('public.dimension') IS NOT NULL THEN
		IF NOT EXISTS (
			SELECT 1
			FROM information_schema.columns
			WHERE table_schema = 'public'
			  AND table_name = 'dimension'
			  AND column_name = 'price'
		) THEN
			ALTER TABLE public.dimension
				ADD COLUMN price NUMERIC(10, 2) NOT NULL DEFAULT 0.00;
		END IF;
	END IF;
END $$;

