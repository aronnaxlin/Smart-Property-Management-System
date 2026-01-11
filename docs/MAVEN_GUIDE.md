# Maven 命令参考手册

## 📋 目录

1. [基础命令](#基础命令)
2. [测试相关](#测试相关)
3. [启动应用](#启动应用)
4. [打包发布](#打包发布)
5. [清理与维护](#清理与维护)
6. [故障排查](#故障排查)
7. [高级用法](#高级用法)

---

## 基础命令

### 1. 编译项目

```bash
mvn compile
```

**说明**: 编译 `src/main/java` 下的源代码到 `target/classes`

**何时使用**:
- 修改 Java 代码后验证编译是否通过
- 检查语法错误

---

### 2. 清理编译结果

```bash
mvn clean
```

**说明**: 删除 `target/` 目录及其所有内容

**何时使用**:
- 遇到奇怪的编译错误时
- 打包前确保环境干净
- 磁盘空间不足时

---

## 测试相关

### 1. 运行所有测试

```bash
mvn test
```

**说明**:
- 编译测试代码（`src/test/java`）
- 执行所有单元测试
- 生成测试报告到 `target/surefire-reports/`

**输出示例**:
```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
```

---

### 2. 跳过测试

```bash
# 方式 1: 完全跳过测试编译和执行
mvn install -DskipTests

# 方式 2: 跳过测试但仍然编译测试代码
mvn install -Dmaven.test.skip=true
```

**何时使用**:
- 快速打包时
- 测试暂时失败但需要先部署
- CI/CD 管道中的特定阶段

**区别**:
- `-DskipTests`: 编译测试，但不执行
- `-Dmaven.test.skip=true`: 不编译也不执行测试

---

### 3. 运行单个测试类

```bash
mvn test -Dtest=PropertyManagementApplicationTests
```

**说明**: 只运行指定的测试类

---

### 4. 运行测试方法

```bash
mvn test -Dtest=PropertyManagementApplicationTests#checkUsers
```

**说明**: 只运行 `PropertyManagementApplicationTests` 类中的 `checkUsers` 方法

---

## 启动应用

### 1. 标准启动（开发模式）

```bash
mvn spring-boot:run
```

**说明**:
- 自动编译代码
- 启动 Spring Boot 应用
- 支持热重载（需要 Spring DevTools）

**何时使用**:
- 日常开发调试
- 本地测试功能

**特点**:
- ✅ 自动检测代码更改
- ✅ 无需手动重启
- ❌ 启动较慢

---

### 2. 跳过测试启动

```bash
mvn spring-boot:run -DskipTests
```

**说明**: 启动应用但跳过测试阶段，加快启动速度

---

### 3. 指定端口启动

```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

**说明**: 在 8082 端口启动应用（默认 8081）

---

### 4. 后台运行（不推荐用于开发）

```bash
nohup mvn spring-boot:run > app.log 2>&1 &
```

**说明**: 后台运行，日志输出到 `app.log`

**查看日志**:
```bash
tail -f app.log
```

**停止应用**:
```bash
# 查找进程
ps aux | grep spring-boot

# 杀死进程
kill -9 <PID>
```

---

### 5. 中断运行中的应用

#### 方式 1: 优雅停止（推荐）

```bash
# 在运行 mvn spring-boot:run 的终端中按：
Ctrl + C
```

这会发送 `SIGINT` 信号，Spring Boot 会执行优雅关闭。

#### 方式 2: 强制停止

```bash
# macOS/Linux
pkill -9 -f spring-boot

# 或者找到进程 ID
ps aux | grep property-management
kill -9 <PID>
```

```cmd
REM Windows
taskkill /F /IM java.exe
```

⚠️ **注意**: 强制停止可能导致数据未保存

---

## 打包发布

### 1. 完整打包

```bash
mvn clean package
```

**说明**:
1. 清理 `target/` 目录
2. 编译源代码
3. 运行测试
4. 打包成 JAR 文件

**输出**: `target/property-management-system-1.0-SNAPSHOT.jar`

**文件大小**: 约 50MB（包含所有依赖）

---

### 2. 跳过测试打包（快速）

```bash
mvn clean package -DskipTests
```

**说明**: 打包但不运行测试，适合快速构建

**何时使用**:
- 本地快速验证打包
- CI/CD 管道的特定阶段
- 测试已在其他步骤完成

---

### 3. 打包并安装到本地仓库

```bash
mvn clean install
```

**说明**:
- 打包项目
- 安装到本地 Maven 仓库 (`~/.m2/repository/`)
- 其他项目可以作为依赖引用

---

### 4. 运行打包后的 JAR

```bash
java -jar target/property-management-system-1.0-SNAPSHOT.jar
```

**优点**:
- ✅ 启动速度快
- ✅ 生产环境推荐方式
- ✅ 独立运行，无需 Maven

---

### 5. 查看打包内容

```bash
# 查看 JAR 包结构
jar tf target/property-management-system-1.0-SNAPSHOT.jar

# 解压查看（不推荐）
unzip target/property-management-system-1.0-SNAPSHOT.jar -d jar-content/
```

---

## 清理与维护

### 1. 清理所有构建产物

```bash
mvn clean
```

**删除**: `target/` 目录

---

### 2. 深度清理

```bash
# 清理并删除本地仓库缓存
mvn dependency:purge-local-repository
```

**何时使用**:
- 依赖损坏
- 版本冲突

---

### 3. 更新依赖

```bash
# 检查可更新的依赖
mvn versions:display-dependency-updates

# 更新所有依赖到最新版本（谨慎使用）
mvn versions:use-latest-versions
```

---

### 4. 查看依赖树

```bash
mvn dependency:tree
```

**说明**: 显示项目的完整依赖关系树，帮助解决依赖冲突

**输出示例**:
```
[INFO] site.aronnax:property-management-system:jar:1.0-SNAPSHOT
[INFO] +- org.springframework.boot:spring-boot-starter-web:jar:3.2.1:compile
[INFO] |  +- org.springframework.boot:spring-boot-starter:jar:3.2.1:compile
[INFO] |  |  +- org.springframework.boot:spring-boot:jar:3.2.1:compile
...
```

---

## 故障排查

### 问题 1: 端口被占用

**错误信息**:
```
***************************
APPLICATION FAILED TO START
***************************

Description:

Web server failed to start. Port 8081 was already in use.
```

#### 解决方案 A: 查找并停止占用端口的进程

**macOS / Linux**:
```bash
# 1. 查找占用 8081 端口的进程
lsof -i :8081

# 输出示例:
# COMMAND   PID  USER   FD   TYPE     DEVICE  SIZE/OFF  NODE  NAME
# java     1234  user   45u  IPv6  0x1234567      0t0   TCP  *:8081

# 2. 杀死进程
kill -9 1234
```

**Windows**:
```cmd
REM 1. 查找占用 8081 端口的进程
netstat -ano | findstr :8081

REM 输出示例:
REM TCP    0.0.0.0:8081    0.0.0.0:0    LISTENING    1234

REM 2. 杀死进程
taskkill /F /PID 1234
```

#### 解决方案 B: 更改应用端口

**临时更改**（命令行）:
```bash
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8082
```

**永久更改**（配置文件）:

编辑 `src/main/resources/application.properties`:
```properties
server.port=8082
```

---

### 问题 2: Maven 构建失败

**错误**: `Failed to execute goal`

#### 解决方案: 清理并重建

```bash
# 1. 完全清理
mvn clean

# 2. 删除本地依赖缓存（可选）
rm -rf ~/.m2/repository/

# 3. 重新下载依赖并构建
mvn clean install -U
```

参数说明:
- `-U`: 强制更新快照和发布版本

---

### 问题 3: 测试失败导致无法打包

**错误**: `There are test failures.`

#### 解决方案: 跳过测试或修复测试

```bash
# 临时跳过测试
mvn clean package -DskipTests

# 查看详细测试报告
cat target/surefire-reports/*.txt
```

---

### 问题 4: 依赖下载失败

**错误**: `Could not resolve dependencies`

#### 解决方案:

```bash
# 1. 检查网络连接

# 2. 清理损坏的依赖
mvn dependency:purge-local-repository

# 3. 使用国内镜像（编辑 ~/.m2/settings.xml）
```

在 `~/.m2/settings.xml` 添加：
```xml
<mirrors>
    <mirror>
        <id>aliyun</id>
        <mirrorOf>central</mirrorOf>
        <name>Aliyun Maven</name>
        <url>https://maven.aliyun.com/repository/public</url>
    </mirror>
</mirrors>
```

---

### 问题 5: Java 版本不兼容

**错误**: `Unsupported class file major version`

#### 解决方案:

```bash
# 1. 检查 Java 版本
java -version

# 2. 设置正确的 JAVA_HOME
export JAVA_HOME=/path/to/java-21

# 3. 或在 pom.xml 中调整版本
```

---

## 高级用法

### 1. 调试模式启动

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
```

**说明**: 在 5005 端口开启远程调试

**IDE 连接**:
- IntelliJ IDEA: Run → Attach to Process → 选择端口 5005
- Eclipse: Debug → Debug Configurations → Remote Java Application

---

### 2. 设置 JVM 参数

```bash
# 增加内存
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xms512m -Xmx1024m"

# 启用 GC 日志
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xlog:gc*"
```

---

### 3. Profile 切换

```bash
# 使用 dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 使用 prod profile
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

**配置文件**:
- `application.properties` (默认)
- `application-dev.properties`
- `application-prod.properties`

---

### 4. 查看项目信息

```bash
# 查看有效的 POM
mvn help:effective-pom

# 查看所有可用的插件目标
mvn help:describe -Dplugin=spring-boot

# 查看项目依赖
mvn dependency:list
```

---

### 5. 生成项目文档

```bash
# 生成站点文档
mvn site

# 输出目录: target/site/index.html
```

---

## 🚀 常用命令速查表

| 操作 | 命令 | 说明 |
|------|------|------|
| **启动开发服务器** | `mvn spring-boot:run` | 开发模式运行 |
| **快速打包** | `mvn clean package -DskipTests` | 跳过测试打包 |
| **运行测试** | `mvn test` | 执行所有测试 |
| **清理项目** | `mvn clean` | 删除 target/ |
| **完整构建** | `mvn clean install` | 清理+编译+测试+打包+安装 |
| **停止应用** | `Ctrl + C` | 优雅停止 |
| **查看依赖** | `mvn dependency:tree` | 依赖树 |
| **更新依赖** | `mvn clean install -U` | 强制更新 |

---

## 📝 最佳实践

### 开发阶段
```bash
# 每天第一次启动
mvn clean spring-boot:run

# 代码修改后
# 无需操作，自动重载（如果配置了 DevTools）

# 添加新依赖后
mvn clean compile
```

### 测试阶段
```bash
# 运行所有测试
mvn clean test

# 查看测试覆盖率
mvn clean test jacoco:report
```

### 打包发布
```bash
# 生产环境打包
mvn clean package -DskipTests

# 验证 JAR 可运行
java -jar target/property-management-system-1.0-SNAPSHOT.jar
```

---

## 🔧 配置 Maven 环境

### 设置 Maven 本地仓库位置

编辑 `~/.m2/settings.xml`:

```xml
<settings>
    <localRepository>/path/to/your/repo</localRepository>
</settings>
```

### 增加 Maven 内存

创建或编辑 `~/.mavenrc` (macOS/Linux):
```bash
export MAVEN_OPTS="-Xms512m -Xmx1024m"
```

Windows (`MAVEN_HOME\bin\mvn.cmd` 前添加):
```cmd
set MAVEN_OPTS=-Xms512m -Xmx1024m
```

---

## 📚 参考资源

- [Maven 官方文档](https://maven.apache.org/guides/)
- [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/current/maven-plugin/reference/htmlsingle/)
- [Maven 生命周期](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)

---

**文档版本**: 1.0
**最后更新**: 2026-01-11
**适用项目**: Smart Property Management System
