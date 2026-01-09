package site.aronnax.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

/**
 * AI配置类
 * 使用OpenAI官方Java SDK
 * 从环境变量读取配置并初始化客户端
 *
 * 支持的API提供商：
 * - OpenAI官方API
 * - 阿里云通义千问（DashScope）
 * - DeepSeek
 * - 任何OpenAI兼容API
 *
 * @author Aronnax (Li Linhan)
 */
@Configuration
public class AIConfig {

    /**
     * API基础URL
     * 从环境变量OPENAI_API_BASE_URL读取
     */
    @Value("${openai.api.base-url:https://api.openai.com/v1}")
    private String apiBaseUrl;

    /**
     * API密钥
     * 从环境变量OPENAI_API_KEY读取
     */
    @Value("${openai.api.key:}")
    private String apiKey;

    /**
     * 模型名称
     * 从环境变量OPENAI_MODEL_NAME读取
     */
    @Value("${openai.model.name:gpt-3.5-turbo}")
    private String modelName;

    /**
     * 创建OpenAI客户端Bean
     *
     * @return OpenAIClient实例，如果未配置API密钥则返回null
     */
    @Bean
    public OpenAIClient openAIClient() {
        // 检查API密钥是否配置
        if (apiKey == null || apiKey.trim().isEmpty()) {
            System.out.println("[AIConfig] OPENAI_API_KEY未配置，AI功能将使用Fallback模式");
            return null;
        }

        try {
            // 使用Builder模式创建客户端
            OpenAIOkHttpClient.Builder builder = OpenAIOkHttpClient.builder()
                    .apiKey(apiKey);

            // 如果配置了自定义BaseURL，设置它
            if (apiBaseUrl != null && !apiBaseUrl.trim().isEmpty()
                    && !apiBaseUrl.equals("https://api.openai.com/v1")) {
                builder.baseUrl(apiBaseUrl);
            }

            OpenAIClient client = builder.build();

            System.out.println("[AIConfig] ✅ AI服务初始化成功");
            System.out.println("[AIConfig] Base URL: " + apiBaseUrl);
            System.out.println("[AIConfig] Model: " + modelName);

            return client;
        } catch (Exception e) {
            System.err.println("[AIConfig] ❌ AI服务初始化失败: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取配置的模型名称
     *
     * @return 模型名称
     */
    @Bean
    public String aiModelName() {
        return modelName;
    }
}
