-- Ad-hoc sales: a sale recorded directly against a species, without a tracked
-- plant batch, harvest event, or bouquet behind it. The backend auto-creates a
-- SOLD_OUT sale_lot at record time so the existing sale/event infrastructure
-- stays unchanged.

ALTER TABLE sale_lot
    ADD COLUMN species_id BIGINT REFERENCES species(id) ON DELETE RESTRICT;

-- Drop the auto-named CHECK constraints we need to replace.
-- Postgres 17 auto-names single-column CHECKs as <table>_<column>_check and
-- multi-column CHECKs as <table>_check, <table>_check1, ... in declaration
-- order. In V33 the multi-column checks are: quantity_remaining<=quantity_total
-- (-> sale_lot_check), polymorphic XOR (-> sale_lot_check1), BUNCH stems
-- (-> sale_lot_check2). The source_kind IN check is single-column, so it is
-- named sale_lot_source_kind_check.
ALTER TABLE sale_lot DROP CONSTRAINT IF EXISTS sale_lot_source_kind_check;
ALTER TABLE sale_lot DROP CONSTRAINT IF EXISTS sale_lot_check1;

-- Re-add: extended source-kind list including ADHOC.
ALTER TABLE sale_lot
    ADD CONSTRAINT sale_lot_source_kind_check
    CHECK (source_kind IN ('PLANT', 'HARVEST_EVENT', 'BOUQUET', 'ADHOC'));

-- Re-add: polymorphic XOR extended for ADHOC.
ALTER TABLE sale_lot
    ADD CONSTRAINT sale_lot_source_polymorphic_check
    CHECK (
        (source_kind = 'PLANT'         AND plant_id IS NOT NULL     AND harvest_event_id IS NULL     AND bouquet_id IS NULL     AND species_id IS NULL) OR
        (source_kind = 'HARVEST_EVENT' AND plant_id IS NULL         AND harvest_event_id IS NOT NULL AND bouquet_id IS NULL     AND species_id IS NULL) OR
        (source_kind = 'BOUQUET'       AND plant_id IS NULL         AND harvest_event_id IS NULL     AND bouquet_id IS NOT NULL AND species_id IS NULL) OR
        (source_kind = 'ADHOC'         AND plant_id IS NULL         AND harvest_event_id IS NULL     AND bouquet_id IS NULL     AND species_id IS NOT NULL)
    );

CREATE INDEX idx_sale_lot_species ON sale_lot(species_id) WHERE species_id IS NOT NULL;
