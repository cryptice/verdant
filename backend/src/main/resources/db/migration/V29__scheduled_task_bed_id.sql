-- Bed-scoped maintenance tasks (WATER, FERTILIZE, WEED) target a specific
-- bed rather than a species. species_id is already nullable.

ALTER TABLE scheduled_task
    ADD COLUMN bed_id BIGINT REFERENCES bed(id) ON DELETE CASCADE;

CREATE INDEX idx_scheduled_task_bed_id ON scheduled_task(bed_id) WHERE bed_id IS NOT NULL;
