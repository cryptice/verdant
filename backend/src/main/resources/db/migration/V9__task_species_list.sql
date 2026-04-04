-- Join table: acceptable species for a task
CREATE TABLE scheduled_task_species (
    scheduled_task_id  BIGINT NOT NULL REFERENCES scheduled_task(id) ON DELETE CASCADE,
    species_id         BIGINT NOT NULL REFERENCES species(id) ON DELETE CASCADE,
    PRIMARY KEY (scheduled_task_id, species_id)
);

CREATE INDEX idx_scheduled_task_species_task ON scheduled_task_species(scheduled_task_id);
CREATE INDEX idx_scheduled_task_species_species ON scheduled_task_species(species_id);

-- Make species_id nullable (group tasks don't have a single species)
ALTER TABLE scheduled_task ALTER COLUMN species_id DROP NOT NULL;

-- Add origin group reference for display labeling
ALTER TABLE scheduled_task ADD COLUMN origin_group_id BIGINT REFERENCES species_group(id) ON DELETE SET NULL;

-- Backfill: all existing tasks get their species_id as the sole acceptable species
INSERT INTO scheduled_task_species (scheduled_task_id, species_id)
SELECT id, species_id FROM scheduled_task WHERE species_id IS NOT NULL;
