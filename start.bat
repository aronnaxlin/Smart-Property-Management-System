@echo off
chcp 65001 >nul
REM ============================================================================
REM 智慧物业管理系统 - 启动脚本 (Windows)
REM Smart Property Management System - Startup Script
REM ============================================================================

echo ============================================
echo   智慧物业管理系统 启动中...
echo   Smart Property Management System
echo ============================================
echo.

REM 检查 Java 环境
echo 检查 Java 环境...
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ 错误: 未找到 Java 运行环境
    echo    请安装 Java 21 或更高版本
    echo    下载地址: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)

echo ✅ Java 环境正常
echo.

REM 设置 JAR 文件路径
set JAR_FILE=target\property-management-system-1.0-SNAPSHOT.jar

REM 检查 JAR 文件是否存在
if not exist "%JAR_FILE%" (
    echo ❌ 错误: 找不到 JAR 文件: %JAR_FILE%
    echo    请先运行以下命令构建项目:
    echo    mvn clean package -DskipTests
    pause
    exit /b 1
)

echo ✅ 找到 JAR 文件: %JAR_FILE%
echo.

REM 检查 .env 文件
if not exist ".env" (
    echo ⚠️  警告: 未找到 .env 文件
    echo    AI 助手功能将不可用
    echo    如需启用，请复制 .env_template 为 .env 并配置 API 密钥
    echo.
)

REM 设置 JVM 参数
set JVM_OPTS=-Xms256m -Xmx512m -Dfile.encoding=UTF-8

REM 启动应用
echo 🚀 启动应用...
echo    访问地址: http://localhost:8081
echo    按 Ctrl+C 停止应用
echo.
echo ============================================
echo.

java %JVM_OPTS% -jar "%JAR_FILE%"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ 应用启动失败
    pause
)
