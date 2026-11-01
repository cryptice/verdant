-- A variety's usual package size, so its price can be entered and shown per package as well as per seed.
ALTER TABLE species ADD COLUMN seeds_per_package INT CHECK (seeds_per_package > 0);
