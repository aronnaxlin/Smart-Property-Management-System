import random
import os
from datetime import datetime, timedelta

def generate_data():
    owners_count = 100
    properties_count = 120

    users = []
    properties = []
    wallets = []
    fees = []
    cards = []

    # Track unique house codes to avoid duplicates
    unique_houses = set()

    # 1. Users (Admins)
    users.append("('admin', '123456', 'ADMIN', '超级管理员', 'Male', '13811110000')")
    users.append("('manager', '123456', 'ADMIN', '物业经理', 'Female', '13811110001')")

    first_names = ["张", "王", "李", "赵", "刘", "陈", "杨", "黄", "吴", "周", "徐", "孙", "马", "朱", "胡", "郭", "何", "高", "林", "罗"]
    last_names = ["伟", "芳", "娜", "敏", "静", "志强", "秀英", "丽", "强", "磊", "洋", "勇", "杰", "娟", "涛", "鹏", "刚", "平", "辉", "超"]
    genders = ["Male", "Female"]

    # 2. Owners (100)
    for i in range(1, owners_count + 1):
        uname = f"owner_{i}"
        name = random.choice(first_names) + random.choice(last_names)
        gender = random.choice(genders)
        phone = f"139{i:08d}"
        users.append(f"('{uname}', '123456', 'OWNER', '{name}', '{gender}', '{phone}')")

    # 3. Properties (120)
    owner_ids = list(range(3, owners_count + 3)) # admin is 1,2
    random.shuffle(owner_ids)

    buildings = ["A1", "A2", "A3", "B1", "B2", "B3", "C1", "C2"]

    def get_unique_house():
        while True:
            b = random.choice(buildings)
            u = str(random.randint(1, 3))
            r = str(random.randint(1, 20) * 100 + random.randint(1, 10)) # e.g., 101, 502
            code = (b, u, r)
            if code not in unique_houses:
                unique_houses.add(code)
                return code

    # Assign properties to owners
    for owner_id in owner_ids:
        b, u, r = get_unique_house()
        area = round(random.uniform(80.5, 150.0), 1)
        properties.append(f"('{b}', '{u}', '{r}', {area}, 'SOLD', {owner_id})")

    # Assign some extra properties to the first 20 owners
    for i in range(20):
        owner_id = owner_ids[i]
        b, u, r = get_unique_house()
        area = round(random.uniform(80.5, 150.0), 1)
        properties.append(f"('{b}', '{u}', '{r}', {area}, 'SOLD', {owner_id})")

    # Add some unsold ones
    for i in range(5):
        b, u, r = get_unique_house()
        area = 100.0
        properties.append(f"('{b}', '{u}', '{r}', {area}, 'UNSOLD', NULL)")

    # 4. Wallets
    for i in range(3, owners_count + 3):
        balance = round(random.uniform(0, 5000), 2)
        recharged = balance + round(random.uniform(1000, 5000), 2)
        wallets.append(f"({i}, {balance}, {recharged})")

    # 5. Fees & Cards (for each property)
    fee_types = ['PROPERTY_FEE', 'HEATING_FEE', 'WATER_FEE', 'ELECTRICITY_FEE']

    for i in range(1, len(properties) - 5 + 1): # assigned properties
        # Cards
        w_bal = round(random.uniform(10, 200), 2)
        e_bal = round(random.uniform(10, 200), 2)
        cards.append(f"({i}, 'WATER', {w_bal}, NOW())")
        cards.append(f"({i}, 'ELECTRICITY', {e_bal}, NOW())")

        # Fees
        has_arrears = (3 <= (owner_ids[i-1] if i <= len(owner_ids) else owner_ids[0]) <= 22)

        for ft in fee_types:
            amount = round(random.uniform(50, 300), 2) if 'FEE' in ft else round(random.uniform(20, 100), 2)
            if ft == 'HEATING_FEE': amount = round(random.uniform(1000, 2000), 2)

            is_paid = 1
            pay_date = "'2025-12-01 10:00:00'"
            method = "'WALLET'"
            if ft == 'WATER_FEE': method = "'WATER_CARD'"
            if ft == 'ELECTRICITY_FEE': method = "'ELEC_CARD'"

            if has_arrears and random.random() < 0.3:
                is_paid = 0
                pay_date = "NULL"

            fees.append(f"({i}, '{ft}', {amount}, {is_paid}, {method}, {pay_date})")

    # Output SQL
    sql = []
    sql.append("-- 智慧物业管理系统 - 大量测试数据")
    sql.append("USE property_management;")
    sql.append("SET FOREIGN_KEY_CHECKS = 0;")
    sql.append("TRUNCATE TABLE wallet_transactions; TRUNCATE TABLE user_wallets; TRUNCATE TABLE utility_cards; TRUNCATE TABLE fees; TRUNCATE TABLE properties; TRUNCATE TABLE users;")
    sql.append("SET FOREIGN_KEY_CHECKS = 1;")

    sql.append("INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES " + ",\n".join(users) + ";")
    sql.append("INSERT INTO properties (building_no, unit_no, room_no, area, p_status, user_id) VALUES " + ",\n".join(properties) + ";")
    sql.append("INSERT INTO user_wallets (user_id, balance, total_recharged) VALUES " + ",\n".join(wallets) + ";")
    sql.append("INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES " + ",\n".join(fees) + ";")
    sql.append("INSERT INTO utility_cards (p_id, card_type, balance, last_topup) VALUES " + ",\n".join(cards) + ";")

    sql.append("\n-- 数据插入完成提示")
    sql.append("SELECT '==================================================' as '';")
    sql.append("SELECT 'Test Data Inserted Successfully!' as Status;")
    sql.append("SELECT '==================================================' as '';")

    # Write to file
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, "data.sql")

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(sql))

    print(f"✅ Success: Mock data written to {output_path}")
    print(f"📊 Stats: {len(users)} users, {len(properties)} properties, {len(fees)} fee records.")

if __name__ == "__main__":
    generate_data()
