# 2026年 NUC Java数据库课程设计 - 模拟试题

> 生成日期: 2026-01-16
> 来源: 项目代码深度解析

## 简答题

### 1. 数据库与实体变更
**题目：** 如果要给系统中的 `User` 实体（及数据库表 `users`）添加一个“电子邮箱”（`email`）字段，除了修改 `User.java` 实体类外，你还需要修改 `UserDAO.java` 中的哪两个核心方法？请具体说明修改内容。

### 2. 认证与安全机制
**题目：** 在 `AuthController.java` 的 `login` 方法中，你设计了“四阶段登录验证”流程。请简述这四个阶段分别是什么？特别地，代码中提到当前密码验证存在安全隐患，注释中建议的“安全演进方案”是什么？

### 3. 业务风控与逻辑冲突
**题目：** 在 `WalletServiceImpl.java` 的 `topUpCardFromWallet`（钱包转水电卡）方法中，你实现了一个关键的“风控拦截规则”（欠费静默锁定）。请描述该规则的逻辑：系统在允许转账前会检查什么条件？如果触发该条件会发生什么？

### 4. 级联创建与数据完整性
**题目：** 在 `PropertyController.java` 的 `createProperty` 方法中，当一个新的房产被成功创建后，系统会自动执行什么额外的数据库操作以保证业务闭环？此外，你是如何防止“同一个房间被重复创建”的？

### 5. 支付流程与权限控制
**题目：** 在 `WalletServiceImpl.java` 的 `payFeeFromWallet`（余额缴费）方法中，你对 `ADMIN`（管理员）角色施加了特殊的支付限制。请描述这个限制是什么（管理员不能支付哪类费用）？并简述一次成功的钱包缴费涉及的数据库更新操作有哪些？

---

## 参考答案与代码出处

### 1. 答案
- **需要修改 `userRowMapper`**：在映射结果集时，增加 `user.setEmail(rs.getString("email"));`。
- **需要修改 `insert` (或 `update`) 方法**：在 SQL 语句中增加 `email` 字段，并在 `PreparedStatement` 参数设置中加入 `user.getEmail()`。
- *(出处：`UserDAO.java`)*

### 2. 答案
- **四个阶段：**
    1. 输入完整性守卫（非空检查）；
    2. 参数规范拦截（长度检查）；
    3. 数据库凭据比对；
    4. Session 会话状态持久化。
- **安全演进：** 当前使用明文比对，注释建议生产环境应采用 **BCrypt 强盐哈希算法**。
- *(出处：`AuthController.java`)*

### 3. 答案
- **逻辑：** 系统会遍历该用户名为下的所有房产，检查是否存在 **物业费或取暖费欠费** (`feeService.checkWalletArrears`)。
- **结果：** 如果存在欠费，系统会抛出 `IllegalStateException`，提示“检测到逾期未缴单项，已锁定水电卡充值通道”，从而强制用户先缴纳物业费。
- *(出处：`WalletServiceImpl.java`)*

### 4. 答案
- **自动操作：** 系统会自动为该房产创建一张 **水卡** (`WATER`) 和一张 **电卡** (`ELECTRICITY`)，初始余额为 0。
- **防重机制：** 在插入前，通过 `propertyDAO.findByRoomInfo(楼栋, 单元, 房号)` 查询是否存在记录，若存在则返回错误。
- *(出处：`PropertyController.java`)*

### 5. 答案
- **管理员限制：** 管理员 **不能代缴水费和电费** (`WATER_FEE`, `ELECTRICITY_FEE`)，必须由业主自行充值水电卡使用。
- **数据库更新：**
    1. 扣减钱包余额 (`UserWallet`)；
    2. 更新账单状态为“已支付” (`Fee`)；
    3. 插入一条交易流水记录 (`WalletTransaction`)。
- *(出处：`WalletServiceImpl.java`)*
