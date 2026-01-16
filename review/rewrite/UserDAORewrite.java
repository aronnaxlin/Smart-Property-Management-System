package site.aronnax.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import java.util.List;

import site.aronnax.entity.User;

@Repository
public class UserDAORewrite {
    private final JdbcTemplate jdbcTemplate;

    public UserDAORewrite(JdbcTemplate jdbcTemplate) {
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

    public Long insert(User user) {
        String sql = "INSERT INTO users (user_name, password, user_type, name, gender, phone) VALUES (?,?,?,?,?,?)";
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

    public boolean deleteById(Long userId) {
        String sql = "delete from users where user_id = ?";
        return jdbcTemplate.update(sql, userId) > 0;
    }

    public boolean updateUserInfo(User user) {
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            String sql = "UPDATE users SET user_name = ?, user_type = ?, name = ?, phone = ? where user_id = ?";
            return jdbcTemplate.update(sql, user.getUserName(), user.getUserType(), user.getName(), user.getPhone(),
                    user.getUserId()) > 0;
        } else {
            String sql = "UPDATE users SET user_name=?, password=?, user_type=?, name=?, phone=? WHERE user_id=?";
            return jdbcTemplate.update(sql,
                    user.getUserName(),
                    user.getPassword(),
                    user.getUserType(),
                    user.getName(),
                    user.getPhone(),
                    user.getUserId()) > 0;
        }
    }

    public User findByUserName(String userName) {
        String sql = "SELECT * FROM users WHERE user_name = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName);
        return users.isEmpty() ? null : users.get(0);
    }

    @SuppressWarnings("null")
    public User findByUserNameAndPassword(String userName, String password) {
        String sql = "SELECT * FROM users WHERE user_name = ? AND password = ?";
        List<User> users = jdbcTemplate.query(sql, userRowMapper, userName, password);
        return users.isEmpty() ? null : users.get(0);
    }

}
