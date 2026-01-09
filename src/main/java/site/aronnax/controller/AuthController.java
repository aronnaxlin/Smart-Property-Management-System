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

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserDAO userDAO;

    public AuthController(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * 用户登录接口
     * 验证用户名和密码，成功后将用户信息存入Session
     *
     * @param loginRequest 登录请求对象，包含username和password
     * @param session      HTTP会话对象，用于存储登录状态
     * @return Result包装的User对象，登录失败时返回错误信息
     */
    @PostMapping("/login")
    public Result<User> login(@RequestBody LoginRequest loginRequest, HttpSession session) {
        // 输入验证：检查用户名是否为空
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }

        // 输入验证：检查密码是否为空
        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }

        // 输入验证：用户名长度检查（防止恶意超长输入）
        if (loginRequest.getUsername().length() > 50) {
            return Result.error("用户名格式不正确");
        }

        // 数据库查询：通过用户名和密码查找用户
        // 注意：JdbcTemplate已使用PreparedStatement，可防止SQL注入
        // TODO: 未来应使用BCrypt等加密方式存储密码，而非明文
        User user = userDAO.findByUserNameAndPassword(
                loginRequest.getUsername().trim(),
                loginRequest.getPassword());

        // 登录成功：将用户信息存入Session
        if (user != null) {
            session.setAttribute("user", user);
            return Result.success(user);
        }

        // 登录失败：返回统一的错误消息（避免泄露用户是否存在）
        return Result.error("用户名或密码错误");
    }

    /**
     * 用户登出接口
     * 清除当前Session，结束用户登录状态
     *
     * @param session HTTP会话对象
     * @return 成功消息
     */
    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        // 销毁当前Session，清除所有登录状态
        session.invalidate();
        return Result.success("登出成功");
    }
}
