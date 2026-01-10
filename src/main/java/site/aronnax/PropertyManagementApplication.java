package site.aronnax;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * 智慧物业管理系统 - 核心启动类
 * 系统的运行中枢，负责扫描 Spring Bean 容器并挂载 HTTP Server。
 *
 * 背景：本系统聚焦于“人-房-钱”三位一体的数字化管理，内置 AI 决策支持。
 *
 * @author Aronnax (Li Linhan)
 */
@SpringBootApplication
public class PropertyManagementApplication {

    public static void main(String[] args) {
        // 加载 .env 环境变量
        try {
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
            System.out.println("[ENV-LOADER] ✅ .env 文件配置已注入系统属性");
        } catch (Exception e) {
            System.out.println("[ENV-LOADER] ℹ️ 未发现 .env 文件或读取失败，将直接读取系统环境变量");
        }

        // 启动 Spring Boot 微内核
        SpringApplication.run(PropertyManagementApplication.class, args);
    }
}
