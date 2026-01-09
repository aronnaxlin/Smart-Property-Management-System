package site.aronnax.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.service.AIService;

/**
 * AI助手控制器
 * 提供智能问答服务（通义千问集成）
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final AIService aiService;

    public AIController(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * AI聊天接口
     *
     * @param params 包含message字段的参数Map
     * @return AI回复内容
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> params) {
        // 参数验证：提取消息内容
        String message = params.get("message");

        // 验证消息不能为空
        if (message == null || message.trim().isEmpty()) {
            return Result.error("请输入内容");
        }

        // 验证消息长度（防止恶意超长输入）
        if (message.length() > 1000) {
            return Result.error("输入内容过长，请控制在1000字以内");
        }

        try {
            // 调用AI服务处理消息
            String response = aiService.chat(message.trim());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }
}
