# 🏢 智慧物业管理系统 (Smart Property Management System)

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> 一个基于 **Spring Boot + MySQL** 的现代化物业管理系统，集成 **AI 智能助手**，提供业主档案管理、费用收缴、数据可视化看板等核心功能。

---

## ✨ 核心特性

### 🔐 用户管理与权限控制
- **角色分离**：管理员 (ADMIN) 和业主 (OWNER) 两种角色
- **基于角色的访问控制 (RBAC)**：不同角色看到不同的功能模块
- **会话管理**：安全的登录/登出机制

### 👥 业主与房产管理
- **业主档案管理**：支持增删改查 (CRUD) 操作
- **多维检索**：根据姓名、房号、手机号等条件组合查询
- **房产资源管理**：楼栋、单元、房间状态维护（已售/待售/出租）

### 💰 费用管理模块
- **多种费用类型**：物业费、取暖费等
- **批量计费**：支持单笔或批量费用生成
- **欠费分析**：自动筛选逾期未缴纳用户，生成催缴名单
- **业务逻辑硬拦截**：欠费用户无法购买水电卡，直至清偿欠款

### 💳 智慧钱包与水电服务
- **用户钱包系统**：余额管理、充值、交易记录
- **水电卡管理**：模拟水电卡充值流程
- **欠费锁定机制**：通过 `checkArrears()` 校验确保缴费后才能使用服务

### 🤖 AI 智能助手
- **角色定制化服务**：
  - **业主视角**：查询个人费用、房产信息、缴费引导
  - **管理员视角**：数据分析、欠费告警、业务决策支持
- **自然语言交互**：支持文本对话，提供业务指引
- **Markdown 渲染**：AI 回复支持富文本格式显示
- **API 集成**：兼容 OpenAI、通义千问等大模型服务

### 📊 数据可视化看板
- **财务概况**：年度收费率、收入占比、欠费分析
- **图表展示**：使用 ECharts 动态渲染柱状图、饼图
- **实时告警**：高亮显示欠费比例最高的楼栋

---

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2.1
- **数据库**: MySQL 8.0+
- **持久化**: Spring JDBC Template
- **AI 服务**: OpenAI Java SDK / 通义千问 API
- **环境配置**: Dotenv (`.env` 文件管理)

### 前端
- **基础**: HTML5 + CSS3 + Vanilla JavaScript
- **图表库**: ECharts 5.x
- **Markdown 渲染**: Marked.js

### 构建工具
- Maven 3.8+
- Java 21

---

## 📁 项目结构

```
NUC-Java-Database-Course-Design/
├── docs/                          # 项目文档
│   ├── AI-Integration-Guide.md   # AI 集成指南
│   ├── 需求分析.md                # 需求文档
│   └── ...
├── sql/                           # 数据库脚本
│   ├── schema.sql                 # 数据库结构
│   ├── data.sql                   # 模拟数据 (~100 业主)
│   └── generate_mock_data.py      # 数据生成脚本
├── src/
│   ├── main/
│   │   ├── java/site/aronnax/
│   │   │   ├── controller/        # REST API 控制器
│   │   │   ├── service/           # 业务逻辑层
│   │   │   ├── dao/               # 数据访问层
│   │   │   ├── entity/            # 实体类
│   │   │   └── config/            # 配置类
│   │   └── resources/
│   │       ├── static/            # 前端资源
│   │       │   ├── css/           # 样式文件
│   │       │   ├── js/            # JavaScript 脚本
│   │       │   └── views/         # HTML 页面
│   │       └── application.properties  # Spring 配置
│   └── test/                      # 单元测试
├── .env_template                  # 环境变量模板
├── pom.xml                        # Maven 配置
└── README.md                      # 项目说明
```

---

## 🚀 快速开始

### 1. 环境准备

确保已安装以下工具：

- **Java 21+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Git**

### 2. 克隆项目

```bash
git clone https://github.com/aronnaxlin/NUC-Java-Database-Course-Design.git
cd NUC-Java-Database-Course-Design
```

### 3. 配置数据库

#### 3.1 创建数据库

```sql
CREATE DATABASE property_management CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'propertyAdmin'@'%' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON property_management.* TO 'propertyAdmin'@'%';
FLUSH PRIVILEGES;
```

#### 3.2 导入数据

```bash
mysql -u propertyAdmin -p property_management < sql/schema.sql
mysql -u propertyAdmin -p property_management < sql/data.sql
```

### 4. 配置应用

#### 4.1 数据库连接

编辑 `src/main/resources/application.properties`：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/property_management?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8&allowPublicKeyRetrieval=true
spring.datasource.username=propertyAdmin
spring.datasource.password=your_password
```

#### 4.2 AI 服务配置（可选）

复制 `.env_template` 为 `.env` 并填写 API 密钥：

```bash
cp .env_template .env
```

编辑 `.env`：

```env
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_API_KEY=your_api_key_here
AI_MODEL=qwen-plus
```

> 💡 **提示**：支持 OpenAI、通义千问、DeepSeek 等兼容 OpenAI SDK 的 API

### 5. 运行项目

```bash
mvn spring-boot:run
```

应用将在 `http://localhost:8081` 启动。

### 6. 访问系统

- **登录页面**: http://localhost:8081/login.html
- **默认账户**:
  - 管理员: `admin` / `123456`
  - 业主: `owner_1` / `123456`

---

## 📊 数据库设计

### 核心表结构

| 表名               | 说明                     | 关键字段                          |
|--------------------|--------------------------|-----------------------------------|
| `users`            | 用户与业主档案表         | `user_id`, `user_name`, `user_type`, `gender`, `phone` |
| `properties`       | 房产资源表               | `p_id`, `building_no`, `unit_no`, `room_no`, `user_id` |
| `fees`             | 费用账单表               | `f_id`, `p_id`, `fee_type`, `amount`, `is_paid` |
| `utility_cards`    | 水电卡表                 | `card_id`, `p_id`, `card_type`, `balance` |
| `user_wallets`     | 用户钱包表               | `wallet_id`, `user_id`, `balance` |
| `wallet_transactions` | 钱包交易记录表        | `txn_id`, `wallet_id`, `txn_type`, `amount` |

详细 ER 图请参考 [`sql/schema.sql`](sql/schema.sql)。

---

## 🎯 主要功能模块

### 1️⃣ 用户管理 (`/api/users`)

| 接口                | 方法   | 说明           |
|---------------------|--------|----------------|
| `/api/users`        | GET    | 获取用户列表   |
| `/api/users/{id}`   | GET    | 获取用户详情   |
| `/api/users`        | POST   | 创建新用户     |
| `/api/users/{id}`   | PUT    | 更新用户信息   |
| `/api/users/{id}`   | DELETE | 删除用户       |

### 2️⃣ 费用管理 (`/api/fees`)

| 接口                | 方法   | 说明               |
|---------------------|--------|--------------------|
| `/api/fees`         | GET    | 查询费用列表       |
| `/api/fees`         | POST   | 新增费用           |
| `/api/fees/{id}`    | PUT    | 更新费用状态       |
| `/api/fees/arrears` | GET    | 查询欠费记录       |

### 3️⃣ 水电卡服务 (`/api/utility`)

| 接口                     | 方法   | 说明                 |
|--------------------------|--------|----------------------|
| `/api/utility/cards`     | GET    | 查询水电卡列表       |
| `/api/utility/topup`     | POST   | 充值水电卡（含欠费拦截） |

### 4️⃣ AI 助手 (`/api/ai`)

| 接口               | 方法   | 说明                   |
|--------------------|--------|------------------------|
| `/api/ai/chat`     | POST   | 发送消息到 AI 助手     |

### 5️⃣ 数据看板 (`/api/dashboard`)

| 接口                   | 方法   | 说明                 |
|------------------------|--------|----------------------|
| `/api/dashboard/stats` | GET    | 获取财务统计数据     |

---

## 🧪 测试数据

项目自带 **100+ 业主** 和 **120+ 房产** 的模拟数据，覆盖以下场景：

- ✅ 正常缴费用户
- ⚠️ 欠费用户（约 30%）
- 🏡 多房产业主
- 💳 低余额钱包
- ⚡ 不同水电卡余额

**生成新数据**：

```bash
cd sql
python3 generate_mock_data.py
mysql -u propertyAdmin -p property_management < data.sql
```

---

## 📖 详细文档

- [AI 集成指南](docs/AI-Integration-Guide.md) - AI 助手配置与使用
- [需求分析文档](docs/需求分析.md) - 项目背景与业务需求
- [用户钱包指南](docs/user_wallet_guide.md) - 钱包系统说明

---

## 🔧 常见问题

### Q1: 登录后提示"无法连接到服务器"？

**A**: 检查数据库连接配置和 MySQL 服务状态：

```bash
# 检查 MySQL 是否运行
sudo systemctl status mysql  # Linux
brew services list           # macOS

# 测试连接
mysql -u propertyAdmin -p -h localhost property_management
```

### Q2: AI 助手不可用？

**A**: 确认 `.env` 文件已正确配置，且 API 密钥有效：

```bash
# 检查环境变量
cat .env

# 测试 API 连通性 (以通义千问为例)
curl -H "Authorization: Bearer YOUR_API_KEY" \
     https://dashscope.aliyuncs.com/compatible-mode/v1/models
```

### Q3: Maven 构建失败？

**A**: 清理并重新构建：

```bash
mvn clean install -DskipTests
```

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建 Pull Request

---

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

---

## 👨‍💻 作者

**Aronnax (Li Linhan)**

- GitHub: [@aronnaxlin](https://github.com/aronnaxlin)
- Email: lilinhan917@gmail.com

