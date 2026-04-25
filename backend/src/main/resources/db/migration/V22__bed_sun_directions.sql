-- Replace single-value `aspect` with a multi-value `sun_directions` set.
-- A bed can be obstructed on multiple sides, so the list captures every
-- direction it actually receives sun from. FLAT / UNOBSTRUCTED are dropped
-- (the latter is just "all 8 directions checked").

ALTER TABLE bed DROP CONSTRAINT IF EXISTS bed_aspect_chk;
ALTER TABLE bed DROP COLUMN IF EXISTS aspect;

ALTER TABLE bed ADD COLUMN sun_directions TEXT[];

ALTER TABLE bed ADD CONSTRAINT bed_sun_directions_chk
    CHECK (
      sun_directions IS NULL
      OR sun_directions <@ ARRAY['N','NE','E','SE','S','SW','W','NW']::TEXT[]
    );
