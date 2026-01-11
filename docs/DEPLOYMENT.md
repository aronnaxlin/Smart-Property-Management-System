# 📦 部署指南

## 快速部署

### 方式一：使用启动脚本（推荐）

#### Linux / macOS
```bash
./start.sh
```

#### Windows
双击 `start.bat` 或在命令提示符中运行：
```cmd
start.bat
```

---

## 完整打包部署

### 1. 构建可执行 JAR

```bash
mvn clean package -DskipTests
```

构建成功后，会在 `target/` 目录生成：
```
target/
└── property-management-system-1.0-SNAPSHOT.jar  (可执行 JAR，约 50MB)
```

### 2. 准备发布包

创建一个发布目录，包含以下文件：

```
SmartPropertySystem/
├── property-management-system-1.0-SNAPSHOT.jar  # 主程序
├── start.sh                                     # Linux/macOS 启动脚本
├── start.bat                                    # Windows 启动脚本
├── .env_template                                # 环境变量模板
├── application.properties                       # 配置文件（可选）
└── README.md                                    # 使用说明
```

### 3. 配置数据库

在目标服务器上：

#### 3.1 导入数据库
```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p < sql/data.sql
```

#### 3.2 修改配置

编辑 `application.properties`（或通过环境变量）：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/property_management?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=propertyAdmin
spring.datasource.password=your_password
```

### 4. 配置 AI 服务（可选）

复制 `.env_template` 为 `.env`：

```bash
cp .env_template .env
```

编辑 `.env` 填写 API 密钥：

```env
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_API_KEY=sk-your-api-key-here
AI_MODEL=qwen-plus
```

### 5. 启动应用

#### 方式 A：使用脚本（推荐）

```bash
# Linux/macOS
./start.sh

# Windows
start.bat
```

#### 方式 B：直接运行 JAR

```bash
java -jar property-management-system-1.0-SNAPSHOT.jar
```

#### 方式 C：后台运行（Linux 生产环境）

```bash
nohup java -jar property-management-system-1.0-SNAPSHOT.jar > app.log 2>&1 &
```

查看运行状态：
```bash
# 查看日志
tail -f app.log

# 查看进程
ps aux | grep property-management

# 停止应用
kill $(pgrep -f property-management-system)
```

### 6. 访问系统

- **地址**: http://localhost:8081
- **默认账号**:
  - 管理员: `admin` / `123456`
  - 业主: `owner_1` / `123456`

---

## 故障排查

### 端口已被占用

```bash
# 查看占用 8081 端口的进程
lsof -i :8081  # macOS/Linux
netstat -ano | findstr :8081  # Windows

# 更换端口
java -jar app.jar --server.port=8082
```

### 内存不足

```bash
# 减小内存使用
java -Xms128m -Xmx256m -jar app.jar
```

### 数据库连接失败

检查：
1. MySQL 服务是否运行
2. 用户名密码是否正确
3. 数据库是否已创建
4. 防火墙是否允许 3306 端口

---

## 支持

如遇问题，请参考：
- [项目 README](README.md)
- [GitHub Issues](https://github.com/aronnaxlin/NUC-Java-Database-Course-Design/issues)
- Email: lilinhan917@gmail.com
