-- Consolidate daysToSprout and germinationTimeDays into germinationTimeDays range
ALTER TABLE species RENAME COLUMN germination_time_days TO germination_time_days_min;
ALTER TABLE species ADD COLUMN germination_time_days_max INTEGER;

-- Backfill: where germination_time_days was null but days_to_sprout had a value
UPDATE species SET germination_time_days_min = days_to_sprout WHERE germination_time_days_min IS NULL AND days_to_sprout IS NOT NULL;

-- Drop the redundant column
ALTER TABLE species DROP COLUMN days_to_sprout;

-- Convert daysToHarvest to range
ALTER TABLE species RENAME COLUMN days_to_harvest TO days_to_harvest_min;
ALTER TABLE species ADD COLUMN days_to_harvest_max INTEGER;

-- Convert heightCm to range
ALTER TABLE species RENAME COLUMN height_cm TO height_cm_min;
ALTER TABLE species ADD COLUMN height_cm_max INTEGER;
