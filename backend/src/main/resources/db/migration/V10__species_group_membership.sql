-- Many-to-many join table for species <-> groups
CREATE TABLE species_group_membership (
    species_id  BIGINT NOT NULL REFERENCES species(id) ON DELETE CASCADE,
    group_id    BIGINT NOT NULL REFERENCES species_group(id) ON DELETE CASCADE,
    PRIMARY KEY (species_id, group_id)
);

CREATE INDEX idx_species_group_membership_species ON species_group_membership(species_id);
CREATE INDEX idx_species_group_membership_group ON species_group_membership(group_id);

-- Backfill from existing group_id column
INSERT INTO species_group_membership (species_id, group_id)
SELECT id, group_id FROM species WHERE group_id IS NOT NULL;

-- Drop the old column
ALTER TABLE species DROP COLUMN group_id;

-- Remove system-level groups (org_id IS NULL) — groups are now org-only
-- CASCADE on species_group_membership FK handles removing memberships
DELETE FROM species_group WHERE org_id IS NULL;
