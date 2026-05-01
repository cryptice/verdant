-- Backfill every existing organization with four common inexhaustible
-- fertilizers, matching SupplyService.seedInexhaustibleFertilizers. New orgs
-- get the same set via OrganizationService.createOrganization and the dev
-- bootstrap; this migration is for orgs that already exist in dev databases
-- before the feature lands.

INSERT INTO supply_type (org_id, name, category, unit, properties, inexhaustible)
SELECT o.id, t.name, 'FERTILIZER', t.unit, t.properties::jsonb, TRUE
FROM organization o
CROSS JOIN (VALUES
    ('Hästgödsel', 'LITERS', '{"npk":"0.6-0.3-0.5"}'),
    ('Hönsgödsel', 'LITERS', '{"npk":"3.0-2.0-2.0"}'),
    ('Kompost',    'LITERS', '{"npk":"1.0-0.5-1.0"}'),
    ('Träaska',    'LITERS', '{"npk":"0.0-1.0-7.0"}')
) AS t(name, unit, properties);
