-- Remove the in-tree marketplace feature.
-- Marketplace functionality is moving to a separate product (Blomsterportalen)
-- that Verdant will integrate with over HTTP, so the local tables and indexes
-- are no longer needed.

DROP TABLE IF EXISTS order_item CASCADE;
DROP TABLE IF EXISTS market_order CASCADE;
DROP TABLE IF EXISTS listing CASCADE;
