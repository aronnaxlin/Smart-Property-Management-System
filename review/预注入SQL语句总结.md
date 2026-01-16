# 系统 预注入SQL语句 完整总结

> **💡 答辩要点**：所有SQL语句均使用 `PreparedStatement` 或 `JdbcTemplate` 的参数绑定机制（使用 `?` 占位符），有效防止了 **SQL 注入攻击**。这是企业级项目的标准做法。

---

## 1️⃣ UserDAO (用户档案数据访问层)

### 1.1 插入用户
```sql
INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES (?, ?, ?, ?, ?, ?)
```
**说明**：注册新用户或管理员创建业主档案时调用。使用 `KeyHolder` 获取自增ID。

**答辩要点**：这里的 `password` 字段存储的是明文密码（实际生产环境应使用 BCrypt 加密）。

---

### 1.2 删除用户
```sql
DELETE FROM users WHERE user_id = ?
```
**说明**：物理删除用户记录。由于设置了外键级联（`ON DELETE CASCADE`），删除用户会自动删除关联的钱包和房产绑定关系。

**答辩要点**：级联删除的好处是保证了数据一致性，但也要注意误删的风险。

---

### 1.3 更新用户信息（不更新密码）
```sql
UPDATE users SET user_name=?, user_type=?, name=?, phone=? WHERE user_id=?
```
**说明**：当前端传入的密码字段为空时，执行该SQL。避免因误操作清空密码。

**答辩要点**：条件判断在代码层实现，展示了对业务逻辑的精细控制。

---

### 1.4 更新用户信息（包含密码）
```sql
UPDATE users SET user_name=?, password=?, user_type=?, name=?, phone=? WHERE user_id=?
```
**说明**：当需要修改密码时才执行。

---

### 1.5 根据ID查询用户
```sql
SELECT * FROM users WHERE user_id = ?
```
**说明**：通过主键查询单个用户详情。

---

### 1.6 查询所有用户
```sql
SELECT * FROM users
```
**说明**：管理员后台用户列表功能。

**注意**：没有分页，如果数据量大会影响性能。

---

### 1.7 根据账号查询用户
```sql
SELECT * FROM users WHERE user_name = ?
```
**说明**：用于检查账号是否已存在（注册时唯一性校验）。

---

### 1.8 登录验证
```sql
SELECT * FROM users WHERE user_name = ? AND password = ?
```
**说明**：**核心业务SQL**，用于验证用户登录凭据。

**答辩要点**：如果评委问"为什么不用加密"，可以说："当前是明文存储，在实际部署时会引入 Spring Security + BCrypt 进行加密验证。"

---

### 1.9 模糊搜索用户
```sql
SELECT * FROM users WHERE name LIKE ? OR phone LIKE ?
```
**说明**：后台管理中按姓名或手机号模糊查询。使用 `%keyword%` 进行模式匹配。

**答辩要点**：`LIKE` 查询在大数据量下性能较差，实际生产可引入全文检索（Elasticsearch）。

---

## 2️⃣ FeeDAO (费用账单数据访问层)

### 2.1 插入账单
```sql
INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES (?, ?, ?, ?, ?, ?)
```
**说明**：物业后台为房产创建新的缴费账单。使用 `KeyHolder` 获取账单ID。

---

### 2.2 更新账单
```sql
UPDATE fees SET p_id=?, fee_type=?, amount=?, is_paid=?, payment_method=?, pay_date=? WHERE f_id=?
```
**说明**：修改账单信息或标记为已缴费。

---

### 2.3 删除账单
```sql
DELETE FROM fees WHERE f_id = ?
```
**说明**：删除错误录入的账单。

---

### 2.4 查询单笔账单
```sql
SELECT * FROM fees WHERE f_id = ?
```
**说明**：查看账单详情。

---

### 2.5 查询所有账单
```sql
SELECT * FROM fees
```
**说明**：管理员查看系统所有账单。

---

### 2.6 查询指定房产的账单
```sql
SELECT * FROM fees WHERE p_id = ?
```
**说明**：业主查看自己房产的所有历史缴费记录。

---

### 2.7 查询所有未缴费账单
```sql
SELECT * FROM fees WHERE is_paid = 0
```
**说明**：管理员催缴功能，列出所有拖欠账单。

**答辩要点**：`is_paid` 字段上有索引 `idx_check_arrears`，查询速度很快。

---

### 2.8 查询指定房产的欠费账单
```sql
SELECT * FROM fees WHERE p_id = ? AND is_paid = 0
```
**说明**：**硬性拦截逻辑**：业主充值水电卡时，系统会先查询是否有欠费。若有欠费则禁止充值。

**答辩要点**：这是联合索引 `idx_check_arrears (p_id, is_paid)` 的典型应用场景。

---

### 2.9 统计缴费率（总数）
```sql
SELECT COUNT(*) FROM fees
```
**说明**：统计系统中账单总数量。

---

### 2.10 统计缴费率（已缴数量）
```sql
SELECT COUNT(*) FROM fees WHERE is_paid = 1
```
**说明**：统计已缴清的账单数量。

**答辩要点**：这两个SQL配合 Java 代码计算出缴费比例（paid / total），用于数据大屏展示。

---

### 2.11 收入分布图表（按费用类型分组）
```sql
SELECT fee_type, SUM(amount) as total_amount FROM fees WHERE is_paid = 1 GROUP BY fee_type
```
**说明**：**数据统计核心SQL**。按费用类型（物业费、暖气费、水费、电费）分组统计已缴清的金额总额。

**答辩要点**：使用了 `GROUP BY` 和聚合函数 `SUM()`，展示了对复杂查询的掌控。

---

### 2.12 欠费严重楼栋 TOP 5
```sql
SELECT p.building_no, COUNT(*) as unpaid_count, SUM(f.amount) as unpaid_amount
FROM fees f
JOIN properties p ON f.p_id = p.p_id
WHERE f.is_paid = 0
GROUP BY p.building_no
ORDER BY unpaid_count DESC
LIMIT 5
```
**说明**：**高级联表查询**。跨表连接 `fees` 和 `properties`，统计欠费最严重的5个楼栋。

**答辩要点**：
- 使用了 `JOIN` 进行多表关联
- 使用了 `GROUP BY` 分组统计
- 使用了 `ORDER BY` 排序
- 使用了 `LIMIT` 分页截取

如果评委问"为什么按欠费笔数排序而不是按金额"，你可以说："欠费笔数更能反映业主的缴费配合度，金额可能受面积影响产生偏差。"

---

## 3️⃣ PropertyDAO (房产资源数据访问层)

### 3.1 插入房产
```sql
INSERT INTO properties (building_no, unit_no, room_no, area, p_status, user_id) VALUES (?, ?, ?, ?, ?, ?)
```
**说明**：物业后台录入新房产资源。

---

### 3.2 删除房产
```sql
DELETE FROM properties WHERE p_id = ?
```
**说明**：删除房产记录。设置了 `ON DELETE CASCADE`，会自动删除关联的费用和水电卡。

---

### 3.3 更新房产
```sql
UPDATE properties SET building_no=?, unit_no=?, room_no=?, area=?, p_status=?, user_id=? WHERE p_id=?
```
**说明**：修改房产信息或变更业主绑定。

---

### 3.4 根据ID查询房产
```sql
SELECT * FROM properties WHERE p_id = ?
```
**说明**：精确查询单个房产详情。

---

### 3.5 查询所有房产
```sql
SELECT * FROM properties
```
**说明**：管理员查看小区所有房产列表。

---

### 3.6 根据业主ID查询房产
```sql
SELECT * FROM properties WHERE user_id = ?
```
**说明**：业主登录后查看自己名下的所有房产。

---

### 3.7 根据楼、单元、房号查询房产
```sql
SELECT * FROM properties WHERE building_no = ? AND unit_no = ? AND room_no = ?
```
**说明**：**唯一性校验**。业主绑定房产时，通过门牌号查找房产。

**答辦要点**：数据库中设置了联合唯一索引 `uk_house_code (building_no, unit_no, room_no)`，保证了房产编号的唯一性。

---

## 4️⃣ UserWalletDAO (用户钱包数据访问层)

### 4.1 初始化钱包
```sql
INSERT INTO user_wallets (user_id, balance, total_recharged) VALUES (?, ?, ?)
```
**说明**：用户注册时自动创建钱包，初始余额为 0.00。

---

### 4.2 根据用户ID查询钱包
```sql
SELECT * FROM user_wallets WHERE user_id = ?
```
**说明**：**核心业务查询**。业主查看钱包余额或充值前先获取钱包对象。

**答辩要点**：`user_id` 字段设置了 `UNIQUE` 约束，保证一个用户只有一个钱包（1:1关系）。

---

### 4.3 根据钱包ID查询
```sql
SELECT * FROM user_wallets WHERE wallet_id = ?
```
**说明**：通过主键查询钱包详情。

---

### 4.4 更新钱包余额
```sql
UPDATE user_wallets SET balance=?, total_recharged=? WHERE wallet_id=?
```
**说明**：**核心业务SQL**。每次充值或扣费都会更新余额。

**答辩要点**：
- 这个SQL必须配合 **事务（@Transactional）** 使用，防止并发扣款导致余额错误。
- 如果评委问"如何保证并发安全"，可以说："在 Service 层使用了 `@Transactional` 注解，保证了钱包余额更新和流水记录插入的原子性。"

---

## 5️⃣ UtilityCardDAO (水电卡数据访问层)

### 5.1 插入水电卡
```sql
INSERT INTO utility_cards (p_id, card_type, balance, last_topup) VALUES (?, ?, ?, ?)
```
**说明**：为房产配置水卡或电卡。

---

### 5.2 删除水电卡
```sql
DELETE FROM utility_cards WHERE card_id = ?
```
**说明**：注销水电卡。

---

### 5.3 更新水电卡
```sql
UPDATE utility_cards SET p_id=?, card_type=?, balance=?, last_topup=? WHERE card_id=?
```
**说明**：充值后更新卡内余额和充值时间。

---

### 5.4 根据卡ID查询
```sql
SELECT * FROM utility_cards WHERE card_id = ?
```
**说明**：查询单张水电卡详情。

---

### 5.5 查询所有水电卡
```sql
SELECT * FROM utility_cards
```
**说明**：管理员查看系统所有水电卡。

---

### 5.6 根据房产ID查询水电卡
```sql
SELECT * FROM utility_cards WHERE p_id = ?
```
**说明**：业主查看自己房产的水卡和电卡余额。

**答辩要点**：数据库中设置了联合唯一索引 `uk_card_property_type (p_id, card_type)`，保证一个房产只能有一张水卡和一张电卡。

---

## 6️⃣ WalletTransactionDAO (钱包交易流水数据访问层)

### 6.1 插入交易记录
```sql
INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description) VALUES (?, ?, ?, ?, ?, ?)
```
**说明**：**核心业务SQL**。每次钱包充值、缴费、卡充值都会记录一条流水。

**答辩要点**：
- `trans_type` 字段区分交易类型：`RECHARGE`（充值）、`PAY_FEE`（缴费）、`TOPUP_CARD`（给水电卡充值）
- `balance_after` 字段记录交易后的余额快照，便于对账
- 交易记录**只插入不修改不删除**，保证了账务的可追溯性（审计要求）

---

### 6.2 根据钱包ID查询流水（按时间倒序）
```sql
SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY trans_time DESC
```
**说明**：业主查看交易历史明细。最新的交易记录排在最前面。

**答辩要点**：`ORDER BY trans_time DESC` 使用了时间倒序，符合用户查看"最近消费"的习惯。

---

### 6.3 根据流水ID查询
```sql
SELECT * FROM wallet_transactions WHERE trans_id = ?
```
**说明**：查询单笔交易详情。

---

## 📊 SQL 语句统计总览

| DAO 类                  | SQL 语句数量 | 核心业务SQL                                    |
| ----------------------- | ------------ | ---------------------------------------------- |
| **UserDAO**             | 9            | 登录验证、模糊搜索                             |
| **FeeDAO**              | 12           | 欠费查询、收入统计、楼栋排名（联表查询）       |
| **PropertyDAO**         | 7            | 房产唯一性校验                                 |
| **UserWalletDAO**       | 4            | 钱包余额更新（需配合事务）                     |
| **UtilityCardDAO**      | 6            | 水电卡充值                                     |
| **WalletTransactionDAO**| 3            | 交易流水记录（不可修改删除，保证审计可追溯性） |
| **总计**                | **41 条**    | -                                              |

---

## 🎯 答辩高频问题预判

### Q1：为什么使用 PreparedStatement 而不是拼接 SQL？
**A**：PreparedStatement 使用 `?` 占位符，所有参数都经过预编译和绑定，即使用户输入 `' OR 1=1 --` 这样的恶意代码，也会被当作普通字符串处理，**从根本上防止了 SQL 注入攻击**。

---

### Q2：你的项目中哪个 SQL 最复杂？
**A**：`FeeDAO` 中的**欠费楼栋 TOP 5 统计查询**，它使用了：
- 多表 JOIN（fees + properties）
- WHERE 条件过滤
- GROUP BY 分组聚合
- SUM() 和 COUNT() 聚合函数
- ORDER BY 排序
- LIMIT 分页截取

这个查询综合展示了对 SQL 高级特性的掌握。

---

### Q3：如何保证钱包扣款的并发安全？
**A**：在 Service 层使用了 Spring 的 `@Transactional` 注解，保证了：
1. 查询钱包余额
2. 扣除余额
3. 更新钱包
4. 插入交易流水

这四个操作要么全部成功，要么全部回滚，**保证了数据的原子性和一致性**。

---

### Q4：为什么交易记录不允许修改和删除？
**A**：这是金融级账务系统的标准做法。交易流水一旦生成，就不能篡改，这样可以：
- 保证审计的可追溯性
- 防止内部作弊或误操作
- 符合财务合规要求

如果发生错误，应该通过"冲正交易"（插入一条反向记录）来纠正，而不是直接修改或删除原记录。

---

## ✅ 总结

本系统共使用 **41 条预注入SQL语句**，涵盖了：
- ✅ 基础 CRUD 操作（增删改查）
- ✅ 条件查询与模糊搜索
- ✅ 多表联合查询（JOIN）
- ✅ 分组聚合统计（GROUP BY + SUM/COUNT）
- ✅ 排序与分页（ORDER BY + LIMIT）

所有SQL均采用 **参数化查询**，有效防止了 SQL 注入攻击，符合企业级开发规范。
