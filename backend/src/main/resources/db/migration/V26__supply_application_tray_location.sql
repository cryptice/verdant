-- Allow supply applications to be scoped to a tray location instead of a bed.

ALTER TABLE supply_application ALTER COLUMN bed_id DROP NOT NULL;

ALTER TABLE supply_application
    ADD COLUMN tray_location_id BIGINT REFERENCES tray_location(id) ON DELETE SET NULL;

ALTER TABLE supply_application
    ADD CONSTRAINT supply_application_bed_or_tray_exclusive
    CHECK (
        (bed_id IS NOT NULL AND tray_location_id IS NULL)
        OR (bed_id IS NULL AND tray_location_id IS NOT NULL)
    );

CREATE INDEX idx_supply_application_tray_location
    ON supply_application(tray_location_id, applied_at DESC);
