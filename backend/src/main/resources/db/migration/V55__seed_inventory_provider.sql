-- A batch names its supplier directly, so any supplier can be chosen, also for a catalogue variety whose
-- supplier links are shared and can't take an organization's own. Existing batches take theirs from the link.
ALTER TABLE seed_inventory ADD COLUMN provider_id BIGINT REFERENCES provider(id) ON DELETE SET NULL;
UPDATE seed_inventory si SET provider_id = sp.provider_id FROM species_provider sp WHERE si.species_provider_id = sp.id;
