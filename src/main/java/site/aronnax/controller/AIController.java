package site.aronnax.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import site.aronnax.common.Result;
import site.aronnax.entity.User;
import site.aronnax.service.AIService;

/**
 * AI助手控制器
 * 提供智能问答服务（OpenAI兼容API）
 *
 * 功能：
 * - 支持业主和管理员角色化对话
 * - 自动注入用户上下文数据
 * - 提供智能数据分析和建议
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
     * @param params  包含message字段的参数Map
     * @param session HTTP会话，用于获取当前登录用户信息
     * @return AI回复内容
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> params, HttpSession session) {
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
            // 从Session获取当前用户信息
            User currentUser = (User) session.getAttribute("user");

            // 如果未登录，使用默认值
            Long userId = currentUser != null ? currentUser.getUserId() : 1L;
            String userType = currentUser != null ? currentUser.getUserType() : "OWNER";

            // 调用AI服务处理消息
            // 传递用户ID和角色信息，AI会根据角色使用不同的提示词
            String response = aiService.chat(message.trim(), userId, userType);

            return Result.success(response);
        } catch (Exception e) {
            System.err.println("[AIController] AI服务调用失败: " + e.getMessage());
            e.printStackTrace();
            return Result.error("AI服务暂时不可用，请稍后重试");
        }
    }

    /**
     * 获取AI服务状态
     *
     * @return AI服务配置状态
     */
    @PostMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        try {
            Map<String, Object> status = Map.of(
                    "available", true,
                    "mode", System.getenv("OPENAI_API_KEY") != null ? "API" : "Fallback",
                    "message", System.getenv("OPENAI_API_KEY") != null
                            ? "AI服务已配置"
                            : "使用演示模式，配置API密钥后可获得完整AI功能");
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("获取状态失败");
        }
    }
}
