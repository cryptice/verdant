-- Add UNOBSTRUCTED aspect (sun from all directions).
-- Widen aspect column to fit the new longer value and extend the CHECK constraint.

ALTER TABLE bed ALTER COLUMN aspect TYPE VARCHAR(16);

ALTER TABLE bed DROP CONSTRAINT bed_aspect_chk;

ALTER TABLE bed ADD CONSTRAINT bed_aspect_chk
    CHECK (aspect IS NULL OR aspect IN ('FLAT','N','NE','E','SE','S','SW','W','NW','UNOBSTRUCTED'));
