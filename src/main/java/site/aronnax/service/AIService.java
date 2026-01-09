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
 * AI智能助手服务
 *
 * 基于OpenAI官方Java SDK实现智能对话功能
 * 支持角色化提示词（业主助手/管理员助手）
 * 集成数据分析能力
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
     * openAIClient可能为null（当API未配置时）
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AIService(@org.springframework.lang.Nullable OpenAIClient openAIClient, AIDataService aiDataService) {
        this.openAIClient = openAIClient;
        this.aiDataService = aiDataService;
    }

    /**
     * 业主助手系统提示词
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

            【重要规则】
            1. 只能查询和回答当前登录业主的信息
            2. 不能执行缴费等操作，只能提供指引
            3. 遇到无法处理的问题，建议联系物业前台（电话：8888-1234）
            4. 保护业主隐私，不泄露其他业主信息
            5. 如果业主询问欠费情况，主动查询并告知详细信息

            【常见问题处理】
            - 缴费问题：引导到"费用管理"或"我的钱包"页面
            - 充值问题：说明需先清缴欠费才能充值水电卡
            - 报修问题：提供物业热线和前台地址
            - 投诉建议：记录并承诺转达给物业管理处
            """;

    /**
     * 管理员助手系统提示词
     */
    private static final String ADMIN_SYSTEM_PROMPT = """
            你是一位专业的物业管理数据分析助手，为物业管理人员提供决策支持。

            【你的职责】
            1. 分析小区整体的收费情况和欠费趋势
            2. 提供数据统计和可视化建议
            3. 识别高风险欠费楼栋和业主
            4. 辅助制定催缴策略和管理决策

            【你的能力】
            - 查询全小区的欠费统计
            - 分析收费率和收入分布
            - 查询特定业主的详细信息
            - 生成数据报告和趋势分析
            - 识别异常数据和风险点

            【沟通风格】
            - 使用专业的管理术语和数据分析语言
            - 提供数据支持的建议和洞察
            - 突出关键指标、异常情况和风险点
            - 提供可执行的行动建议
            - 使用图表、百分比等可视化描述

            【重要规则】
            1. 保护业主隐私，仅在必要时提供具体业主信息
            2. 提供的建议应基于数据分析，避免主观臆断
            3. 强调合规和人性化管理
            4. 识别数据异常时主动提醒
            5. 提供决策建议时考虑可行性和成本

            【分析重点】
            - 收费率趋势：关注低于80%的情况
            - 欠费集中度：识别欠费超过3个月的业主
            - 楼栋风险：标注欠费率超过30%的楼栋
            - 费用类型：分析哪类费用欠缴最严重
            - 季节性规律：识别缴费的时间规律
            """;

    /**
     * 处理用户消息并返回AI回复
     *
     * @param userMessage 用户输入的消息
     * @param userId      用户ID
     * @param userType    用户类型（OWNER/ADMIN）
     * @return AI回复内容
     */
    public String chat(String userMessage, Long userId, String userType) {
        // 如果OpenAI客户端未配置，使用fallback模式
        if (openAIClient == null) {
            return fallbackChat(userMessage, userId, userType);
        }

        try {
            // 构建消息列表 (使用 List<ChatCompletionMessageParam>)
            List<ChatCompletionMessageParam> messages = new ArrayList<>();

            // 1. 添加系统提示词
            String systemPrompt = "ADMIN".equalsIgnoreCase(userType)
                    ? ADMIN_SYSTEM_PROMPT
                    : OWNER_SYSTEM_PROMPT;

            messages.add(ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                    ChatCompletionSystemMessageParam.builder()
                            .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                            .content(systemPrompt)
                            .build()));

            // 2. 添加上下文数据
            String contextData = buildContextData(userId, userType);
            if (!contextData.isEmpty()) {
                messages.add(ChatCompletionMessageParam.ofChatCompletionSystemMessageParam(
                        ChatCompletionSystemMessageParam.builder()
                                .role(ChatCompletionSystemMessageParam.Role.SYSTEM)
                                .content("【当前数据上下文】\n" + contextData)
                                .build()));
            }

            // 3. 添加用户消息
            messages.add(ChatCompletionMessageParam.ofChatCompletionUserMessageParam(
                    ChatCompletionUserMessageParam.builder()
                            .role(ChatCompletionUserMessageParam.Role.USER)
                            .content(userMessage)
                            .build()));

            // 4. 构建请求参数
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .model(modelName)
                    .messages(messages)
                    .temperature(0.7)
                    .maxTokens(1000)
                    .build();

            // 5. 调用API
            ChatCompletion chatCompletion = openAIClient.chat().completions().create(params);

            // 6. 提取回复
            return chatCompletion.choices().stream()
                    .findFirst()
                    .flatMap(choice -> choice.message().content())
                    .orElse("抱歉，AI服务暂时无法响应，请稍后重试。");

        } catch (Exception e) {
            System.err.println("[AIService] API调用失败: " + e.getMessage());
            e.printStackTrace();
            return fallbackChat(userMessage, userId, userType);
        }
    }

    /**
     * 构建上下文数据
     * 根据用户类型提供不同的数据上下文
     */
    private String buildContextData(Long userId, String userType) {
        StringBuilder context = new StringBuilder();

        try {
            if ("ADMIN".equalsIgnoreCase(userType)) {
                // 管理员：提供全局统计数据
                Map<String, Object> stats = aiDataService.getGlobalArrearsStatistics();
                Map<String, Object> collectionRate = aiDataService.getCollectionRateStatistics();

                context.append("全局统计数据：\n");
                context.append("- 总欠费金额：").append(stats.get("totalUnpaidAmount")).append("元\n");
                context.append("- 欠费账单数：").append(stats.get("unpaidCount")).append("条\n");
                context.append("- 收费率：").append(String.format("%.2f%%",
                        ((Number) collectionRate.get("rate")).doubleValue() * 100)).append("\n");

            } else {
                // 业主：提供个人数据
                Map<String, Object> arrears = aiDataService.getUserArrears(userId);
                Map<String, Object> wallet = aiDataService.getUserWalletBalance(userId);
                Map<String, Object> cards = aiDataService.getUserUtilityCards(userId);

                context.append("您的账户信息：\n");

                // 欠费信息
                if ((Boolean) arrears.get("hasArrears")) {
                    context.append("- ⚠️ 您有 ").append(arrears.get("arrearsCount"))
                            .append(" 笔未缴费用，总计 ").append(arrears.get("totalArrears")).append(" 元\n");
                } else {
                    context.append("- ✅ 您没有欠费\n");
                }

                // 钱包余额
                context.append("- 钱包余额：").append(wallet.get("balance")).append(" 元\n");

                // 水电卡
                int cardCount = (Integer) cards.get("cardCount");
                if (cardCount > 0) {
                    context.append("- 水电卡数量：").append(cardCount).append(" 张\n");
                }
            }
        } catch (Exception e) {
            System.err.println("[AIService] 构建上下文数据失败: " + e.getMessage());
        }

        return context.toString();
    }

    /**
     * Fallback模式：当API未配置时使用规则匹配
     */
    private String fallbackChat(String userMessage, Long userId, String userType) {
        // 基于关键词的简单规则匹配
        if (userMessage.contains("欠费") || userMessage.contains("账单")) {
            try {
                Map<String, Object> arrears = aiDataService.getUserArrears(userId);
                if ((Boolean) arrears.get("hasArrears")) {
                    return String.format("您当前有 %d 笔未缴费用，总计 %.2f 元。请及时缴纳以避免影响水电卡充值。" +
                            "您可以在【费用管理】或【我的钱包】中进行缴费。",
                            arrears.get("arrearsCount"), arrears.get("totalArrears"));
                } else {
                    return "您目前没有欠费，账单状态良好！";
                }
            } catch (Exception e) {
                return "查询欠费信息失败，请稍后重试。";
            }
        } else if (userMessage.contains("缴费")) {
            return "您可以在【费用管理】或者【我的钱包】中进行缴费。支持微信、支付宝及余额支付。如果您的余额不足，请先充值。";
        } else if (userMessage.contains("报修")) {
            return "报修请拨打物业热线 8888-1234，或者在前台填写报修单。我们将尽快安排维修师傅上门。";
        } else if (userMessage.contains("水电") || userMessage.contains("充值")) {
            return "水电充值请前往【水电卡管理】页面。请注意，如果您有未缴的物业费或取暖费，系统会限制您的充值功能，请优先结清账单。";
        }

        return "我是您的智能物业助手。您可以问我关于缴费、报修、欠费查询等问题。" +
                "\n\n💡 提示：当前使用演示模式，配置 OPENAI_API_KEY 后将获得更智能的AI服务。";
    }
}
