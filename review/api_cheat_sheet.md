# 🧭 项目核心方法速查表 (Method Cheat Sheet)

这份表格帮你快速定位代码。当老师问“**充值功能在哪？**”或“**这个接口调用了哪个Service？**”时，看这里！

---

## 1. 👤 用户与认证模块 (User & Auth)
**核心职责**：负责登录、RBAC 权限控制、用户信息管理。

| 层级 | 类名/方法名 | 功能描述 | 关键点 |
| :--- | :--- | :--- | :--- |
| **Controller** | `AuthController.login()` | **用户登录** | 校验用户名密码，**写入 Session** |
| | `AuthController.logout()` | **用户登出** | `session.invalidate()` 销毁会话 |
| **DAO** | `UserDAO.findByUserNameAndPassword()` | **查库校验** | 执行 SQL 查询用户是否存在 |
| **Controller** | `UserOwnerController` | **业主管理** | **RESTful** 风格的增删改查 |
| | `.createOwner()` | **新增业主** | 接收 JSON，插入 User 表 |
| | `.updateOwner()` | **修改信息** | 修改业主手机号、姓名等 |

---

## 2. 💰 费用账单模块 (Fee)
**核心职责**：生成账单、查询欠费、统计报表。

| 层级 | 类名/方法名 | 功能描述 | 关键点 |
| :--- | :--- | :--- | :--- |
| **Controller** | `FeeController.createFee()` | **单笔计费** | 下发一张新账单 |
| | `FeeController.batchCreate()` | **批量计费** | 循环给多个房产下发账单 |
| | `FeeController.getArrearsList()` | **欠费查询** | **角色判断**：管理员看所有，业主看自己 |
| **Service** | `FeeServiceImpl.checkArrears()` | **欠费检查** | 查该房产有无未缴费记录 (通用) |
| | `FeeServiceImpl.checkWalletArrears()` | **软拦截** | **风控核心**：只查物业/取暖费欠费 |
| **DAO** | `FeeDAO.getArrearsByBuilding()` | **欠费统计** | **复杂SQL**：聚合统计欠费严重的楼栋 |

---

## 3. 💳 电子钱包模块 (Wallet)
**核心职责**：余额管理、在线支付、转账记录。

| 层级 | 类名/方法名 | 功能描述 | 关键点 |
| :--- | :--- | :--- | :--- |
| **Controller** | `WalletController.recharge()` | **钱包充值** | 模拟支付宝/微信充值 |
| | `WalletController.payFee()` | **余额缴费** | 用钱包余额去交物业费 |
| **Service** | `WalletServiceImpl.payFeeFromWallet()` | **支付逻辑** | **事务(Transactional)**：扣款+改账单状态 |
| | `WalletServiceImpl.topUpCardFromWallet()` | **转账水卡** | 从钱包扣钱 -> 充入水电卡 |
| **DAO** | `WalletTransactionDAO.insert()` | **记流水** | 每一笔钱的变动都必须记录日志 |

---

## 4. ⚡️ 水电卡服务 (Utility Card)
**核心职责**：水卡/电卡充值、余额查询、**欠费熔断**。

| 层级 | 类名/方法名 | 功能描述 | 关键点 |
| :--- | :--- | :--- | :--- |
| **Controller** | `UtilityCardController.topUp()` | **水电充值** | **入口**：前端点击充值时调用这里 |
| **Service** | `UtilityCardServiceImpl.topUp()` | **业务逻辑** | **🔥 核心亮点**：先调 `checkArrears` 查欠费，有欠费则抛异常 |
| | `.payFeeFromCard()` | **卡内扣费** | 用水电卡余额去结清账单 |
| **DAO** | `UtilityCardDAO.update()` | **更新余额** | 最终执行 SQL 修改卡内余额 |

---

## 🚀 答辩快速索引流程

### 场景一：老师问“用户怎么登录的？”
👉 **路线**：`AuthController.login` (Controller) -> `UserDAO.findByUserNameAndPassword` (DAO)
> **话术**：“前端发 POST 请求给 AuthController，Controller 调用 DAO 层去数据库查是否有匹配的账号密码，如果有，就把用户存到 Session 里。”

### 场景二：老师问“如果我想给业主批量发账单怎么做？”
👉 **路线**：`FeeController.batchCreate` (Controller) -> `FeeServiceImpl.batchCreateFees` (Service)
> **话术**：“我在 Service 层写了一个循环，遍历选中的房产 ID，复用 `createFee` 方法逐条插入数据库，整个过程是在一个事务里完成的。”

### 场景三：老师问“你说的‘欠费拦截’代码在哪？”
👉 **路线**：`UtilityCardServiceImpl.topUp` (Service)
> **话术**：“在 Service 层的 `topUp` 方法里。您看这里（指代码），我先调用 `feeService.checkWalletArrears()`，如果返回 true，我直接抛出异常，中断充值。”
