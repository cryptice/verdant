-- Optional horticultural profile for each bed. All fields nullable.
-- Enum columns use VARCHAR + CHECK per project convention.

ALTER TABLE bed ADD COLUMN soil_type       VARCHAR(16);
ALTER TABLE bed ADD COLUMN soil_ph         DOUBLE PRECISION;
ALTER TABLE bed ADD COLUMN sun_exposure    VARCHAR(16);
ALTER TABLE bed ADD COLUMN drainage        VARCHAR(16);
ALTER TABLE bed ADD COLUMN aspect          VARCHAR(4);
ALTER TABLE bed ADD COLUMN irrigation_type VARCHAR(16);
ALTER TABLE bed ADD COLUMN protection      VARCHAR(16);
ALTER TABLE bed ADD COLUMN raised_bed      BOOLEAN;

ALTER TABLE bed ADD CONSTRAINT bed_soil_type_chk
    CHECK (soil_type IS NULL OR soil_type IN ('SANDY','LOAMY','CLAY','SILTY','PEATY','CHALKY'));

ALTER TABLE bed ADD CONSTRAINT bed_soil_ph_chk
    CHECK (soil_ph IS NULL OR (soil_ph >= 3.0 AND soil_ph <= 9.0));

ALTER TABLE bed ADD CONSTRAINT bed_sun_exposure_chk
    CHECK (sun_exposure IS NULL OR sun_exposure IN ('FULL_SUN','PARTIAL_SUN','PARTIAL_SHADE','FULL_SHADE'));

ALTER TABLE bed ADD CONSTRAINT bed_drainage_chk
    CHECK (drainage IS NULL OR drainage IN ('POOR','MODERATE','GOOD','SHARP'));

ALTER TABLE bed ADD CONSTRAINT bed_aspect_chk
    CHECK (aspect IS NULL OR aspect IN ('FLAT','N','NE','E','SE','S','SW','W','NW'));

ALTER TABLE bed ADD CONSTRAINT bed_irrigation_type_chk
    CHECK (irrigation_type IS NULL OR irrigation_type IN ('DRIP','SPRINKLER','SOAKER_HOSE','MANUAL','NONE'));

ALTER TABLE bed ADD CONSTRAINT bed_protection_chk
    CHECK (protection IS NULL OR protection IN ('OPEN_FIELD','ROW_COVER','LOW_TUNNEL','HIGH_TUNNEL','GREENHOUSE','COLDFRAME'));
