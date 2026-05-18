-- Ad-hoc sales with free-text label: extends the existing ADHOC source kind
-- so it can identify what was sold via a label (e.g. "Brudbukett") instead
-- of a tracked species. Used for bouquet quick-sales where the user doesn't
-- want to materialise a Bouquet entity for a one-off transaction.

ALTER TABLE sale_lot
    ADD COLUMN adhoc_label VARCHAR(200);

-- Replace the polymorphic-XOR check so ADHOC requires exactly one of
-- species_id / adhoc_label.
ALTER TABLE sale_lot DROP CONSTRAINT IF EXISTS sale_lot_source_polymorphic_check;

ALTER TABLE sale_lot
    ADD CONSTRAINT sale_lot_source_polymorphic_check
    CHECK (
        (source_kind = 'PLANT'         AND plant_id IS NOT NULL     AND harvest_event_id IS NULL     AND bouquet_id IS NULL     AND species_id IS NULL     AND adhoc_label IS NULL) OR
        (source_kind = 'HARVEST_EVENT' AND plant_id IS NULL         AND harvest_event_id IS NOT NULL AND bouquet_id IS NULL     AND species_id IS NULL     AND adhoc_label IS NULL) OR
        (source_kind = 'BOUQUET'       AND plant_id IS NULL         AND harvest_event_id IS NULL     AND bouquet_id IS NOT NULL AND species_id IS NULL     AND adhoc_label IS NULL) OR
        (source_kind = 'ADHOC'         AND plant_id IS NULL         AND harvest_event_id IS NULL     AND bouquet_id IS NULL     AND ((species_id IS NOT NULL AND adhoc_label IS NULL) OR (species_id IS NULL AND adhoc_label IS NOT NULL)))
    );
