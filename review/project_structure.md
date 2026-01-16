# 项目结构与架构设计 (Project Structure & Architecture)

为了在答辩中清晰地说明你的项目，我们采用了标准的 **Spring Boot 三层架构**。你可以用“餐厅模型”来向评委解释各层的功能：

## 1. 核心架构分层 (The Restaurant Model)

| 目录/包名 | 角色 (Role) | 餐厅比喻 | 详细职责 |
| :--- | :--- | :--- | :--- |
| `controller` | **控制层** | **服务员** | 负责接待前端请求（点菜），调用 Service 处理逻辑，最后把结果返回给前端（上菜）。使用 `@RestController` 注解。 |
| `service` | **业务逻辑层** | **后厨/厨师** | 核心逻辑所在地。负责对数据进行加工、计算（炒菜）。它不直接碰数据库，而是通过 DAO 拿数据。使用 `@Service` 注解。 |
| `dao` (or Mapper) | **数据访问层** | **仓库管理员** | 专门负责和数据库打交道（进仓取货）。执行 SQL 语句。在这个项目里我们用了 `JdbcTemplate`。使用 `@Repository` 注解。 |
| `entity` | **实体层** | **食材/菜品** | 对应数据库里的表。每一行数据就是一个 Entity 对象。 |
| `dto` | **数据传输对象** | **菜单/托盘** | 专门用于前后端传输数据，可能只包含 Entity 的部分字段，保护数据库结构不直接暴露。 |

---

## 2. 核心组件说明

### 🚀 启动类: `PropertyManagementApplication.java`
项目的入口，带有 `@SpringBootApplication` 注解。它负责启动 Spring 容器，开启**自动装配 (Auto-Configuration)**，扫描所有带注解的类。

### ⚙️ 配置类: `config` 包
存放数据库连接属性、跨域配置 (CORS) 或权限校验拦截器的配置。

### 🛠️ 工具类: `util` 包
存放通用的工具方法，比如密码加密、JWT 生成、时间格式化等。

---

## 3. 请求流转全过程 (Request Flow)
当用户在网页点击“登录”按钮时：
1. **Frontend**: 发送 HTTP 请求到 `UserController`。
2. **Controller**: 接收请求，校验参数，调用 `UserService.login()`。
3. **Service**: 处理业务逻辑（比如判断账号密码是否匹配），调用 `UserDAO.findByUserName()`。
4. **DAO**: 执行 `SELECT * FROM users...`，利用 `RowMapper` 将数据库结果转为 `User` 对象。
5. **Backtrack**: 数据逐层返回，最终由 `Controller` 返回 JSON 结果给前端。

---

## 4. 答辩高频考点 (Quiz Strategy)

> [!TIP]
> **评委问：为什么不直接在 Controller 里写 SQL 语句？**
> **回答：** 为了实现 **解耦 (Decoupling)**。Controller 只负责交互，Service 负责逻辑，DAO 负责数据。这样如果以后数据库从 MySQL 换成 MongoDB，我只需要修改 DAO 层，而业务逻辑和接口层不需要动，极大地提高了代码的可维护性。

> [!NOTE]
> **关于 `@Autowired` vs 构造器注入：**
> 该项目中的 DAO 采用了 **构造器注入**。它是 Spring 官方推荐的，因为它可以保证注入的对象是 `final` 的（不可变的），并且能防止循环依赖。
