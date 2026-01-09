package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.WalletTransaction;

/**
 * Wallet Transaction Data Access Object
 * Implemented with Spring JdbcTemplate
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class WalletTransactionDAO {

    private final JdbcTemplate jdbcTemplate;

    public WalletTransactionDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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
            transaction.setRelatedId(relatedId);
        }
        transaction.setDescription(rs.getString("description"));
        if (rs.getTimestamp("trans_time") != null) {
            transaction.setTransTime(rs.getTimestamp("trans_time").toLocalDateTime());
        }
        return transaction;
    };

    public Long insert(WalletTransaction transaction) {
        String sql = "INSERT INTO wallet_transactions (wallet_id, trans_type, amount, balance_after, related_id, description) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, transaction.getWalletId(), transaction.getTransType(), transaction.getAmount(),
                transaction.getBalanceAfter(), transaction.getRelatedId(), transaction.getDescription());
        return null; // ID retrieval omitted
    }

    public List<WalletTransaction> findByWalletId(Long walletId) {
        String sql = "SELECT * FROM wallet_transactions WHERE wallet_id = ? ORDER BY trans_time DESC";
        return jdbcTemplate.query(sql, transactionRowMapper, walletId);
    }

    public WalletTransaction findById(Long transId) {
        String sql = "SELECT * FROM wallet_transactions WHERE trans_id = ?";
        List<WalletTransaction> list = jdbcTemplate.query(sql, transactionRowMapper, transId);
        return list.isEmpty() ? null : list.get(0);
    }
}
