-- Sale tracking, prep step 1: drop columns the new model supersedes.
--
-- plant_event.harvest_destination_id: previously a primitive sale-attribution
-- (which customer received this harvest). Replaced by sale.outlet_id /
-- sale.customer_id joined via sale_lot.harvest_event_id.
--
-- bouquet.price_cents: the lot owns prices now (initial_requested,
-- current_requested, plus per-sale price). bouquet_recipe.price_cents stays
-- as the template default.

DROP INDEX IF EXISTS idx_plant_event_harvest_destination_id;
ALTER TABLE plant_event DROP COLUMN harvest_destination_id;

ALTER TABLE bouquet DROP COLUMN price_cents;
