-- Rename all monetary columns from *_sek to *_cents.
-- Storage has always been integer öre (cents); the _sek suffix was misleading.
-- Convention going forward: API + DB store integer cents; UI converts at the boundary.
ALTER TABLE species RENAME COLUMN cost_per_seed_sek TO cost_per_seed_cents;
ALTER TABLE species_provider RENAME COLUMN cost_per_unit_sek TO cost_per_unit_cents;
ALTER TABLE seed_inventory RENAME COLUMN cost_per_unit_sek TO cost_per_unit_cents;
ALTER TABLE bouquet_recipe RENAME COLUMN price_sek TO price_cents;
ALTER TABLE bouquet RENAME COLUMN price_sek TO price_cents;
ALTER TABLE supply_inventory RENAME COLUMN cost_sek TO cost_cents;
