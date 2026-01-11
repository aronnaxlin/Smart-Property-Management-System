package site.aronnax.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.dao.UserDAO;
import site.aronnax.entity.User;
import site.aronnax.service.OwnerService;

/**
 * 用户与业主统一管理控制器
 * 整合了用户CRUD和业主房产管理功能，面向管理端操作。
 *
 * 路由规则:
 * - /api/user/* - 用户CRUD操作
 * - /api/owner/* - 业主房产相关操作
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
public class UserOwnerController {

    private final UserDAO userDAO;
    private final OwnerService ownerService;

    public UserOwnerController(UserDAO userDAO, OwnerService ownerService) {
        this.userDAO = userDAO;
        this.ownerService = ownerService;
    }

    // ========== 用户管理接口 (/api/user/*) ==========

    /**
     * 获取所有用户列表
     *
     * @return 用户列表
     */
    @GetMapping("/api/user/list")
    public Result<List<User>> getAllUsers() {
        try {
            List<User> users = userDAO.findAll();
            return Result.success(users != null ? users : List.of());
        } catch (Exception e) {
            return Result.error("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取单个用户详情
     *
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/api/user/{id}")
    public Result<User> getUserById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("用户ID无效");
        }

        try {
            User user = userDAO.findById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }
            return Result.success(user);
        } catch (Exception e) {
            return Result.error("获取用户详情失败：" + e.getMessage());
        }
    }

    /**
     * 搜索用户
     * 支持按姓名或手机号模糊搜索
     *
     * @param keyword 搜索关键词
     * @return 匹配的用户列表
     */
    @GetMapping("/api/user/search")
    public Result<List<User>> searchUsers(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {

        // 防御性编程：确保 keyword 永远不为 null
        if (keyword == null) {
            keyword = "";
        }

        // 防止恶意超长输入
        if (keyword.length() > 50) {
            return Result.error("搜索关键词超出长度限制");
        }

        try {
            List<User> users = keyword.isEmpty() ? userDAO.findAll() : userDAO.searchByKeyword(keyword);
            return Result.success(users != null ? users : List.of());
        } catch (Exception e) {
            return Result.error("搜索用户失败：" + e.getMessage());
        }
    }

    /**
     * 创建新用户
     *
     * @param user 用户信息
     * @return 创建的用户ID
     */
    @PostMapping("/api/user/create")
    public Result<Long> createUser(@RequestBody User user) {
        // 参数校验
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            return Result.error("密码不能为空");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return Result.error("姓名不能为空");
        }
        if (user.getUserType() == null || user.getUserType().trim().isEmpty()) {
            return Result.error("用户类型不能为空");
        }

        // 检查用户名是否已存在
        try {
            User existingUser = userDAO.findByUserName(user.getUserName());
            if (existingUser != null) {
                return Result.error("用户名已存在，请使用其他用户名");
            }

            // 插入新用户
            Long userId = userDAO.insert(user);
            if (userId != null && userId > 0) {
                return Result.success(userId);
            }
            return Result.error("创建用户失败");
        } catch (Exception e) {
            return Result.error("创建用户失败：" + e.getMessage());
        }
    }

    /**
     * 更新用户信息
     *
     * @param user 用户信息（必须包含userId）
     * @return 是否成功
     */
    @PutMapping("/api/user/update")
    public Result<String> updateUser(@RequestBody User user) {
        // 参数校验
        if (user.getUserId() == null || user.getUserId() <= 0) {
            return Result.error("用户ID无效");
        }
        if (user.getUserName() == null || user.getUserName().trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            return Result.error("姓名不能为空");
        }

        try {
            // 检查用户是否存在
            User existingUser = userDAO.findById(user.getUserId());
            if (existingUser == null) {
                return Result.error("用户不存在");
            }

            // 如果修改了用户名，检查新用户名是否已被其他用户使用
            if (!existingUser.getUserName().equals(user.getUserName())) {
                User userWithSameName = userDAO.findByUserName(user.getUserName());
                if (userWithSameName != null && !userWithSameName.getUserId().equals(user.getUserId())) {
                    return Result.error("用户名已被其他用户使用");
                }
            }

            // 执行更新
            boolean success = userDAO.update(user);
            if (success) {
                return Result.success("更新成功");
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            return Result.error("更新用户失败：" + e.getMessage());
        }
    }

    /**
     * 删除用户
     *
     * @param id 用户ID
     * @return 是否成功
     */
    @DeleteMapping("/api/user/{id}")
    public Result<String> deleteUser(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("用户ID无效");
        }

        try {
            // 检查用户是否存在
            User user = userDAO.findById(id);
            if (user == null) {
                return Result.error("用户不存在");
            }

            // 执行删除
            boolean success = userDAO.deleteById(id);
            if (success) {
                return Result.success("删除成功");
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除用户失败：" + e.getMessage());
        }
    }

    // ========== 业主管理接口 (/api/owner/*) ==========

    /**
     * 条件搜索业主
     * 支持姓名、手机号等模糊匹配。
     *
     * @param keyword 检索关键字
     */
    @GetMapping("/api/owner/search")
    public Result<List<Map<String, Object>>> searchOwners(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        // 防御性编程：确保 keyword 永远不为 null
        if (keyword == null) {
            keyword = "";
        }

        // 安全前置：限制关键字长度，防止扫描探测
        if (keyword.length() > 50) {
            return Result.error("搜索词超出长度限制");
        }

        try {
            List<Map<String, Object>> results = ownerService.searchOwners(keyword);
            return Result.success(results != null ? results : List.of());
        } catch (Exception e) {
            return Result.error("检索链路异常：" + e.getMessage());
        }
    }

    /**
     * 业主档案详情
     * 返回业主基本资料及其名下的资产配置（房产列表）。
     *
     * @param id 用户唯一标识
     */
    @GetMapping("/api/owner/{id}")
    public Result<Map<String, Object>> getOwnerDetail(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("业主 ID 获取失败");
        }

        try {
            Map<String, Object> ownerDetail = ownerService.getOwnerWithProperties(id);

            if (ownerDetail == null || ownerDetail.isEmpty()) {
                return Result.error("该业主档案不存在或已被注销");
            }

            return Result.success(ownerDetail);
        } catch (Exception e) {
            return Result.error("详情加载失败：" + e.getMessage());
        }
    }

    /**
     * 房产过户业务
     * 变更房产档案的归属人。
     *
     * @param propertyId 目标房产 ID
     * @param newOwnerId 接收方业主 ID
     */
    @PostMapping("/api/owner/property/transfer")
    public Result<String> transferProperty(@RequestParam("propertyId") Long propertyId,
            @RequestParam("newOwnerId") Long newOwnerId) {
        if (propertyId == null || propertyId <= 0) {
            return Result.error("请选择有效的房产");
        }

        if (newOwnerId == null || newOwnerId <= 0) {
            return Result.error("请选择有效的新业主");
        }

        try {
            // 执行业务变更
            boolean success = ownerService.updatePropertyOwner(propertyId, newOwnerId);

            if (success) {
                return Result.success("房产权属变更已生效");
            }

            return Result.error("过户失败：系统核验不通过，请确认人员与房产状态");
        } catch (Exception e) {
            return Result.error("服务端处理异常：" + e.getMessage());
        }
    }
}
