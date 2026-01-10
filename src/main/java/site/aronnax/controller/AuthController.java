package site.aronnax.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import site.aronnax.common.Result;
import site.aronnax.dao.UserDAO;
import site.aronnax.dto.LoginRequest;
import site.aronnax.entity.User;

/**
 * 身份认证控制器
 * 负责系统的登录、权限校验预检及登出逻辑。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserDAO userDAO;

    public AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * 用户统一登录入口
     *
     * 逻辑流程：
     * 1. 基础非空校验 -> 2. 字段规范校验（长度限制） -> 3. 数据库凭据比对 -> 4. Session 状态持久化。
     *
     * @param loginRequest 包含 username 和 password 的 DTO
     * @param session      用于记录登录态的 HTTP 会话
     * @return 登录用户实体（脱敏后）或错误信息
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        // 第一阶段：输入完整性守卫
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            return Result.error("请输入您的用户名");
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return Result.error("请输入登录密码");
        }

        // 第二阶段：参数规范拦截（防御性编程，防止超长字符攻击）
        if (loginRequest.getUsername().length() > 50) {
            return Result.error("用户名格式非法");
        }

        // 第三阶段：凭据校验
        // 安全提示：PreparedStatement 已由 JdbcTemplate 自动处理，可防御 SQL 注入。
        // [安全演进建议]：当前演示版使用明文对比，生产环境中应采用 BCrypt 强盐哈希算法。
        User user = userDAO.findByUserNameAndPassword(
                loginRequest.getUsername().trim(),
                loginRequest.getPassword());

        // 第四阶段：会话管理
        if (user != null) {
            // 将用户对象注入 Session，开启会话生命周期
            session.setAttribute("user", user);
            return Result.success(user);
        }

        // 统一提示：登录失败时不透露是“用户名不存在”还是“密码错误”，提高安全性
        return Result.error("用户名或登录密码不匹配");
    }

    /**
     * 安全登出
     * 立即销毁服务器端会话，确保账户安全。
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        session.invalidate();
        return Result.success("您已安全退出系统");
    }
}
