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
 * AI 智能管家控制器
 * 提供基于全区数据的语义问答与决策指引（OpenAI API 兼容协议）。
 *
 * 特性：
 * - 角色感知：支持 OWNER（业主）与 ADMIN（管理员）双重 Prompt 策略。
 * - 上下文注入：自动提取当前用户的财务、房产数据并喂给模型，实现精准回答。
 * - 降级容错：当远程模型不可达时，自动触发本地 fallback 模式。
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
     * 异步对话交互入口
     *
     * 业务流程：
     * 1. 语义提取 -> 2. 用户会话感知 -> 3. 数据透传 -> 4. 结果包装。
     *
     * @param params  包含待处理 message 的 JSON 负载
     * @param session 用于标识当前登录用户身份
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody Map<String, String> params, HttpSession session) {
        String message = params.get("message");

        if (message == null || message.trim().isEmpty()) {
            return Result.error("消息内容不能为空");
        }

        // 字节级安全防护：防止恶意大量文本注入
        if (message.length() > 1000) {
            return Result.error("输入字数已触及语义分析上限（1000字）");
        }

        try {
            // 获取会话锚点：识别当前操作者的权限范围
            User currentUser = (User) session.getAttribute("user");

            // 权限后退机制：未登录状态下默认作为“普通业主”进行有限咨询
            Long userId = currentUser != null ? currentUser.getUserId() : 1L;
            String userType = currentUser != null ? currentUser.getUserType() : "OWNER";

            // 执行核心推理链路
            String response = aiService.chat(message.trim(), userId, userType);

            return Result.success(response);
        } catch (Exception e) {
            // 异常落盘：由于 AI 接口受公网波动影响，需详细记录链路报错
            System.err.println("[AI-API-ERROR] 智能助手通信链路中断: " + e.getMessage());
            return Result.error("AI 助手正在思考中，当前连接不稳定，请换个话题试试");
        }
    }

    /**
     * 服务健康度审计
     * 反馈当前 AI 引擎的运行模式（API 联动或本地演示模式）。
     */
    @PostMapping("/status")
    public Result<Map<String, Object>> getStatus() {
        try {
            // 从环境变量嗅探配置状态
            boolean isConfigured = System.getenv("OPENAI_API_KEY") != null;
            Map<String, Object> status = Map.of(
                    "available", true,
                    "mode", isConfigured ? "SMART_API" : "FALLBACK_DEMO",
                    "status_desc", isConfigured
                            ? "AI 实时大脑已就绪"
                            : "当前处于本地演示模式，配置 API Key 后可解锁全量数据分析能力");
            return Result.success(status);
        } catch (Exception e) {
            return Result.error("无法获取 AI 服务运行状态");
        }
    }
}
