package site.aronnax.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import site.aronnax.entity.Fee;
import site.aronnax.util.DBUtil;

/**
 * 费用账单数据访问对象 (Data Access Object)
 * 提供对 fees 表的 CRUD 操作
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class FeeDAO {

    /**
     * 插入新账单
     *
     * @param fee 费用对象
     * @return 插入成功返回生成的主键ID，失败返回null
     */
    public Long insert(Fee fee) {
        String sql = "INSERT INTO fees (p_id, fee_type, amount, is_paid, payment_method, pay_date) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, fee.getpId());
            pstmt.setString(2, fee.getFeeType());
            pstmt.setDouble(3, fee.getAmount());
            pstmt.setInt(4, fee.getIsPaid());
            pstmt.setString(5, fee.getPaymentMethod() != null ? fee.getPaymentMethod() : "WALLET");

            if (fee.getPayDate() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(fee.getPayDate()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("插入账单失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * 根据ID删除账单
     *
     * @param fId 账单ID
     * @return 删除成功返回true
     */
    public boolean deleteById(Long fId) {
        String sql = "DELETE FROM fees WHERE f_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, fId);

            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("删除账单失败: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 更新账单信息
     *
     * @param fee 费用对象（需包含fId）
     * @return 更新成功返回true
     */
    public boolean update(Fee fee) {
        String sql = "UPDATE fees SET p_id=?, fee_type=?, amount=?, is_paid=?, payment_method=?, pay_date=? WHERE f_id=?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, fee.getpId());
            pstmt.setString(2, fee.getFeeType());
            pstmt.setDouble(3, fee.getAmount());
            pstmt.setInt(4, fee.getIsPaid());
            pstmt.setString(5, fee.getPaymentMethod());

            if (fee.getPayDate() != null) {
                pstmt.setTimestamp(6, Timestamp.valueOf(fee.getPayDate()));
            } else {
                pstmt.setNull(6, Types.TIMESTAMP);
            }

            pstmt.setLong(7, fee.getfId());

            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("更新账单失败: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * 根据ID查询账单
     *
     * @param fId 账单ID
     * @return Fee对象，不存在则返回null
     */
    public Fee findById(Long fId) {
        String sql = "SELECT * FROM fees WHERE f_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, fId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToFee(rs);
            }
        } catch (SQLException e) {
            System.err.println("查询账单失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * 查询所有账单
     *
     * @return 账单列表
     */
    public List<Fee> findAll() {
        String sql = "SELECT * FROM fees";
        List<Fee> fees = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                fees.add(mapResultSetToFee(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询所有账单失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return fees;
    }

    /**
     * 根据房产ID查询账单列表
     *
     * @param pId 房产ID
     * @return 账单列表
     */
    public List<Fee> findByPropertyId(Long pId) {
        String sql = "SELECT * FROM fees WHERE p_id = ?";
        List<Fee> fees = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, pId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                fees.add(mapResultSetToFee(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询房产账单失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return fees;
    }

    /**
     * Find all unpaid fees
     *
     * @return List of unpaid fees
     */
    public List<Fee> findUnpaidFees() {
        String sql = "SELECT * FROM fees WHERE is_paid = 0";
        List<Fee> fees = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                fees.add(mapResultSetToFee(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询未缴费用失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return fees;
    }

    /**
     * Find unpaid fees for a specific property
     *
     * @param propertyId Property ID
     * @return List of unpaid fees
     */
    public List<Fee> findUnpaidByPropertyId(Long propertyId) {
        String sql = "SELECT * FROM fees WHERE p_id = ? AND is_paid = 0";
        List<Fee> fees = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, propertyId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                fees.add(mapResultSetToFee(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询房产未缴费用失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return fees;
    }

    /**
     * 将ResultSet映射为Fee对象
     */
    private Fee mapResultSetToFee(ResultSet rs) throws SQLException {
        Fee fee = new Fee();
        fee.setfId(rs.getLong("f_id"));
        fee.setpId(rs.getLong("p_id"));
        fee.setFeeType(rs.getString("fee_type"));
        fee.setAmount(rs.getDouble("amount"));
        fee.setIsPaid(rs.getInt("is_paid"));
        fee.setPaymentMethod(rs.getString("payment_method"));

        Timestamp payDate = rs.getTimestamp("pay_date");
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");

        if (payDate != null)
            fee.setPayDate(payDate.toLocalDateTime());
        if (createdAt != null)
            fee.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null)
            fee.setUpdatedAt(updatedAt.toLocalDateTime());

        return fee;
    }

    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, PreparedStatement pstmt, ResultSet rs) {
        try {
            if (rs != null)
                rs.close();
            if (pstmt != null)
                pstmt.close();
            if (conn != null)
                conn.close();
        } catch (SQLException e) {
            System.err.println("关闭资源失败: " + e.getMessage());
        }
    }
}
