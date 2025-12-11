-- Add category_id to consignments
ALTER TABLE consignments ADD COLUMN category_id BIGINT;
ALTER TABLE consignments ADD CONSTRAINT fk_consignments_category FOREIGN KEY (category_id) REFERENCES categories(id);

-- Add template_type to inspection_items
ALTER TABLE inspection_items ADD COLUMN template_type VARCHAR(50);

-- Clear existing items (optional, but safer to start fresh for the new templates)
DELETE FROM inspection_items;

-- Insert 3C Digital Template (template_type = 'DIGITAL')
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at) VALUES
('开机测试', '检查设备能否正常开机进入系统', 1, 'DIGITAL', NOW(), NOW()),
('屏幕显示', '检查屏幕是否有划痕、坏点、色差', 2, 'DIGITAL', NOW(), NOW()),
('电池健康', '检查电池循环次数及健康度', 3, 'DIGITAL', NOW(), NOW()),
('功能检测', '检查摄像头、麦克风、扬声器、按键等功能', 4, 'DIGITAL', NOW(), NOW());

-- Insert Clothing/Bags Template (template_type = 'CLOTHING')
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at) VALUES
('污渍检查', '检查表面是否有明显污渍、油渍', 1, 'CLOTHING', NOW(), NOW()),
('破损检查', '检查是否有破洞、开线、磨损', 2, 'CLOTHING', NOW(), NOW()),
('五金件检查', '检查拉链、扣子等五金件是否完好', 3, 'CLOTHING', NOW(), NOW()),
('尺码核对', '核对标签尺码与实际测量数据', 4, 'CLOTHING', NOW(), NOW());

-- Insert Books Template (template_type = 'BOOKS')
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at) VALUES
('缺页检查', '检查是否有缺页、脱页现象', 1, 'BOOKS', NOW(), NOW()),
('划痕笔记', '检查内页是否有划痕、笔记、涂鸦', 2, 'BOOKS', NOW(), NOW()),
('封面完整度', '检查封面是否有折痕、破损', 3, 'BOOKS', NOW(), NOW());

-- Insert Lifestyle/Beauty Template (template_type = 'LIFESTYLE')
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at) VALUES
('有效期检查', '检查生产日期及保质期', 1, 'LIFESTYLE', NOW(), NOW()),
('余量/磨损', '检查剩余容量或使用磨损程度', 2, 'LIFESTYLE', NOW(), NOW()),
('正品鉴定', '核对防伪标识，进行正品鉴定', 3, 'LIFESTYLE', NOW(), NOW());

-- Insert Basic Appearance Template (template_type = 'BASIC')
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at) VALUES
('外观完好', '检查整体外观是否有明显瑕疵', 1, 'BASIC', NOW(), NOW()),
('基础功能', '检查基础功能是否可用', 2, 'BASIC', NOW(), NOW());
