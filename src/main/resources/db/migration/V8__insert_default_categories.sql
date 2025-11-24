-- Root Categories
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(1, '数码产品', NULL, 'digital', '/1', 'ACTIVE', NOW(), NOW()),
(2, '服装鞋帽', NULL, 'clothing', '/2', 'ACTIVE', NOW(), NOW()),
(3, '生活家居', NULL, 'home', '/3', 'ACTIVE', NOW(), NOW()),
(4, '书籍教材', NULL, 'books', '/4', 'ACTIVE', NOW(), NOW()),
(5, '运动器材', NULL, 'sports', '/5', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();

-- Digital (1)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(11, '手机', 1, 'phones', '/1/11', 'ACTIVE', NOW(), NOW()),
(12, '电脑', 1, 'computers', '/1/12', 'ACTIVE', NOW(), NOW()),
(13, '相机', 1, 'cameras', '/1/13', 'ACTIVE', NOW(), NOW()),
(14, '平板', 1, 'tablets', '/1/14', 'ACTIVE', NOW(), NOW()),
(15, '配件', 1, 'accessories', '/1/15', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();

-- Clothing (2)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(21, '男装', 2, 'mens-clothing', '/2/21', 'ACTIVE', NOW(), NOW()),
(22, '女装', 2, 'womens-clothing', '/2/22', 'ACTIVE', NOW(), NOW()),
(23, '鞋靴', 2, 'shoes', '/2/23', 'ACTIVE', NOW(), NOW()),
(24, '箱包', 2, 'bags', '/2/24', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();

-- Home (3)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(31, '家具', 3, 'furniture', '/3/31', 'ACTIVE', NOW(), NOW()),
(32, '家电', 3, 'appliances', '/3/32', 'ACTIVE', NOW(), NOW()),
(33, '日用', 3, 'daily-use', '/3/33', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();

-- Books (4)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(41, '教材', 4, 'textbooks', '/4/41', 'ACTIVE', NOW(), NOW()),
(42, '小说', 4, 'novels', '/4/42', 'ACTIVE', NOW(), NOW()),
(43, '课外书', 4, 'extracurricular', '/4/43', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();

-- Sports (5)
INSERT INTO categories (id, name, parent_id, slug, path, status, created_at, updated_at) VALUES
(51, '球类', 5, 'ball-games', '/5/51', 'ACTIVE', NOW(), NOW()),
(52, '健身', 5, 'fitness', '/5/52', 'ACTIVE', NOW(), NOW()),
(53, '户外', 5, 'outdoors', '/5/53', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
name = VALUES(name),
parent_id = VALUES(parent_id),
slug = VALUES(slug),
path = VALUES(path),
status = VALUES(status),
updated_at = NOW();
