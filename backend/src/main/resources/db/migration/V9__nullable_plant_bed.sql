-- Allow plants without a bed (e.g. sown in portable trays)
ALTER TABLE plant ALTER COLUMN bed_id DROP NOT NULL;
ALTER TABLE plant ADD COLUMN user_id BIGINT REFERENCES app_user(id);

-- Backfill user_id from existing bed->garden->owner chain
UPDATE plant SET user_id = g.owner_id
FROM bed b JOIN garden g ON b.garden_id = g.id
WHERE plant.bed_id = b.id AND plant.user_id IS NULL;

ALTER TABLE plant ALTER COLUMN user_id SET NOT NULL;
CREATE INDEX idx_plant_user ON plant(user_id);
