-- users, user_wallets, wallet_transactions, properties, utility_cards, fees

-- user 1:1 user_wallets
-- user_wallets 1:n wallet_transactions
-- properties 1:n utility_cards
-- properties 1:n fees

create table users(
    -- user config
    user_id bigint auto_increment primary key,
    user_name varchar(50) not null,
    password varchar(100) not null,
    user_type varchar(20) not null,
    -- owner config
    name varchar(50),
    gender varchar(10),
    phone varchar(20) unique,
    -- time stamp
    created_at DATETIME DEFAULT current_timestamp,
    updated_at DATETIME DEFAULT current_timestamp ON UPDATE current_timestamp,
    -- constraints
    constraint chk_user_type CHECK (user_type in ('ADMIN', 'OWNER')),
    constraint chk_gender check (gender in ('MALE', 'FEMALE', '')),
    -- index
    INDEX idx_user_name(user_name)
) DEFAULT CHARSET=utf8mb4;

-- user_id, user_name, password, user_type, name, gender, phone, created_at, updated_at

create table user_wallet(
    wallet_id bigint auto_increment primary key,
    user_id BIGINT not null unique,
    balance decimal(10,2) default 0.00,
    total_recharged decimal(10,2) default 0.00,
    created_at DATETIME DEFAULT current_timestamp,
    updated_at DATETIME DEFAULT current_timestamp ON UPDATE current_timestamp,

    -- constraints
    constraint fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_balance (user_id, balance),
    constraint chk_balance CHECK (balance >= 0)
) DEFAULT CHARSET = utf8mb4;

-- wallet_id, user_id (fk), balance, total_recharged, updated_at, created_at

CREATE TABLE wallet_transactions(
    trans_id bigint auto_increment primary key,
    wallet_id bigint not null,
    trans_type varchar(20) not null comment 'PAY_FEE, RECHARGE, TOPUP_CARD',
    amount DECIMAL(10,2) not null,
    balance_after decimal(10,2) not null,
    related_id bigint,
    description varchar(200),
    trans_time DATETIME DEFAULT current_timestamp,

    -- constraints
    constraint fk_trans_wallet FOREIGN KEY (wallet_id) REFERENCES user_wallets(wallet_id) ON DELETE CASCADE,
    constraint chk_trans_type CHECK (trans_type in ('RECHARGE', 'PAY_FEE', 'TOPUP_CARD'))
    index idx_wallet_time(wallet_id, trans_time)
) DEFAULT CHARSET = utf8mb4;

-- trans_id, wallet_id, trans_type, amount, balance_after, related_id, description, trans_time


create table properties(
    p_id bigint auto_increment primary key,
    building_no varchar(20) not null,
    unit_no varchar(20) not null,
    room_no varchar(20) not null,
    area decimal(10,2), -- 建筑面积
    p_status varchar(20) default 'UNSOLD',

    user_id bigint,
    constraint fk_property_user foreign key (user_id) references users(user_id) on delete set null,
    unique key uk_house_code (building_no, unit_no, room_no),
    constraint chk_p_status check (p_status in ('UNSOLD', 'SOLD', 'RENTED'))
) DEFAULT CHARSET=utf8mb4;

create table fee(
    f_id bigint auto_increment primary key,
    p_id bigint not null,
    fee_type varchar(20) not null, -- 费用类型 4
    is_paid tinyint(1) not null default 0,
    pay_method varchar(20) not null,

    pay_date DATETIME,
    created_at DATETIME DEFAULT current_timestamp,
    updated_at DATETIME DEFAULT current_timestamp ON UPDATE current_timestamp,

    constraint fk_fee_properties foreign key (p_id) references properties(p_id) on delete CASCADE,

    index idx_check_arrears(p_id, is_paid),
    index idx_payment_method(pay_method, is_paid),

    constraint chk_fee_type check (fee_type in ('PROPERTY_FEE', 'HEATING_FEE', 'WATER_FEE', 'ELECTRICITY_FEE')),
    constraint chk_is_paid check (is_paid in (0,1)),
    constraint chk_payment_method check(pay_method in ('WALLET', 'ELECTRICITY_CARD', 'WATER_CARD'))
)default charset = utf8mb4;

CREATE TABLE utility_cards (
    card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    p_id BIGINT NOT NULL COMMENT,
    card_type VARCHAR(20) NOT NULL COMMENT '卡片类型: WATER-水卡, ELECTRICITY-电卡',
    balance DECIMAL(10, 2) DEFAULT 0.00 COMMENT '卡内余额',
    last_topup DATETIME,
    CONSTRAINT fk_card_property FOREIGN KEY (p_id) REFERENCES properties(p_id) ON DELETE CASCADE,
    UNIQUE KEY uk_card_property_type (p_id, card_type),
    CONSTRAINT chk_card_type CHECK (card_type IN ('WATER', 'ELECTRICITY'))
) DEFAULT CHARSET=utf8mb4 COMMENT='水电卡自助管理表';