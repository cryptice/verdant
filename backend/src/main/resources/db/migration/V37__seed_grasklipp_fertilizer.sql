-- Backfill every existing organization with the new "Gräsklipp" (grass
-- clippings) inexhaustible fertilizer. Skipped where the org already has
-- a supply_type with that name (e.g. a user added one manually) to keep
-- the migration idempotent against pre-existing rows.

INSERT INTO supply_type (org_id, name, category, unit, properties, inexhaustible)
SELECT o.id, 'Gräsklipp', 'FERTILIZER', 'LITERS', '{"npk":"0.6-0.2-0.5"}'::jsonb, TRUE
FROM organization o
WHERE NOT EXISTS (
    SELECT 1 FROM supply_type st
    WHERE st.org_id = o.id AND st.name = 'Gräsklipp'
);
