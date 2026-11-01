-- Sort variety names the Swedish way, with Å, Ä and Ö after Z, wherever they are ordered or compared.
ALTER TABLE species
    ALTER COLUMN common_name     TYPE VARCHAR(255) COLLATE "sv-x-icu",
    ALTER COLUMN common_name_sv  TYPE VARCHAR(255) COLLATE "sv-x-icu",
    ALTER COLUMN variant_name    TYPE VARCHAR(255) COLLATE "sv-x-icu",
    ALTER COLUMN variant_name_sv TYPE VARCHAR(255) COLLATE "sv-x-icu";
