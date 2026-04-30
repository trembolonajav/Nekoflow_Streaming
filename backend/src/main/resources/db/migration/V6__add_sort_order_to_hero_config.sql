ALTER TABLE hero_config
ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 1;

UPDATE hero_config
SET sort_order = 1
WHERE sort_order IS NULL;
