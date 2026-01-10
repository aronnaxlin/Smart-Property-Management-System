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
 * 用户数据访问对象 (DAO)
 * 实现对用户档案（业主与管理员）的 CRUD 操作，并提供登录校验及模糊搜索接口。
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class UserDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 用户表行映射器
     * 将数据库记录转换为 User 实体对象，处理了创建时间和更新时间的时间戳转换。
     */
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
     * 插入新用户记录
     * 使用 KeyHolder 模式捕获并返回数据库自动生成的 user_id。
     *
     * @param user 待插入的用户实体
     * @return 数据库生成的自增 ID
     */
    public Long insert(User user) {
        String sql = "INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES (?, ?, ?, ?, ?, ?)";
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

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    /**
     * 根据主键ID物理删除用户
     * 
     * @param userId 目标用户ID
     * @return 是否删除成功
     */
    public boolean deleteById(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId) > 0;
    }

    /**
     * 更新用户档案信息
     * 
     * @param user 包含新信息的实体 (必须包含 userId)
     * @return 是否更新成功
     */
    public boolean update(User user) {
        String sql = "UPDATE users SET user_name=?, password=?, user_type=?, name=?, gender=?, phone=? WHERE user_id=?";
        return jdbcTemplate.update(sql, user.getUserName(), user.getPassword(), user.getUserType(),
                user.getName(), user.getGender(), user.getPhone(), user.getUserId()) > 0;
    }

    /**
     * 根据ID查询单个用户详情
     */
    @SuppressWarnings("null")
    public User findById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userId);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 获取系统中所有注册用户的列表
     */
    @SuppressWarnings("null")
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, userRowMapper);
    }

    /**
     * 根据登录账号 (user_name) 查找用户
     * 
     * @param userName 账号字符串
     */
    @SuppressWarnings("null")
    public User findByUserName(String userName) {
        String sql = "SELECT * FROM users WHERE user_name = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 核心业务：登录凭据验证
     * 通过账号和密码(当前为明文)进行匹配查询。
     *
     * @param userName 登录账号
     * @param password 登录密码
     * @return 匹配成功的用户对象，否则返回 null
     */
    @SuppressWarnings("null")
    public User findByUserNameAndPassword(String userName, String password) {
        String sql = "SELECT * FROM users WHERE user_name = ? AND password = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName, password);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * 模糊搜索用户
     * 用于后台管理搜索框，支持同时按姓名或手机号进行部分匹配 (LIKE)。
     *
     * @param keyword 搜索关键词 (可以是姓名片段或号码片段)
     * @return 符合匹配条件的用户集合
     */
    @SuppressWarnings("null")
    public List<User> searchByKeyword(String keyword) {
        String sql = "SELECT * FROM users WHERE name LIKE ? OR phone LIKE ?";
        String searchPattern = "%" + keyword + "%";
        return jdbcTemplate.query(sql, userRowMapper, searchPattern, searchPattern);
    }
}
