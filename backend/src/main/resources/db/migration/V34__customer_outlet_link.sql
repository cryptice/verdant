-- Sale tracking, customer cleanup: a customer is now a person/account at an
-- outlet. Customer.channel duplicated outlet.channel; outlet is the source of
-- truth for channel classification. Nullable outlet_id supports anonymous /
-- unassigned customers (legacy rows).

ALTER TABLE customer ADD COLUMN outlet_id BIGINT REFERENCES outlet(id) ON DELETE SET NULL;
CREATE INDEX idx_customer_outlet ON customer(outlet_id) WHERE outlet_id IS NOT NULL;

ALTER TABLE customer DROP COLUMN channel;
