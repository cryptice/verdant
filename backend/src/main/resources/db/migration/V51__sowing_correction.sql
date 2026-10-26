-- Each sown plant points at the ledger draw that paid for it, so removing a
-- mistaken sowing can give its seed back. A correction points at the draw it reverses.
ALTER TABLE plant ADD COLUMN sown_movement_id BIGINT REFERENCES seed_stock_movement(id) ON DELETE SET NULL;
ALTER TABLE seed_stock_movement ADD COLUMN corrects_movement_id BIGINT REFERENCES seed_stock_movement(id) ON DELETE CASCADE;

CREATE INDEX idx_plant_sown_movement ON plant (sown_movement_id);
CREATE INDEX idx_seed_stock_movement_corrects ON seed_stock_movement (corrects_movement_id);

-- A batch sowing creates its plants with consecutive ids starting at first_plant_id.
UPDATE plant p SET sown_movement_id = m.id
FROM seed_stock_movement m
WHERE m.kind = 'SOWN' AND m.first_plant_id IS NOT NULL AND m.plant_count IS NOT NULL
  AND p.org_id = m.org_id AND p.species_id = m.species_id
  AND p.id >= m.first_plant_id AND p.id < m.first_plant_id + m.plant_count;
