-- Tasks planned from a seed batch remember it, so the sowing task opens the sow screen with that batch picked.
ALTER TABLE scheduled_task ADD COLUMN seed_inventory_id BIGINT REFERENCES seed_inventory(id) ON DELETE SET NULL;
CREATE INDEX idx_scheduled_task_seed_inventory ON scheduled_task(seed_inventory_id);
