# AI集成使用指南

## 环境配置

### 1. 配置环境变量

复制 `.env.example` 为 `.env` 并填入真实配置：

```bash
cp .env.example .env
```

编辑 `.env` 文件：

```properties
# OpenAI兼容API基础URL
OPENAI_API_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1

# API密钥
OPENAI_API_KEY=sk-your-api-key-here

# 模型名称
OPENAI_MODEL_NAME=qwen-plus
```

### 2. 支持的API提供商

#### 阿里云通义千问（推荐）
```properties
OPENAI_API_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
OPENAI_API_KEY=sk-your-dashscope-key
OPENAI_MODEL_NAME=qwen-plus
```

#### DeepSeek
```properties
OPENAI_API_BASE_URL=https://api.deepseek.com/v1
OPENAI_API_KEY=sk-your-deepseek-key
OPENAI_MODEL_NAME=deepseek-chat
```

#### OpenAI官方
```properties
OPENAI_API_BASE_URL=https://api.openai.com/v1
OPENAI_API_KEY=sk-your-openai-key
OPENAI_MODEL_NAME=gpt-4
```

#### 本地Ollama
```properties
OPENAI_API_BASE_URL=http://localhost:11434/v1
OPENAI_API_KEY=ollama
OPENAI_MODEL_NAME=llama2
```

### 3. 在系统中设置环境变量

#### macOS/Linux
```bash
export OPENAI_API_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
export OPENAI_API_KEY="your-api-key"
export OPENAI_MODEL_NAME="qwen-plus"
```

#### Windows
```cmd
set OPENAI_API_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
set OPENAI_API_KEY=your-api-key
set OPENAI_MODEL_NAME=qwen-plus
```

## 功能说明

### 业主助手

业主登录后，AI助手会：
- 使用友好、礼貌的语气
- 自动查询业主的欠费、账单、钱包余额
- 提供缴费、充值的操作指引
- 解答物业费、水电费相关问题
- 提供报修、投诉服务指引

**示例对话**：
```
业主：我有欠费吗？
AI：您好！经查询，您当前有2笔未缴费用，总计350元：
    - 物业费：200元
    - 取暖费：150元
    建议您尽快缴纳，以免影响水电卡充值。您可以在【费用管理】或【我的钱包】中进行缴费。
```

### 管理员助手

管理员登录后，AI助手会：
- 使用专业的数据分析语言
- 提供全局统计数据和趋势分析
- 识别高风险欠费楼栋
- 提供催缴策略建议
- 生成数据报告

**示例对话**：
```
管理员：哪些楼栋欠费最严重？
AI：根据当前数据分析：

    欠费TOP3楼栋：
    1. 3号楼：欠费12户，总计8,500元
    2. 7号楼：欠费9户，总计6,200元
    3. 5号楼：欠费7户，总计4,800元

    建议：
    - 优先催缴3号楼，欠费集中度高
    - 关注长期欠费（超过3个月）的业主
    - 可考虑分期缴费方案
```

## API接口

### 聊天接口

**请求**：
```http
POST /api/ai/chat
Content-Type: application/json

{
  "message": "我有欠费吗？"
}
```

**响应**：
```json
{
  "code": 200,
  "message": "success",
  "data": "您好！经查询，您当前没有欠费，账单状态良好！"
}
```

### 状态查询

**请求**：
```http
POST /api/ai/status
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "available": true,
    "mode": "API",
    "message": "AI服务已配置"
  }
}
```

## Fallback模式

如果未配置API密钥，系统会自动切换到Fallback模式：
- 使用基于规则的关键词匹配
- 仍可查询数据并提供基本回答
- 功能有限，建议配置API获得完整体验

## 故障排查

### 问题1：AI服务不可用

**检查**：
1. 环境变量是否正确配置
2. API密钥是否有效
3. 网络是否可访问API地址

**解决**：
```bash
# 测试API连接
curl -X POST "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions" \
  -H "Authorization: Bearer YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"qwen-plus","messages":[{"role":"user","content":"你好"}]}'
```

### 问题2：响应缓慢

**原因**：
- API服务器响应慢
- 网络延迟

**解决**：
- 使用国内API服务（如通义千问）
- 调整超时时间配置

## 数据分析能力

AI助手可以调用以下数据接口：

1. **用户欠费查询** - `getUserArrears(userId)`
2. **钱包余额查询** - `getUserWalletBalance(userId)`
3. **水电卡查询** - `getUserUtilityCards(userId)`
4. **全局统计**（管理员）- `getGlobalArrearsStatistics()`
5. **收费率统计**（管理员）- `getCollectionRateStatistics()`

AI会根据对话内容自动判断是否需要查询数据，并将数据融入回答中。
