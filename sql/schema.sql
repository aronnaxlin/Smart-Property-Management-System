-- 智慧物业管理系统 - 完整数据库表结构（包含钱包系统）
-- Smart Property Management System - Complete Schema with Wallet System

CREATE DATABASE IF NOT EXISTS property_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE property_management;

DROP TABLE IF EXISTS wallet_transactions;
DROP TABLE IF EXISTS user_wallets;
DROP TABLE IF EXISTS utility_cards;
DROP TABLE IF EXISTS fees;
DROP TABLE IF EXISTS properties;
DROP TABLE IF EXISTS users;

-- 1. 用户表 (系统账号 + 业主档案)
-- 合并优化: 将原 Owner 表信息合并至此，消除传递依赖
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    user_name VARCHAR(50) NOT NULL COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT '加密后的密码',
    user_type VARCHAR(20) NOT NULL COMMENT '用户类型: ADMIN, OWNER',

    -- 业主档案字段 (Merge from owners)
    name VARCHAR(50) COMMENT '真实姓名',
    gender VARCHAR(10) COMMENT '性别',
    phone VARCHAR(20) UNIQUE COMMENT '联系电话',

    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_user_name (user_name),
    -- 数据约束
    CONSTRAINT chk_user_type CHECK (user_type IN ('ADMIN', 'OWNER')),
    CONSTRAINT chk_gender CHECK (gender IN ('Male', 'Female', ''))
) DEFAULT CHARSET=utf8mb4 COMMENT='系统用户与业主档案表';

-- 2. 房产信息表
CREATE TABLE properties (
    p_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    building_no VARCHAR(20) NOT NULL COMMENT '楼栋号',
    unit_no VARCHAR(20) NOT NULL COMMENT '单元号',
    room_no VARCHAR(20) NOT NULL COMMENT '房间号',
    area DECIMAL(10, 2) COMMENT '建筑面积(平方米)',
    p_status VARCHAR(20) DEFAULT 'UNSOLD' COMMENT '房屋状态: SOLD-已售, UNSOLD-未售, RENTED-已租',

    -- 直接关联用户表 (Flattened Hierarchy)
    user_id BIGINT COMMENT '关联业主(用户)ID',
    CONSTRAINT fk_property_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,

    UNIQUE KEY uk_house_code (building_no, unit_no, room_no),
    CONSTRAINT chk_p_status CHECK (p_status IN ('SOLD', 'UNSOLD', 'RENTED'))
) DEFAULT CHARSET=utf8mb4 COMMENT='房产资源表';

-- 3. 费用账单表（包含支付方式字段）
CREATE TABLE fees (
    f_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    p_id BIGINT NOT NULL COMMENT '关联房产ID',
    fee_type VARCHAR(50) NOT NULL COMMENT '费用类型: PROPERTY_FEE-物业费, HEATING_FEE-取暖费, WATER_FEE-水费, ELECTRICITY_FEE-电费',
    amount DECIMAL(10, 2) NOT NULL COMMENT '账单金额',
    is_paid TINYINT(1) DEFAULT 0 COMMENT '缴费状态: 0-未缴, 1-已缴',

    -- 新增：支付方式字段
    payment_method VARCHAR(20) DEFAULT 'WALLET' COMMENT '支付方式: WALLET-钱包, WATER_CARD-水卡, ELEC_CARD-电卡',

    pay_date DATETIME COMMENT '缴费时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '账单生成时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间',

    CONSTRAINT fk_fee_property FOREIGN KEY (p_id) REFERENCES properties(p_id) ON DELETE CASCADE,

    -- 核心性能优化: 联合索引用于快速检测欠费 (Hard Interception)
    INDEX idx_check_arrears (p_id, is_paid),
    INDEX idx_payment_method (payment_method, is_paid),

    CONSTRAINT chk_fee_type CHECK (fee_type IN ('PROPERTY_FEE', 'HEATING_FEE', 'WATER_FEE', 'ELECTRICITY_FEE')),
    CONSTRAINT chk_is_paid CHECK (is_paid IN (0, 1)),
    CONSTRAINT chk_payment_method CHECK (payment_method IN ('WALLET', 'WATER_CARD', 'ELEC_CARD'))
) DEFAULT CHARSET=utf8mb4 COMMENT='物业缴费账单表';

-- 4. 水电卡表
CREATE TABLE utility_cards (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    p_id BIGINT NOT NULL COMMENT '关联房产ID',
    card_type VARCHAR(20) NOT NULL COMMENT '卡片类型: WATER-水卡, ELECTRICITY-电卡',
    balance DECIMAL(10, 2) DEFAULT 0.00 COMMENT '卡内余额',
    last_topup DATETIME COMMENT '最后充值时间',
    CONSTRAINT fk_card_property FOREIGN KEY (p_id) REFERENCES properties(p_id) ON DELETE CASCADE,
    UNIQUE KEY uk_card_property_type (p_id, card_type),
    CONSTRAINT chk_card_type CHECK (card_type IN ('WATER', 'ELECTRICITY'))
) DEFAULT CHARSET=utf8mb4 COMMENT='水电卡自助管理表';

-- 5. 用户钱包表（新增）
-- 一个用户只有一个钱包
CREATE TABLE user_wallets (
    wallet_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '钱包ID',
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID (一对一关系)',
    balance DECIMAL(10, 2) DEFAULT 0.00 COMMENT '钱包余额',
    total_recharged DECIMAL(10, 2) DEFAULT 0.00 COMMENT '累计充值金额',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_balance (user_id, balance),
    CONSTRAINT chk_balance CHECK (balance >= 0)
) DEFAULT CHARSET=utf8mb4 COMMENT='用户钱包表';

-- 6. 钱包交易记录表（新增）
CREATE TABLE wallet_transactions (
    trans_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '交易ID',
    wallet_id BIGINT NOT NULL COMMENT '钱包ID',
    trans_type VARCHAR(20) NOT NULL COMMENT '交易类型: RECHARGE-充值, PAY_FEE-缴费, TOPUP_CARD-卡充值',
    amount DECIMAL(10, 2) NOT NULL COMMENT '交易金额',
    balance_after DECIMAL(10, 2) NOT NULL COMMENT '交易后余额',
    related_id BIGINT COMMENT '关联ID(fee_id或card_id)',
    description VARCHAR(200) COMMENT '交易描述',
    trans_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',

    CONSTRAINT fk_trans_wallet FOREIGN KEY (wallet_id) REFERENCES user_wallets(wallet_id) ON DELETE CASCADE,
    INDEX idx_wallet_time (wallet_id, trans_time),
    CONSTRAINT chk_trans_type CHECK (trans_type IN ('RECHARGE', 'PAY_FEE', 'TOPUP_CARD'))
) DEFAULT CHARSET=utf8mb4 COMMENT='钱包交易记录表';

-- 创建数据库用户
CREATE USER IF NOT EXISTS 'propertyAdmin'@'%' IDENTIFIED BY 'realAronnaxlin917-';
GRANT ALL PRIVILEGES ON property_management.* TO 'propertyAdmin'@'%';
FLUSH PRIVILEGES;

-- 完成提示
SELECT 'Database schema created successfully with wallet system!' as Status;