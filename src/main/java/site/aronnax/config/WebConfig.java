package site.aronnax.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 通用 Web 拦截与跨域配置
 * 定义了前后端分离架构下的基础通信规则。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置全域跨域访问控制 (CORS)
     * 允许前端开发服务器及生产环境跨域调用 REST 接口。
     */
    @Override
    public void addCorsMappings(@org.springframework.lang.NonNull CorsRegistry registry) {
        // 开发模式下允许所有来源，并放行主流的 HTTP 方法
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
