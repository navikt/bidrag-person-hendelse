
ALTER TABLE hendelsemottak ALTER COLUMN hendelse TYPE jsonb using hendelse::jsonb;

