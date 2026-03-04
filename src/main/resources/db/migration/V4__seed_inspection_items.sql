-- Seed inspection item templates for different categories
-- Idempotent inserts: each item inserts only when same (name, template_type) is absent

INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '外观成色', '检查机身/表面磨损、划痕、磕碰情况', 1, 'BASIC', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '外观成色' AND template_type = 'BASIC'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '功能完整性', '检查核心功能是否正常可用', 2, 'BASIC', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '功能完整性' AND template_type = 'BASIC'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '配件齐全度', '检查原装配件或必要组件是否齐全', 3, 'BASIC', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '配件齐全度' AND template_type = 'BASIC'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '序列号/来源核验', '核验编号、来源信息及基础合规性', 4, 'BASIC', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '序列号/来源核验' AND template_type = 'BASIC'
);

INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '屏幕显示', '检测亮点、坏点、偏色、触控响应', 1, 'DIGITAL', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '屏幕显示' AND template_type = 'DIGITAL'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '电池健康', '检测电池健康度、充放电与续航稳定性', 2, 'DIGITAL', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '电池健康' AND template_type = 'DIGITAL'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '摄像/音频', '检测摄像头、扬声器、麦克风功能', 3, 'DIGITAL', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '摄像/音频' AND template_type = 'DIGITAL'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '通信模块', '检测蓝牙/Wi-Fi/蜂窝网络模块可用性', 4, 'DIGITAL', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '通信模块' AND template_type = 'DIGITAL'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '机身结构', '检测边框、后盖、接口及按键状态', 5, 'DIGITAL', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '机身结构' AND template_type = 'DIGITAL'
);

INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '面料状态', '检测面料起球、破损、污渍、褪色情况', 1, 'CLOTHING', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '面料状态' AND template_type = 'CLOTHING'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '版型与尺码', '核对尺码标注与版型完整性', 2, 'CLOTHING', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '版型与尺码' AND template_type = 'CLOTHING'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '五金与配件', '检测拉链、扣件、金属件氧化磨损情况', 3, 'CLOTHING', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '五金与配件' AND template_type = 'CLOTHING'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '走线工艺', '检测缝线脱线、开胶和工艺完整性', 4, 'CLOTHING', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '走线工艺' AND template_type = 'CLOTHING'
);

INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '结构稳定性', '检测结构件是否牢固、松动或变形', 1, 'LIFESTYLE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '结构稳定性' AND template_type = 'LIFESTYLE'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '功能开关', '检测开关、控制部件是否正常', 2, 'LIFESTYLE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '功能开关' AND template_type = 'LIFESTYLE'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '耗材损耗', '检测易损件、耗材老化与使用寿命', 3, 'LIFESTYLE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '耗材损耗' AND template_type = 'LIFESTYLE'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '安全风险', '检测漏电、异味、结构锐边等风险点', 4, 'LIFESTYLE', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '安全风险' AND template_type = 'LIFESTYLE'
);

INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '封面品相', '检测封面污渍、折角、破损', 1, 'BOOKS', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '封面品相' AND template_type = 'BOOKS'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '书脊装订', '检测书脊开裂、掉页、装订牢固度', 2, 'BOOKS', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '书脊装订' AND template_type = 'BOOKS'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '内页完整', '检测缺页、污渍、笔记划线情况', 3, 'BOOKS', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '内页完整' AND template_type = 'BOOKS'
);
INSERT INTO inspection_items (name, description, sequence_order, template_type, created_at, updated_at)
SELECT '版本核验', '检测版次印次、ISBN等基础信息', 4, 'BOOKS', NOW(), NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM inspection_items WHERE name = '版本核验' AND template_type = 'BOOKS'
);
