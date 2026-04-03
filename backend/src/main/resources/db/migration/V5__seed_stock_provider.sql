ALTER TABLE seed_inventory ADD COLUMN species_provider_id BIGINT REFERENCES species_provider(id) ON DELETE SET NULL;
