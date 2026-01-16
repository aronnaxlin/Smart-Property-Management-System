# 🎓 复杂 SQL 解析与动态查询答辩指南

针对你提到的 **"在 `findByCondition` 中如何实现多条件组合查询"** 这个问题，我们需要先明确一个事实：

> **⚠️ 事实核查**：
> 你的项目中 **并没有** 使用 `StringBuilder` 拼接 SQL 的复杂动态查询（Dynamic SQL）。
> 你使用的是 `JdbcTemplate` 配合 **静态 SQL**。唯一接近的是 `UserDAO` 中的模糊搜索。

但这不代表你不能回答！如果老师问到了，通常是考察你的**知识面**。

---

## 🚀 第一部分：如何回答“动态 SQL”问题？（防御性回答）

**Q: "如果我要实现一个功能，既能查姓名，又能查手机号，还能查房号，条件是可选的，你怎么写 SQL？"**

> **🗣️ 标准满分回答**：
> "在我的当前项目中，为了保持代码简洁，我主要使用了固定 SQL。但如果需要实现这种多条件动态组合查询，我会使用 `StringBuilder` 来动态构建 SQL 语句。
>
> **具体逻辑是**：
> 1.  **初始化 SQL**：先写一个 `SELECT * FROM table WHERE 1=1`。（*解释：`1=1` 是个技巧，方便后面直接追加 `AND` 条件，不用判断是否是第一个条件*）
> 2.  **判空追加**：
>     *   如果 `name` 参数不为空，就 `sql.append(" AND name = ?")`，并把参数加到参数列表中。
>     *   如果 `phone` 参数不为空，就 `sql.append(" AND phone = ?")`。
> 3.  **执行查询**：最后用 ` jdbcTemplate.query(sql.toString(), params.toArray(), rowMapper)` 执行。
>
> 这种方式既灵活又能通过 `PreparedStatement` 防止 SQL 注入。"

---

## 🔍 第二部分：你项目中原本就有的 "复杂 SQL" 解析

虽然没有动态 SQL，但你的 **`FeeDAO`** 中有几个非常有技术含量的 **聚合统计 SQL**，这通常是答辩的加分项。

### 1️⃣ 欠费楼栋统计 (最复杂的 SQL)
**文件**：`FeeDAO.java` -> `getArrearsByBuilding()`

```sql
SELECT
    p.building_no,
    COUNT(*) as unpaid_count,
    SUM(f.amount) as unpaid_amount
FROM fees f
JOIN properties p ON f.p_id = p.p_id
WHERE f.is_paid = 0
GROUP BY p.building_no
ORDER BY unpaid_count DESC
LIMIT 5
```

**💡 答辩解析话术**：
> "这个查询用于在大屏上展示‘欠费重灾区’。
> 1.  **`JOIN` (联表)**：我把 `fees` (账单表) 和 `properties` (房产表) 连接起来，因为账单里只有房产ID，而我需要通过房产表拿到具体的 `building_no` (楼号)。
> 2.  **`WHERE` (筛选)**：只筛选 `is_paid = 0` 的未缴费记录。
> 3.  **`GROUP BY` (分组)**：按楼号分组，这样就能算出每一栋楼总共有多少欠费。
> 4.  **聚合函数**：用 `COUNT(*)` 算欠费笔数，用 `SUM(amount)` 算欠费总金额。
> 5.  **`ORDER BY` & `LIMIT`**：最后按欠费笔数倒序排列，取前 5 名。这能帮助物业快速定位哪栋楼最难管理。"

---

### 2️⃣ 收入分布统计
**文件**：`FeeDAO.java` -> `getIncomeDistribution()`

```sql
SELECT
    fee_type,
    SUM(amount) as total_amount
FROM fees
WHERE is_paid = 1
GROUP BY fee_type
```

**💡 答辩解析话术**：
> "这个查询用于生成财务饼图。
> 核心是 **`GROUP BY fee_type`**。它会把所有已缴费的记录按‘物业费’、‘水费’、‘电费’分类，然后用 `SUM()` 算出每种费用的总收入。数据直接传给前端 ECharts 渲染成饼图。"

---

### 3️⃣ 模糊查询 (最接近动态查询的 SQL)
**文件**：`UserDAO.java` -> `searchByKeyword()`

```sql
SELECT * FROM users WHERE name LIKE ? OR phone LIKE ?
```

**💡 答辩解析话术**：
> "这是后台搜索框的实现逻辑。
> 我使用了 SQL 的 **`LIKE` 操作符** 配合 **`OR` 逻辑**。
> 参数在 Java 层处理成 `%关键字%` 的形式传入。这样用户无论是输入姓名的片段，还是手机号的片段，都能把人找出来。
> 这里利用了数据库的模糊匹配能力，提高了用户体验。"

---

## ⚡️ 总结

如果老师让你展示“复杂的数据库操作”，**不要犹豫，直接展示 `FeeDAO` 中的 `getArrearsByBuilding` 方法**。即便它没有用到动态拼接，但它涵盖了 **联表、过滤、分组、聚合、排序、分页** 六大 SQL 核心要素，足以证明你的数据库水平！
