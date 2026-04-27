-- Inexhaustible supply types: a flag on supply_type so an "always available"
-- material (e.g. own horse manure) can be selected when fertilizing without
-- needing a tracked inventory lot. Applications referencing an inexhaustible
-- type record supply_type_id only and leave supply_inventory_id NULL.

ALTER TABLE supply_type
    ADD COLUMN inexhaustible BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE supply_application
    ALTER COLUMN supply_inventory_id DROP NOT NULL;
