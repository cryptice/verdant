ALTER TABLE species RENAME COLUMN cost_per_seed_cents TO cost_per_seed_sek;
ALTER TABLE species_provider RENAME COLUMN cost_per_unit_cents TO cost_per_unit_sek;
ALTER TABLE seed_inventory RENAME COLUMN cost_per_unit_cents TO cost_per_unit_sek;
ALTER TABLE bouquet_recipe RENAME COLUMN price_cents TO price_sek;
ALTER TABLE listing RENAME COLUMN price_per_stem_cents TO price_per_stem_sek;
ALTER TABLE market_order RENAME COLUMN total_cents TO total_sek;
ALTER TABLE order_item RENAME COLUMN price_per_stem_cents TO price_per_stem_sek;
