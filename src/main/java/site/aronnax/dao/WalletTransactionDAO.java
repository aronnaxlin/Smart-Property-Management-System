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

import site.aronnax.entity.WalletTransaction;
import site.aronnax.util.DBUtil;

/**
 * Wallet Transaction Data Access Object
 * Provides operations for wallet_transactions table
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class WalletTransactionDAO {

    /**
     * Insert transaction record
     *
     * @param transaction WalletTransaction object
     * @return Generated transaction ID
     */
    public Long insert(WalletTransaction transaction) {
        String sql = "INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, transaction.getWalletId());
            pstmt.setString(2, transaction.getTransType());
            pstmt.setDouble(3, transaction.getAmount());
            pstmt.setDouble(4, transaction.getBalanceAfter());

            if (transaction.getRelatedId() != null) {
                pstmt.setLong(5, transaction.getRelatedId());
            } else {
                pstmt.setNull(5, Types.BIGINT);
            }

            pstmt.setString(6, transaction.getDescription());

            int affected = pstmt.executeUpdate();
            if (affected > 0) {
                rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("记录交易失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Find transactions by wallet ID
     *
     * @param walletId Wallet ID
     * @return List of transactions
     */
    public List<WalletTransaction> findByWalletId(Long walletId) {
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY trans_time DESC";
        List<WalletTransaction> transactions = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, walletId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                transactions.add(mapResultSetToTransaction(rs));
            }
        } catch (SQLException e) {
            System.err.println("查询交易记录失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return transactions;
    }

    /**
     * Find transaction by ID
     *
     * @param transId Transaction ID
     * @return WalletTransaction object or null
     */
    public WalletTransaction findById(Long transId) {
        String sql = "SELECT * FROM wallet_transactions WHERE trans_id = ?";
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DBUtil.getConnection();
            pstmt = conn.prepareStatement(sql);
            pstmt.setLong(1, transId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapResultSetToTransaction(rs);
            }
        } catch (SQLException e) {
            System.err.println("查询交易记录失败: " + e.getMessage());
        } finally {
            closeResources(conn, pstmt, rs);
        }
        return null;
    }

    /**
     * Map ResultSet to WalletTransaction object
     */
    private WalletTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransId(rs.getLong("trans_id"));
        transaction.setWalletId(rs.getLong("wallet_id"));
        transaction.setTransType(rs.getString("trans_type"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setBalanceAfter(rs.getDouble("balance_after"));

        long relatedId = rs.getLong("related_id");
        if (!rs.wasNull()) {
            transaction.setRelatedId(relatedId);
        }

        transaction.setDescription(rs.getString("description"));

        Timestamp transTime = rs.getTimestamp("trans_time");
        if (transTime != null)
            transaction.setTransTime(transTime.toLocalDateTime());

        return transaction;
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
