package site.aronnax.service;

import org.springframework.stereotype.Service;

/**
 * AI智能助手服务
 *
 * 当前实现：基于规则的简单响应（演示模式）
 * 生产环境：应集成阿里云通义千问API
 *
 * 集成步骤：
 * 1. 添加DashScope SDK依赖到pom.xml
 * 2. 在application.properties中配置API密钥
 * 3. 实现真实的API调用逻辑
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class AIService {

    // TODO: 生产环境中应从配置文件读取API密钥
    // @Value("${dashscope.api.key}")
    // private String apiKey;

    /**
     * 处理用户消息并返回AI回复
     *
     * 当前实现：基于关键词的规则匹配
     * TODO: 集成通义千问API后，此方法应调用真实的AI服务
     *
     * @param userMessage 用户输入的消息
     * @return AI回复内容
     */
    public String chat(String userMessage) {
        // 输入验证和消毒
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "请输入您的问题。";
        }

        // 简单规则匹配（演示模式）
        // 真实环境应替换为API调用
        if (userMessage.contains("缴费")) {
            return "您可以在【费用管理】或者【我的钱包】中进行缴费。支持微信、支付宝及余额支付。如果您的余额不足，请先充值。";
        } else if (userMessage.contains("报修")) {
            return "报修请拨打物业热线 8888-1234，或者在前台填写报修单。我们将尽快安排维修师傅上门。";
        } else if (userMessage.contains("水电")) {
            return "水电充值请前往【水电卡管理】页面。请注意，如果您有未缴的物业费，系统可能会限制您的购电功能，请优先结清账单。";
        } else if (userMessage.contains("欠费")) {
            return "欠费会导致您的水电卡无法充值。您可以点击首页的【欠费查询】查看具体欠费项。";
        }

        return "我是您的智能物业助手。您可以问我关于缴费、报修、停车等问题。(当前为演示模式，接入通义千问API后将更加智能)";
    }
}
