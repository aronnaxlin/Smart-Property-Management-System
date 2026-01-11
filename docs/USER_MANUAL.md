# 智慧物业管理系统 - 使用说明

## 系统要求

- **Java**: 21 或更高版本
- **操作系统**: Windows / macOS / Linux
- **内存**: 最低 512MB，推荐 1GB+

## 快速启动

### Windows 用户
1. 双击 `start.bat`
2. 等待应用启动完成
3. 浏览器访问 http://localhost:8081

### macOS / Linux 用户
1. 打开终端，进入程序目录
2. 运行命令: `./start.sh`
3. 浏览器访问 http://localhost:8081

## 默认账号

### 管理员账号
- 用户名: `admin`
- 密码: `123456`

### 业主账号（测试）
- 用户名: `owner_1` 到 `owner_100`
- 密码: `123456`

## 主要功能

### 管理员功能
- 📊 数据看板：查看收费统计、欠费分析
- 👥 用户管理：管理业主信息
- 🏠 房产管理：维护房产资源
- 💰 费用管理：收费、催缴、账单查询
- 💳 水电卡管理：充值、余额查询
- 🤖 AI 助手：业务决策支持

### 业主功能
- 💳 我的钱包：余额查询、充值
- ⚡ 水电服务：水电卡充值
- 🤖 AI 助手：费用查询、缴费引导

## 常见问题

### Q: 打开 start.bat 闪退怎么办？
A: 可能是 Java 未安装或版本过低。请安装 Java 21：
https://www.oracle.com/java/technologies/downloads/

### Q: 提示数据库连接失败？
A: 请确保 MySQL 服务正在运行，且数据库已正确导入。

### Q: AI 助手不可用？
A: AI 助手需要配置 API 密钥。如不需要此功能，可正常使用其他模块。

### Q: 如何修改端口？
A: 编辑 `application.properties` 文件中的 `server.port` 配置。

## 停止应用

按 `Ctrl + C` 即可停止应用。

## 技术支持

- GitHub: https://github.com/aronnaxlin/NUC-Java-Database-Course-Design
- Email: lilinhan917@gmail.com

---

**版本**: 1.0-SNAPSHOT
**开发者**: Aronnax (Li Linhan)
