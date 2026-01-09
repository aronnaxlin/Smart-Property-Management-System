package site.aronnax.dao;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.Fee;

/**
 * Fee DAO Implementation using Spring JdbcTemplate
 *
 * @author Aronnax
 */
@Repository
public class FeeDAO {

    private final JdbcTemplate jdbcTemplate;

    public FeeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("null")
    private final RowMapper<Fee> feeRowMapper = (rs, rowNum) -> {
        Fee fee = new Fee();
        fee.setfId(rs.getLong("f_id"));
        fee.setpId(rs.getLong("p_id"));
        fee.setFeeType(rs.getString("fee_type"));
        fee.setAmount(rs.getDouble("amount"));
        fee.setIsPaid(rs.getInt("is_paid"));
        fee.setPaymentMethod(rs.getString("payment_method"));

        Timestamp payDate = rs.getTimestamp("pay_date");
        if (payDate != null)
            fee.setPayDate(payDate.toLocalDateTime());

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null)
            fee.setCreatedAt(createdAt.toLocalDateTime());

        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null)
            fee.setUpdatedAt(updatedAt.toLocalDateTime());

        return fee;
    };

    public Long insert(Fee fee) {
        String sql = "INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, fee.getpId());
            ps.setString(2, fee.getFeeType());
            ps.setDouble(3, fee.getAmount());
            ps.setInt(4, fee.getIsPaid());
            ps.setString(5, fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "OFFLINE"); // Default or given

            if (fee.getPayDate() != null) {
                ps.setTimestamp(6, Timestamp.valueOf(fee.getPayDate()));
            } else {
                ps.setNull(6, Types.TIMESTAMP);
            }
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key != null ? key.longValue() : null;
    }

    public boolean update(Fee fee) {
        String sql = "UPDATE fees SET p_id=?, fee_type=?, amount=?, is_paid=?, payment_method=?, pay_date=? WHERE f_id=?";
        return jdbcTemplate.update(sql,
                fee.getpId(),
                fee.getFeeType(),
                fee.getAmount(),
                fee.getIsPaid(),
                fee.getPaymentMethod(),
                fee.getPayDate() != null ? Timestamp.valueOf(fee.getPayDate()) : null,
                fee.getfId()) > 0;
    }

    public boolean deleteById(Long fId) {
        String sql = "DELETE FROM fees WHERE f_id = ?";
        return jdbcTemplate.update(sql, fId) > 0;
    }

    public Fee findById(Long fId) {
        String sql = "SELECT * FROM fees WHERE f_id = ?";
        List<Fee> list = jdbcTemplate.query(sql, feeRowMapper, fId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Fee> findAll() {
        String sql = "SELECT * FROM fees";
        return jdbcTemplate.query(sql, feeRowMapper);
    }

    public List<Fee> findByPropertyId(Long pId) {
        String sql = "SELECT * FROM fees WHERE p_id = ?";
        return jdbcTemplate.query(sql, feeRowMapper, pId);
    }

    public List<Fee> findUnpaidFees() {
        String sql = "SELECT * FROM fees WHERE is_paid = 0";
        return jdbcTemplate.query(sql, feeRowMapper);
    }

    public List<Fee> findUnpaidByPropertyId(Long propertyId) {
        String sql = "SELECT * FROM fees WHERE p_id = ? AND is_paid = 0";
        return jdbcTemplate.query(sql, feeRowMapper, propertyId);
    }

    // Statistics
    public Map<String, Object> getCollectionRate() {
        String sqlTotal = "SELECT COUNT(*) FROM fees";
        String sqlPaid = "SELECT COUNT(*) FROM fees WHERE is_paid = 1";
        Integer total = jdbcTemplate.queryForObject(sqlTotal, Integer.class);
        Integer paid = jdbcTemplate.queryForObject(sqlPaid, Integer.class);

        // Handle nulls
        int totalCount = total != null ? total : 0;
        int paidCount = paid != null ? paid : 0;

        Map<String, Object> rate = new HashMap<>();
        rate.put("total", totalCount);
        rate.put("paid", paidCount);
        rate.put("rate", (totalCount == 0) ? 0.0 : (double) paidCount / totalCount);
        return rate;
    }

    public List<Map<String, Object>> getIncomeDistribution() {
        String sql = "SELECT fee_type, SUM(amount) as total_amount FROM fees WHERE is_paid = 1 GROUP BY fee_type";
        return jdbcTemplate.queryForList(sql);
    }

    public List<Map<String, Object>> getArrearsByBuilding() {
        String sql = "SELECT p.building_no, COUNT(*) as unpaid_count, SUM(f.amount) as unpaid_amount " +
                "FROM fees f JOIN properties p ON f.p_id = p.p_id " +
                "WHERE f.is_paid = 0 " +
                "GROUP BY p.building_no " +
                "ORDER BY unpaid_count DESC LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }
}
