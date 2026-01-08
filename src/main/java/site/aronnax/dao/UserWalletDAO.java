package site.aronnax.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import site.aronnax.entity.UserWallet;
import site.aronnax.util.DBUtil;

/**
 * User Wallet Data Access Object
 * Provides CRUD operations for user_wallets table
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class UserWalletDAO {

    /**
     * Create wallet for a user
     *
     * @param wallet UserWallet object
     * @return Generated wallet ID
     */
    public Long insert(UserWallet wallet) {
        String sql = "INSERT INTO user_wallets (user_id, balance, total_recharged) VALUES (?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, wallet.getUserId());
            pstmt.setDouble(2, wallet.getBalance() != null ? wallet.getBalance() : 0.00);
            pstmt.setDouble(3, wallet.getTotalRecharged() != null ? wallet.getTotalRecharged() : 0.00);

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("创建钱包失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Find wallet by user ID
     *
     * @param userId User ID
     * @return UserWallet object or null
     */
    public UserWallet findByUserId(Long userId) {
        String sql = "SELECT * FROM user_wallets WHERE user_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, userId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToWallet(rs);
            }
        } catch (SQLException e) {
            System.err.println("查询钱包失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Find wallet by wallet ID
     *
     * @param walletId Wallet ID
     * @return UserWallet object or null
     */
    public UserWallet findById(Long walletId) {
        String sql = "SELECT * FROM user_wallets WHERE wallet_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, walletId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToWallet(rs);
            }
        } catch (SQLException e) {
            System.err.println("查询钱包失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Update wallet
     *
     * @param wallet UserWallet object
     * @return true if successful
     */
    public boolean update(UserWallet wallet) {
        String sql = "UPDATE user_wallets SET balance=?, total_recharged=? WHERE wallet_id=?";
        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setDouble(1, wallet.getBalance());
            pstmt.setDouble(2, wallet.getTotalRecharged());
            pstmt.setLong(3, wallet.getWalletId());

            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.err.println("更新钱包失败: " + e.getMessage());
            return false;
        } finally {
            closeResources(conn, pstmt, null);
        }
    }

    /**
     * Map ResultSet to UserWallet object
     */
    private UserWallet mapResultSetToWallet(ResultSet rs) throws SQLException {
        UserWallet wallet = new UserWallet();
        wallet.setWalletId(rs.getLong("wallet_id"));
        wallet.setUserId(rs.getLong("user_id"));
        wallet.setBalance(rs.getDouble("balance"));
        wallet.setTotalRecharged(rs.getDouble("total_recharged"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (createdAt != null)
            wallet.setCreatedAt(createdAt.toLocalDateTime());
        if (updatedAt != null)
            wallet.setUpdatedAt(updatedAt.toLocalDateTime());

        return wallet;
    }

    /**
     * Close database resources
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
