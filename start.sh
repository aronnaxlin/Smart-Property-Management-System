#!/bin/bash

###############################################################################
# 智慧物业管理系统 - 启动脚本 (Linux/macOS)
# Smart Property Management System - Startup Script
###############################################################################

echo "============================================"
echo "  智慧物业管理系统 启动中..."
echo "  Smart Property Management System"
echo "============================================"
echo ""

# 检查 Java 版本
echo "检查 Java 环境..."
if ! command -v java &> /dev/null; then
    echo "❌ 错误: 未找到 Java 运行环境"
    echo "   请安装 Java 21 或更高版本"
    echo "   下载地址: https://www.oracle.com/java/technologies/downloads/"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d '.' -f 1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ 错误: Java 版本过低 (需要 Java 21+, 当前 Java $JAVA_VERSION)"
    exit 1
fi

echo "✅ Java 环境正常 (Java $JAVA_VERSION)"
echo ""

# 设置 JAR 文件路径
JAR_FILE="target/property-management-system-1.0-SNAPSHOT.jar"

# 检查 JAR 文件是否存在
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ 错误: 找不到 JAR 文件: $JAR_FILE"
    echo "   请先运行以下命令构建项目:"
    echo "   mvn clean package -DskipTests"
    exit 1
fi

echo "✅ 找到 JAR 文件: $JAR_FILE"
echo ""

# 检查 .env 文件
if [ ! -f ".env" ]; then
    echo "⚠️  警告: 未找到 .env 文件"
    echo "   AI 助手功能将不可用"
    echo "   如需启用，请复制 .env_template 为 .env 并配置 API 密钥"
    echo ""
fi

# 设置 JVM 参数
JVM_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8"

# 启动应用
echo "🚀 启动应用..."
echo "   访问地址: http://localhost:8081"
echo "   按 Ctrl+C 停止应用"
echo ""
echo "============================================"
echo ""

java $JVM_OPTS -jar "$JAR_FILE"
