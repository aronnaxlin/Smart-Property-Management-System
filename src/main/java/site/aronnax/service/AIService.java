package site.aronnax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletion;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionMessageParam;
import com.openai.models.ChatCompletionSystemMessageParam;
import com.openai.models.ChatCompletionUserMessageParam;

/**
 * AI 智能助手服务
 * 基于 OpenAI 官方 Java SDK 实现智能对话功能，深度集成小区物业数据。
 *
 * 核心特性：
 * 1. 角色化提示词：内置“业主助手”与“管理员助手”两种角色逻辑。
 * 2. 实时上下文：自动根据登录用户身份（业主/管理）提取关联的欠费、余额、统计等数据作为 AI 背景。
 * 3. 稳健回退：当 API 密钥未配置或调用失败时，自动切换至基于规则的演示/Fallback 模式。
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class AIService {

    private final OpenAIClient openAIClient;
    private final AIDataService aiDataService;

    @Value("${openai.model.name:gpt-3.5-turbo}")
    private String modelName;

    /**
     * 构造函数注入
     *
     * @param openAIClient  OpenAI 客户端实例（若 API 缺失可能为 null）
     * @param aiDataService 提供业务数据上下文支持
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AIService(@org.springframework.lang.Nullable OpenAIClient openAIClient, AIDataService aiDataService) {
        this.openAIClient = openAIClient;
        this.aiDataService = aiDataService;
    }

    /**
     * 业主助手系统提示词：定义对话风格、职责范围及业务规则。
     */
    private static final String OWNER_SYSTEM_PROMPT = """
            你是一位专业、友好的物业管理助手，专门为小区业主提供服务。

            【你的职责】
            1. 解答业主关于物业费、取暖费、水电费的疑问
            2. 指导业主如何使用系统进行缴费、充值等操作
            3. 提供报修、投诉等服务指引
            4. 查询业主的账单和欠费情况

            【你的能力】
            - 可以查询业主的欠费信息
            - 可以查看业主的账单明细
            - 可以查询钱包和水电卡余额
            - 可以提供缴费指引

            【沟通风格】
            - 使用礼貌、专业的语气，称呼业主为"您"
            - 用简洁明了的语言解释，避免使用过多专业术语
            - 主动提供解决方案和操作指引
            - 对于欠费情况，委婉提醒并引导缴费
            - 回答要具体、实用，包含具体的操作步骤

            【严格的权限限制】
            1. ⚠️ 你只能访问当前登录业主的个人数据，绝对禁止查询其他业主信息
            2. 如果业主询问"所有业主"、"其他业主"、"全小区"等全局数据，必须回复："为保护业主隐私，我只能查询您本人的信息"
            3. 不能执行任何写操作（缴费、充值等），只能提供操作指引
            4. 遇到无法处理的问题，建议联系物业前台（电话：8888-1234）
            5. 如果业主询问欠费情况，主动查询并告知详细信息

            【常见问题处理】
            - 缴费问题：引导到"费用管理"或"我的钱包"页面
            - 充值问题：说明需先清缴欠费才能充值水电卡
            - 报修问题：提供物业热线和前台地址
            - 投诉建议：记录并承诺转达给物业管理处
            - 其他业主信息查询：委婉拒绝并说明隐私保护政策
            """;

    /**
     * 管理员助手系统提示词：专注于数据分析、决策支持和风险识别。
     */
    private static final String ADMIN_SYSTEM_PROMPT = """
            你是一位专业的物业管理数据分析助手，为物业管理人员提供决策支持。

            【你的职责】
            1. 分析小区整体的收费情况和欠费趋势
            2. 提供数据统计和可视化建议
            3. 识别高风险欠费楼栋和业主
            4. 辅助制定催缴策略和管理决策

            【管理员权限能力】
            - ✅ 查询全小区的欠费统计和收费率
            - ✅ 分析收入分布和楼栋风险
            - ✅ 查看业主概览（脱敏）和入住率
            - ✅ 生成数据报告和趋势分析
            - ✅ 识别异常数据和风险点

            【沟通风格】
            - 使用专业的管理术语和数据分析语言
            - 提供数据支持的建议和洞察
            - 突出关键指标、异常情况和风险点
            - 提供可执行的行动建议
            - 使用图表、百分比等可视化描述

            【重要规则】
            1. 遵守数据合规要求，避免批量导出敏感个人信息
            2. 提供的建议应基于数据分析，避免主观臆断
            3. 强调合规和人性化管理，催缴时注意方式方法
            4. 识别数据异常时主动提醒并建议核查
            5. 提供决策建议时考虑可行性、成本和社会影响

            【分析重点】
            - 收费率趋势：关注低于80%的情况并分析原因
            - 欠费集中度：识别欠费超过3个月的业主，建议重点跟进
            - 楼栋风险：标注欠费率超过30%的楼栋，分析区域特征
            - 费用类型：分析哪类费用欠缴最严重，优化收费策略
            - 季节性规律：识别缴费的时间规律，优化催缴时机
            """;

    /**
     * 执行智能对话
     *
     * 逻辑流程：
     * 1. 验证客户端可用性 -> 2. 根据身份选择 System Prompt -> 3. 提取用户关联业务数据（如欠费、余额）
     * -> 4. 组装消息包发送至云端 -> 5. 解析并返回回复。
     *
     * @param userMessage 用户输入的消息文本
     * @param userId      当前登录用户的主键 ID
     * @param userType    用户角色标识（ADMIN 或 OWNER）
     * @return AI 生成的回复内容
     */
    public String chat(String userMessage, Long userId, String userType) {
        // 如果 API 客户端未初始化，则进入本地 Fallback 模式
        if (openAIClient == null) {
            return fallbackChat(userMessage, userId, userType);
        }

        try {
            List<ChatCompletionMessageParam> messages = new ArrayList<>();

            // 1. 设置角色提示词
            String systemPrompt = "ADMIN".equalsIgnoreCase(userType)
                    ? ADMIN_SYSTEM_PROMPT
                    : OWNER_SYSTEM_PROMPT;

            messages.add(ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                    ChatCompletionSystemMessageParam.builder()
                            .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                            .content(systemPrompt)
                            .build()));

            // 2. 注入实时业务上下文（让 AI 能够“看见”该业主的实际欠费和余额）
            String contextData = buildContextData(userId, userType);
            if (!contextData.isEmpty()) {
                messages.add(ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                        ChatCompletionSystemMessageParam.builder()
                                .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                                .content("【当前动态业务数据】\n" + contextData)
                                .build()));
            }

            // 3. 用户消息
            messages.add(ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                    ChatCompletionUserMessageParam.builder()
                            .role(ChatCompletionUserMessageParam.Role.USER)
                            .content(userMessage)
                            .build()));

            // 4. 配置模型参数并执行请求
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(modelName)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();

            ChatCompletion chatCompletion = openAIClient.chat().completions().create(params);

            // 5. 提取并返回首选回复文本
            return chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("抱歉，AI 服务暂时无法响应，请稍后重试。");

        } catch (Exception e) {
            System.err.println("[AIService] 接口调用异常: " + e.getMessage());
            return fallbackChat(userMessage, userId, userType);
        }
    }

    /**
     * 构建动态上下文数据
     * 从数据库中提取该用户的实时财务与资产状态，作为 AI 对话的“知识库”。
     */
    private String buildContextData(Long userId, String userType) {
        StringBuilder context = new StringBuilder();

        try {
            if ("ADMIN".equalsIgnoreCase(userType)) {
                // 管理员模式：提供全局数据和分析能力
                context.append("=== 管理员全局数据视图 ===\n\n");

                // 1. 全局欠费统计
                Map<String, Object> stats = aiDataService.getGlobalArrearsStatistics(userType);
                if (!Boolean.TRUE.equals(stats.get("permissionDenied"))) {
                    context.append("【小区欠费统计】\n");
                    context.append("- 待缴总额：").append(stats.get("totalUnpaidAmount")).append(" 元\n");
                    context.append("- 待缴笔数：").append(stats.get("unpaidCount")).append(" 条\n");
                }

                // 2. 收费率分析
                Map<String, Object> collectionRate = aiDataService.getCollectionRateStatistics(userType);
                if (!Boolean.TRUE.equals(collectionRate.get("permissionDenied"))) {
                    context.append("- 当前收费率：").append(String.format("%.2f%%",
                            ((Number) collectionRate.get("rate")).doubleValue() * 100)).append("\n");
                }

                // 3. 业主概览
                Map<String, Object> ownersOverview = aiDataService.getAllOwnersOverview(userType);
                if (!Boolean.TRUE.equals(ownersOverview.get("permissionDenied"))) {
                    context.append("\n【房产入住情况】\n");
                    context.append("- 房产总数：").append(ownersOverview.get("totalProperties")).append("\n");
                    context.append("- 已入住：").append(ownersOverview.get("occupiedProperties")).append("\n");
                    context.append("- 空置：").append(ownersOverview.get("vacantProperties")).append("\n");
                    context.append("- 入住率：").append(ownersOverview.get("occupancyRate")).append("\n");
                }

                // 4. 风险楼栋分析
                Map<String, Object> riskBuildings = aiDataService.getRiskBuildingAnalysis(userType);
                if (!Boolean.TRUE.equals(riskBuildings.get("permissionDenied"))) {
                    context.append("\n【高风险楼栋】\n");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> topRisk = (List<Map<String, Object>>) riskBuildings
                            .get("topRiskBuildings");
                    if (topRisk != null && !topRisk.isEmpty()) {
                        for (int i = 0; i < Math.min(3, topRisk.size()); i++) {
                            Map<String, Object> bldg = topRisk.get(i);
                            context.append("  ").append(i + 1).append(". ")
                                    .append(bldg.get("building_no")).append("号楼：")
                                    .append(bldg.get("unpaid_count")).append("笔欠费\n");
                        }
                    }
                }

            } else {
                // 业主模式：仅提供个人数据，强调权限边界
                context.append("=== 您的个人账户信息 ===\n");
                context.append("（提示：您只能查询本人数据，无法访问其他业主信息）\n\n");

                // 1. 个人欠费情况
                Map<String, Object> arrears = aiDataService.getUserArrears(userId, userId, userType);
                if (Boolean.TRUE.equals(arrears.get("permissionDenied"))) {
                    context.append("- ⚠️ 权限受限：").append(arrears.get("message")).append("\n");
                } else if (Boolean.TRUE.equals(arrears.get("hasArrears"))) {
                    context.append("【待缴账单】\n");
                    context.append("- ⚠️ 待结账单：共 ").append(arrears.get("arrearsCount"))
                            .append(" 笔，合计金额 ").append(arrears.get("totalArrears")).append(" 元\n");
                } else {
                    context.append("【账单状态】\n");
                    context.append("- ✅ 状态良好：目前无待缴费用\n");
                }

                // 2. 钱包余额
                Map<String, Object> wallet = aiDataService.getUserWalletBalance(userId);
                context.append("\n【钱包余额】\n");
                context.append("- 当前余额：").append(wallet.get("balance")).append(" 元\n");

                // 3. 水电卡信息
                Map<String, Object> cards = aiDataService.getUserUtilityCards(userId);
                int cardCount = (Integer) cards.get("cardCount");
                context.append("\n【水电卡】\n");
                if (cardCount > 0) {
                    context.append("- 已绑定卡片数：").append(cardCount).append(" 张\n");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> cardList = (List<Map<String, Object>>) cards.get("cards");
                    for (Map<String, Object> card : cardList) {
                        context.append("  · ").append(card.get("cardType")).append("：")
                                .append(card.get("balance")).append("元（")
                                .append(card.get("propertyInfo")).append("）\n");
                    }
                } else {
                    context.append("- 暂无绑定水电卡\n");
                }
            }
        } catch (Exception e) {
            System.err.println("[AIService] 上下文组装失败: " + e.getMessage());
            e.printStackTrace();
        }

        return context.toString();
    }

    /**
     * Fallback 对话模式（本地演示模式）
     * 采用简单的关键词匹配算法，在离线或 API 异常时依然能提供基础的业务指引。
     */
    private String fallbackChat(String userMessage, Long userId, String userType) {
        // 权限检查 - 业主询问全局数据
        if (!"ADMIN".equalsIgnoreCase(userType) &&
                (userMessage.contains("所有业主") || userMessage.contains("全小区") ||
                        userMessage.contains("其他业主") || userMessage.contains("整体") ||
                        userMessage.contains("全局"))) {
            return "为保护业主隐私，我只能查询您本人的账单和财务信息。如需了解小区整体情况，请联系物业管理处。";
        }

        // 基于关键词的简单规则匹配
        if (userMessage.contains("欠费") || userMessage.contains("账单")) {
            try {
                Map<String, Object> arrears = aiDataService.getUserArrears(userId, userId, userType);
                if (Boolean.TRUE.equals(arrears.get("permissionDenied"))) {
                    return "权限不足：" + arrears.get("message");
                }
                if (Boolean.TRUE.equals(arrears.get("hasArrears"))) {
                    return String.format("您当前有 %d 笔未缴费用，总计 %.2f 元。请注意，欠缴物业费会导致水电卡充值功能锁定。" +
                            "您可前往【费用管理】模块进行结算。",
                            arrears.get("arrearsCount"), arrears.get("totalArrears"));
                } else {
                    return "检测到您当前并无欠费，感谢您的支持！";
                }
            } catch (Exception e) {
                return "抱歉，系统暂时无法同步您的财务数据，建议稍后查看个人账单。";
            }
        } else if (userMessage.contains("缴费")) {
            return "您可以在【费用管理】或者【我的钱包】中进行缴费。支持微信、支付宝及余额支付。如果您的余额不足，请先充值。";
        } else if (userMessage.contains("报修")) {
            return "报修请拨打物业热线 8888-1234，或者在前台填写报修单。我们将尽快安排维修师傅上门。";
        } else if (userMessage.contains("水电") || userMessage.contains("充值")) {
            return "水电充值请前往【水电卡管理】页面。请注意，如果您有未缴的物业费或取暖费，系统会限制您的充值功能，请优先结清账单。";
        } else if ("ADMIN".equalsIgnoreCase(userType) &&
                (userMessage.contains("统计") || userMessage.contains("收费率") || userMessage.contains("分析"))) {
            return "管理员模式：您可以询问全小区的收费率、欠费统计、风险楼栋分析等数据。" +
                    "\n\n💡 提示：当前使用演示模式，配置 OPENAI_API_KEY 后将获得更智能的数据分析能力。";
        }

        String roleDesc = "ADMIN".equalsIgnoreCase(userType) ? "管理员" : "业主";
        return "我是您的智能物业助手（" + roleDesc + "模式）。您可以问我关于缴费、报修、欠费查询等问题。" +
                "\n\n💡 提示：当前使用演示模式，配置 OPENAI_API_KEY 后将获得更智能的AI服务。";
    }
}
