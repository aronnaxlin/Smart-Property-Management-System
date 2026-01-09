package site.aronnax.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.User;

/**
 * 用户数据访问对象 (Data Access Object)
 * 使用 Spring JdbcTemplate 实现
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("null")
    private final RowMapper<User> userRowMapper = (rs, rowNum) -> {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUserName(rs.getString("user_name"));
        user.setPassword(rs.getString("password"));
        user.setUserType(rs.getString("user_type"));
        user.setName(rs.getString("name"));
        user.setGender(rs.getString("gender"));
        user.setPhone(rs.getString("phone"));
        if (rs.getTimestamp("created_at") != null) {
            user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        }
        if (rs.getTimestamp("updated_at") != null) {
            user.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        }
        return user;
    };

    /**
     * 插入新用户
     *
     * @param user 用户对象
     * @return 生成的用户ID
     */
    public Long insert(User user) {
        String sql = "INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES (?, ?, ?, ?, ?, ?)";

        // 使用KeyHolder获取自动生成的主键
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getUserName());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getUserType());
            ps.setString(4, user.getName());
            ps.setString(5, user.getGender());
            ps.setString(6, user.getPhone());
            return ps;
        }, keyHolder);

        // 返回生成的ID
        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    /**
     * 删除用户
     *
     * @param userId 用户ID
     * @return 是否删除成功
     */
    public boolean deleteById(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId) > 0;
    }

    /**
     * 更新用户信息
     *
     * @param user 用户对象
     * @return 是否更新成功
     */
    public boolean update(User user) {
        String sql = "UPDATE users SET user_name=?, password=?, user_type=?, name=?, gender=?, phone=? WHERE user_id=?";
        return jdbcTemplate.update(sql, user.getUserName(), user.getPassword(), user.getUserType(),
                user.getName(), user.getGender(), user.getPhone(), user.getUserId()) > 0;
    }

    /**
     * 根据ID查询用户
     *
     * @param userId 用户ID
     * @return 用户对象，不存在时返回null
     */
    public User findById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userId);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 查询所有用户
     *
     * @return 用户列表
     */
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    /**
     * 根据用户名查询用户
     *
     * @param userName 用户名
     * @return 用户对象，不存在时返回null
     */
    public User findByUserName(String userName) {
        String sql = "SELECT * FROM users WHERE user_name = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 登录验证：根据用户名和密码查询用户
     *
     * 注意：此方法使用PreparedStatement，已防止SQL注入
     * TODO: 未来应使用加密密码而非明文比对
     *
     * @param userName 用户名
     * @param password 密码（明文）
     * @return 用户对象，验证失败时返回null
     */
    public User findByUserNameAndPassword(String userName, String password) {
        String sql = "SELECT * FROM users WHERE user_name = ? AND password = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName, password);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 模糊搜索用户
     * 支持按姓名或电话号码搜索
     *
     * @param keyword 搜索关键词
     * @return 符合条件的用户列表
     */
    public List<User> searchByKeyword(String keyword) {
        String sql = "SELECT * FROM users WHERE name LIKE ? OR phone LIKE ?";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, userRowMapper, searchPattern, searchPattern);
    }
}
