package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.WalletTransaction;

/**
 * 钱包交易流水数据访问对象 (DAO)
 * 实现交易流水的记录与查询。该类仅支持插入和查询，流水记录通常不允许修改或删除，以保证账务安全性。
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class WalletTransactionDAO {

    private final JdbcTemplate jdbcTemplate;

    public WalletTransactionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 交易流水表行映射器
     * 处理 related_id 时通过 wasNull() 特别判断了空值。
     */
    @SuppressWarnings("null")
    private final RowMapper<WalletTransaction> transactionRowMapper = (rs, rowNum) -> {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setTransId(rs.getLong("trans_id"));
        transaction.setWalletId(rs.getLong("wallet_id"));
        transaction.setTransType(rs.getString("trans_type"));
        transaction.setAmount(rs.getDouble("amount"));
        transaction.setBalanceAfter(rs.getDouble("balance_after"));
        long relatedId = rs.getLong("related_id");
        if (!rs.wasNull()) {
            transaction.setRelatedId(relatedId); // 关联业务ID (如账单 f_id 或 水电卡 card_id)
        }
        transaction.setDescription(rs.getString("description"));
        if (rs.getTimestamp("trans_time") != null) {
            transaction.setTransTime(rs.getTimestamp("trans_time").toLocalDateTime());
        }
        return transaction;
    };

    /**
     * 插入新的交易记录
     * 记录每一笔资金的出账与增项。
     */
    public Long insert(WalletTransaction transaction) {
        String sql = "INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, transaction.getWalletId(), transaction.getTransType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getRelatedId(), transaction.getDescription());
        return null;
    }

    /**
     * 按钱包ID查询所有历史流水，按时间倒序排列（最近的交易在前）。
     * 
     * @param walletId 钱包ID
     */
    @SuppressWarnings("null")
    public List<WalletTransaction> findByWalletId(Long walletId) {
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY trans_time DESC";
        return jdbcTemplate.query(sql, transactionRowMapper, walletId);
    }

    /**
     * 根据流水主键查询详情
     */
    @SuppressWarnings("null")
    public WalletTransaction findById(Long transId) {
        String sql = "SELECT * FROM wallet_transactions WHERE trans_id = ?";
        List<WalletTransaction> list = jdbcTemplate.query(sql, transactionRowMapper, transId);
        return list.isEmpty() ? null : list.get(0);
    }
}
