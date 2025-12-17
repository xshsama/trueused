-- Update Product Statuses
UPDATE products SET status = 'PENDING' WHERE status IN ('CREATED', 'SHIPPED', 'RECEIVED', 'INSPECTING', 'WAREHOUSED');
UPDATE products SET status = 'SOLD' WHERE status = 'SOLD_OUT';
UPDATE products SET status = 'OFF_SHELF' WHERE status IN ('CANCELLED', 'REJECTED', 'RETURNED');

-- Consignment statuses are compatible, no update needed for existing data.
-- New statuses (RETURNING, RETURNED) will be used for new/future flows.
