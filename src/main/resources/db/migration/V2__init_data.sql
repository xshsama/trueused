/*
 * V2__init_data.sql
 * Initial Data Population
 */

-- Roles
INSERT INTO roles (name, description, created_at, updated_at) VALUES 
('ROLE_USER', 'Standard User Role', NOW(), NOW()),
('ROLE_ADMIN', 'Administrator Role', NOW(), NOW())
ON DUPLICATE KEY UPDATE description=VALUES(description);

-- Root Categories
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(1, '数码产品', NULL, 'digital', '/1', 'ACTIVE', NOW(), NOW()),
(2, '服装鞋帽', NULL, 'clothing', '/2', 'ACTIVE', NOW(), NOW()),
(3, '生活家居', NULL, 'home', '/3', 'ACTIVE', NOW(), NOW()),
(4, '书籍教材', NULL, 'books', '/4', 'ACTIVE', NOW(), NOW()),
(5, '运动器材', NULL, 'sports', '/5', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Digital (1)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(11, '手机', 1, 'phones', '/1/11', 'ACTIVE', NOW(), NOW()),
(12, '电脑', 1, 'computers', '/1/12', 'ACTIVE', NOW(), NOW()),
(13, '相机', 1, 'cameras', '/1/13', 'ACTIVE', NOW(), NOW()),
(14, '平板', 1, 'tablets', '/1/14', 'ACTIVE', NOW(), NOW()),
(15, '配件', 1, 'accessories', '/1/15', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Clothing (2)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(21, '男装', 2, 'mens-clothing', '/2/21', 'ACTIVE', NOW(), NOW()),
(22, '女装', 2, 'womens-clothing', '/2/22', 'ACTIVE', NOW(), NOW()),
(23, '鞋靴', 2, 'shoes', '/2/23', 'ACTIVE', NOW(), NOW()),
(24, '箱包', 2, 'bags', '/2/24', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Home (3)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(31, '家具', 3, 'furniture', '/3/31', 'ACTIVE', NOW(), NOW()),
(32, '家电', 3, 'appliances', '/3/32', 'ACTIVE', NOW(), NOW()),
(33, '日用', 3, 'daily-use', '/3/33', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Books (4)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(41, '教材', 4, 'textbooks', '/4/41', 'ACTIVE', NOW(), NOW()),
(42, '小说', 4, 'novels', '/4/42', 'ACTIVE', NOW(), NOW()),
(43, '工具书', 4, 'reference-books', '/4/43', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Sports (5)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(51, '球类', 5, 'balls', '/5/51', 'ACTIVE', NOW(), NOW()),
(52, '健身器材', 5, 'fitness', '/5/52', 'ACTIVE', NOW(), NOW()),
(53, '户外装备', 5, 'outdoor', '/5/53', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Coupons
INSERT INTO coupons (code, title, description, discount_amount, min_spend, valid_days, type, is_active, created_at, updated_at) VALUES 
('PROMO_POLISH_1', '商品擦亮券', '用于擦亮商品，提升曝光率', 0.00, 0.00, 30, 'PROMOTION', TRUE, NOW(), NOW()),
('PROMO_POLISH_NEW', '新人推广券', '新人专属，免费擦亮商品', 0.00, 0.00, 60, 'PROMOTION', TRUE, NOW(), NOW()),
('PROMO_VVIP', 'VIP推广券', '尊贵VIP专属推广资源', 0.00, 0.00, 90, 'PROMOTION', TRUE, NOW(), NOW())
ON DUPLICATE KEY UPDATE title=VALUES(title);
