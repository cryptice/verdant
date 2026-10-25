-- How many units one package of the batch holds. Nullable: batches entered
-- before this column existed only know their total quantity.
ALTER TABLE seed_inventory ADD COLUMN seeds_per_package INT CHECK (seeds_per_package > 0);
