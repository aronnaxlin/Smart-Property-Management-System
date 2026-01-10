package site.aronnax.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * AI 智能互联配置类
 * 基于 OpenAI 官方 SDK 构建，支持多种兼容协议的大模型接入。
 *
 * 扩展性说明：
 * 通过修改环境变量，可以无缝切换至：
 * - 阿里云通义千问 (DashScope)
 * - DeepSeek 深度求索
 * - 自建 LocalAI 实例
 *
 * @author Aronnax (Li Linhan)
 */
@Configuration
public class AIConfig {

    /**
     * API 基础端点 (Base URL)
     * 默认指向 OpenAI 官方节点：https://api.openai.com/v1
     */
    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String apiBaseUrl;

    /**
     * 密钥校验令牌 (API Key)
     * 必须在外部环境中通过 OPENAI_API_KEY 注入。
     */
    @Value("${openai.api.key:}")
    private String apiKey;

    /**
     * 模型路由标识 (Model Name)
     * 指定对话模型版本（如 gpt-3.5-turbo 或 qwen-max）。
     */
    @Value("${openai.model.name:gpt-3.5-turbo}")
    private String modelName;

    /**
     * 自动化初始化 OpenAI 客户端 Bean
     *
     * [风控逻辑]：
     * 如果未检测到 API Key，系统将自动回退（Fallback）至受限的本地演示对话模式，
     * 确保应用在无配置情况下仍能正常启动。
     */
    @Bean
    public OpenAIClient openAIClient() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[AI-CONFIG-WARN] 系统未探测到 OPENAI_API_KEY。AI 控制台将启用“离线演示模式”。");
            return null;
        }

        try {
            // 通过 Builder 构建链条，注入自定义端点与安全凭据
            OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey);

            // 自定义反向代理或国产模型端点适配
            if (apiBaseUrl != null && !apiBaseUrl.trim().isEmpty()
                    && !apiBaseUrl.equals("https://api.openai.com/v1")) {
                builder.baseUrl(apiBaseUrl);
            }

            OpenAIClient client = builder.build();

            // 启动就绪日志
            System.out.println("[AI-SYSTEM] ✅ 智能语义分析引擎已在线就绪");
            System.out.println("[AI-SYSTEM] 接入节点: " + apiBaseUrl);
            System.out.println("[AI-SYSTEM] 指定模型: " + modelName);

            return client;
        } catch (Exception e) {
            System.err.println("[AI-SYSTEM-ERROR] ❌ 智能引擎链路初始化失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 模型名称单例注入，供 Service 层进行 API 调用包装。
     */
    @Bean
    public String aiModelName() {
        return modelName;
    }
}
