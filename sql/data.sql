-- 智慧物业管理系统 - 完整测试数据（包含钱包系统）
-- Smart Property Management System - Complete Test Data with Wallet System

USE property_management;

-- 清空现有数据
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE wallet_transactions;
TRUNCATE TABLE user_wallets;
TRUNCATE TABLE utility_cards;
TRUNCATE TABLE fees;
TRUNCATE TABLE properties;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 1. 填充用户数据（包含业主档案）
-- 默认密码: 123456
INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES
('admin', '123456', 'ADMIN', '系统管理员', 'Male', '13800000000'),
('owner_liang', '123456', 'OWNER', '梁朝伟', 'Male', '13800000001'),
('owner_zhang', '123456', 'OWNER', '张曼玉', 'Female', '13800000002'),
('owner_lau', '123456', 'OWNER', '刘嘉玲', 'Female', '13800000003'),
('owner_chow', '123456', 'OWNER', '周星驰', 'Male', '13800000004');

-- 2. 填充房产数据
INSERT INTO properties (building_no, unit_no, room_no, area, p_status, user_id) VALUES
('A1', '1', '101', 89.5, 'SOLD', 2),   -- 梁朝伟
('A1', '1', '102', 120.0, 'SOLD', 3),  -- 张曼玉
('A2', '2', '201', 95.0, 'SOLD', 4),   -- 刘嘉玲
('A2', '2', '202', 95.0, 'SOLD', 5),   -- 周星驰
('B1', '1', '301', 110.0, 'UNSOLD', NULL);

-- 3. 创建用户钱包
-- 为所有业主创建钱包，初始余额不同
INSERT INTO user_wallets (user_id, balance, total_recharged) VALUES
(2, 2000.00, 5000.00),  -- 梁朝伟：余额充足
(3, 500.00, 2000.00),   -- 张曼玉：余额中等
(4, 50.00, 500.00),     -- 刘嘉玲：余额较少
(5, 10.00, 100.00);     -- 周星驰：余额很少（用于测试余额不足）

-- 4. 填充费用数据
-- 包含不同支付方式和缴费状态的费用

-- 梁朝伟的费用（全部已缴）
INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES
(1, 'PROPERTY_FEE', 268.50, 1, 'WALLET', '2025-12-01 10:00:00'),
(1, 'HEATING_FEE', 1500.00, 1, 'WALLET', '2025-12-01 10:05:00'),
(1, 'WATER_FEE', 50.00, 1, 'WATER_CARD', '2025-12-15 09:00:00'),
(1, 'ELECTRICITY_FEE', 120.00, 1, 'ELEC_CARD', '2025-12-15 09:05:00');

-- 张曼玉的费用（有未缴的钱包费用 - 用于测试硬拦截）
INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES
(2, 'PROPERTY_FEE', 360.00, 0, 'WALLET', NULL),  -- ❌ 未缴物业费
(2, 'HEATING_FEE', 1800.00, 1, 'WALLET', '2025-12-05 14:30:00'),
(2, 'WATER_FEE', 80.00, 0, 'WATER_CARD', NULL),  -- 水费未缴，但不影响钱包充值卡
(2, 'ELECTRICITY_FEE', 150.00, 1, 'ELEC_CARD', '2025-12-10 10:00:00');

-- 刘嘉玲的费用（只有物业费已缴）
INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES
(3, 'PROPERTY_FEE', 285.00, 1, 'WALLET', '2025-12-10 09:20:00'),
(3, 'WATER_FEE', 60.00, 0, 'WATER_CARD', NULL);

-- 周星驰的费用（有多笔未缴的钱包费用）
INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES
(4, 'PROPERTY_FEE', 285.00, 0, 'WALLET', NULL),     -- ❌ 未缴物业费
(4, 'HEATING_FEE', 1500.00, 0, 'WALLET', NULL),     -- ❌ 未缴取暖费
(4, 'WATER_FEE', 40.00, 0, 'WATER_CARD', NULL),
(4, 'ELECTRICITY_FEE', 100.00, 0, 'ELEC_CARD', NULL);

-- 5. 填充水电卡数据
INSERT INTO utility_cards (p_id, card_type, balance, last_topup) VALUES
-- 梁朝伟（余额充足）
(1, 'WATER', 150.00, '2025-11-20 09:00:00'),
(1, 'ELECTRICITY', 200.00, '2025-11-20 09:00:00'),

-- 张曼玉（有余额）
(2, 'WATER', 80.00, '2025-12-05 15:00:00'),
(2, 'ELECTRICITY', 120.00, '2025-12-05 15:00:00'),

-- 刘嘉玲（余额较少）
(3, 'WATER', 10.00, '2025-11-15 16:20:00'),
(3, 'ELECTRICITY', 15.00, '2025-11-15 16:20:00'),

-- 周星驰（余额很少）
(4, 'WATER', 5.00, '2025-10-10 10:00:00'),
(4, 'ELECTRICITY', 8.00, '2025-10-10 10:00:00');

-- 6. 填充钱包交易记录
-- 为已有余额的钱包创建历史交易记录

-- 梁朝伟的交易记录
INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description, trans_time) VALUES
(1, 'RECHARGE', 5000.00, 5000.00, NULL, '钱包充值: +5000元', '2025-11-01 08:00:00'),
(1, 'PAY_FEE', 268.50, 4731.50, 1, '缴费: PROPERTY_FEE -268.5元', '2025-12-01 10:00:00'),
(1, 'PAY_FEE', 1500.00, 3231.50, 2, '缴费: HEATING_FEE -1500元', '2025-12-01 10:05:00'),
(1, 'TOPUP_CARD', 1000.00, 2231.50, 1, '充值WATER卡: -1000元', '2025-11-20 09:00:00'),
(1, 'TOPUP_CARD', 231.50, 2000.00, 2, '充值ELECTRICITY卡: -231.5元', '2025-11-20 09:05:00');

-- 张曼玉的交易记录
INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description, trans_time) VALUES
(2, 'RECHARGE', 2000.00, 2000.00, NULL, '钱包充值: +2000元', '2025-11-15 10:00:00'),
(2, 'PAY_FEE', 1800.00, 200.00, 6, '缴费: HEATING_FEE -1800元', '2025-12-05 14:30:00'),
(2, 'RECHARGE', 500.00, 700.00, NULL, '钱包充值: +500元', '2025-12-20 15:00:00'),
(2, 'TOPUP_CARD', 200.00, 500.00, 3, '充值WATER卡: -200元', '2025-12-20 15:10:00');

-- 刘嘉玲的交易记录
INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description, trans_time) VALUES
(3, 'RECHARGE', 500.00, 500.00, NULL, '钱包充值: +500元', '2025-11-01 09:00:00'),
(3, 'PAY_FEE', 285.00, 215.00, 9, '缴费: PROPERTY_FEE -285元', '2025-12-10 09:20:00'),
(3, 'TOPUP_CARD', 165.00, 50.00, 5, '充值WATER卡: -165元', '2025-11-15 16:20:00');

-- 周星驰的交易记录（余额很少，且有欠费）
INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description, trans_time) VALUES
(4, 'RECHARGE', 100.00, 100.00, NULL, '钱包充值: +100元', '2025-10-01 10:00:00'),
(4, 'TOPUP_CARD', 50.00, 50.00, 7, '充值WATER卡: -50元', '2025-10-10 10:00:00'),
(4, 'TOPUP_CARD', 40.00, 10.00, 8, '充值ELECTRICITY卡: -40元', '2025-10-10 10:05:00');

-- 数据插入完成提示
SELECT '==================================================' as '';
SELECT 'Test Data Inserted Successfully!' as Status;
SELECT '==================================================' as '';
SELECT '' as '';

-- 显示数据统计
SELECT '📊 数据统计:' as '';
SELECT CONCAT('用户数量: ', COUNT(*)) as Info FROM users;
SELECT CONCAT('房产数量: ', COUNT(*)) as Info FROM properties;
SELECT CONCAT('钱包数量: ', COUNT(*)) as Info FROM user_wallets;
SELECT CONCAT('费用记录: ', COUNT(*), ' 条 (未缴: ', SUM(CASE WHEN is_paid=0 THEN 1 ELSE 0 END), ' 条)') as Info FROM fees;
SELECT CONCAT('水电卡数量: ', COUNT(*)) as Info FROM utility_cards;
SELECT CONCAT('交易记录: ', COUNT(*)) as Info FROM wallet_transactions;

SELECT '' as '';
SELECT '🔍 测试场景说明:' as '';
SELECT '1. 梁朝伟 (user_id=2): 所有费用已缴，钱包余额充足，可正常充值水电卡' as Scenario;
SELECT '2. 张曼玉 (user_id=3): ❌ 有未缴物业费，充值水电卡将被拦截' as Scenario;
SELECT '3. 刘嘉玲 (user_id=4): 无钱包欠费，但余额较少' as Scenario;
SELECT '4. 周星驰 (user_id=5): ❌ 多笔欠费，余额不足，用于测试各种失败场景' as Scenario;
