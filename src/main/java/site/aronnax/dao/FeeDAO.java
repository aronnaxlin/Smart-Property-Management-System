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
 * 费用账单数据访问对象 (DAO)
 * 使用 Spring JdbcTemplate 实现对 fees 表的 CRUD 操作及统计分析。
 *
 * @author Aronnax
 */
@Repository
public class FeeDAO {

    private final JdbcTemplate jdbcTemplate;

    public FeeDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 费用表行映射器
     * 将数据库结果集 (ResultSet) 的每一行映射为 Fee 实体对象。
     * 特别注意：处理了 Timestamp 到 LocalDateTime 的转换，并处理了可能为空的时间字段。
     */
    @SuppressWarnings("null")
    private final RowMapper<Fee> feeRowMapper = (rs, rowNum) -> {
        Fee fee = new Fee();
        fee.setfId(rs.getLong("f_id"));
        fee.setpId(rs.getLong("p_id"));
        fee.setFeeType(rs.getString("fee_type"));
        fee.setAmount(rs.getDouble("amount"));
        fee.setIsPaid(rs.getInt("is_paid"));
        fee.setPaymentMethod(rs.getString("payment_method"));

        // 处理缴费时间
        Timestamp payDate = rs.getTimestamp("pay_date");
        if (payDate != null)
            fee.setPayDate(payDate.toLocalDateTime());

        // 处理记录生成时间
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null)
            fee.setCreatedAt(createdAt.toLocalDateTime());

        // 处理最后更新时间
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null)
            fee.setUpdatedAt(updatedAt.toLocalDateTime());

        return fee;
    };

    /**
     * 插入新账单
     * 使用 KeyHolder 获取数据库自动生成的自增主键 (f_id)。
     *
     * @param fee 包含账单信息的对象
     * @return 生成的账单ID
     */
    public Long insert(Fee fee) {
        String sql = "INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, fee.getpId());
            ps.setString(2, fee.getFeeType());
            ps.setDouble(3, fee.getAmount());
            ps.setInt(4, fee.getIsPaid());
            // 如果未指定支付方式，默认为 OFFLINE (线下)
            ps.setString(5, fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "OFFLINE");

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

    /**
     * 更新账单信息
     * 
     * @param fee 要更新的账单对象 (需包含 fId)
     * @return 是否更新成功
     */
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

    /**
     * 根据ID删除账单
     * 
     * @param fId 账单ID
     * @return 是否成功
     */
    public boolean deleteById(Long fId) {
        String sql = "DELETE FROM fees WHERE f_id = ?";
        return jdbcTemplate.update(sql, fId) > 0;
    }

    /**
     * 根据ID查询单笔账单
     */
    @SuppressWarnings("null")
    public Fee findById(Long fId) {
        String sql = "SELECT * FROM fees WHERE f_id = ?";
        List<Fee> list = jdbcTemplate.query(sql, feeRowMapper, fId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取所有账单记录
     */
    @SuppressWarnings("null")
    public List<Fee> findAll() {
        String sql = "SELECT * FROM fees";
        return jdbcTemplate.query(sql, feeRowMapper);
    }

    /**
     * 查询指定房产的所有相关账单
     * 
     * @param pId 房产ID
     */
    @SuppressWarnings("null")
    public List<Fee> findByPropertyId(Long pId) {
        String sql = "SELECT * FROM fees WHERE p_id = ?";
        return jdbcTemplate.query(sql, feeRowMapper, pId);
    }

    /**
     * 查询系统中所有待缴费的账单
     */
    @SuppressWarnings("null")
    public List<Fee> findUnpaidFees() {
        String sql = "SELECT * FROM fees WHERE is_paid = 0";
        return jdbcTemplate.query(sql, feeRowMapper);
    }

    /**
     * 查询指定房产的待缴费账单 (常用于计算欠费金额或拦截充值逻辑)
     */
    @SuppressWarnings("null")
    public List<Fee> findUnpaidByPropertyId(Long propertyId) {
        String sql = "SELECT * FROM fees WHERE p_id = ? AND is_paid = 0";
        return jdbcTemplate.query(sql, feeRowMapper, propertyId);
    }

    /**
     * 统计收费率
     * 计算已缴清的账单占总账单数量的比率。
     * 
     * @return 包含总数、已缴数和比例的 Map
     */
    public Map<String, Object> getCollectionRate() {
        String sqlTotal = "SELECT COUNT(*) FROM fees";
        String sqlPaid = "SELECT COUNT(*) FROM fees WHERE is_paid = 1";
        Integer total = jdbcTemplate.queryForObject(sqlTotal, Integer.class);
        Integer paid = jdbcTemplate.queryForObject(sqlPaid, Integer.class);

        int totalCount = total != null ? total : 0;
        int paidCount = paid != null ? paid : 0;

        Map<String, Object> rate = new HashMap<>();
        rate.put("total", totalCount);
        rate.put("paid", paidCount);
        rate.put("rate", (totalCount == 0) ? 0.0 : (double) paidCount / totalCount);
        return rate;
    }

    /**
     * 获取收入分布图表数据 (按费用类型分组)
     * 仅统计已缴清部分的金额。
     */
    public List<Map<String, Object>> getIncomeDistribution() {
        String sql = "SELECT fee_type, SUM(amount) as total_amount FROM fees WHERE is_paid = 1 GROUP BY fee_type";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 查询欠费严重的楼栋
     * 统计欠费账单数量和金额最高的5个楼栋，用于决策大屏展示及催缴。
     */
    public List<Map<String, Object>> getArrearsByBuilding() {
        String sql = "SELECT p.building_no, COUNT(*) as unpaid_count, SUM(f.amount) as unpaid_amount " +
                "FROM fees f JOIN properties p ON f.p_id = p.p_id " +
                "WHERE f.is_paid = 0 " +
                "GROUP BY p.building_no " +
                "ORDER BY unpaid_count DESC LIMIT 5";
        return jdbcTemplate.queryForList(sql);
    }
}
