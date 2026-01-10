package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.UserWallet;

/**
 * 用户钱包数据访问对象 (DAO)
 * 管理用户的资金账户，支持查询余额和余额变动更新。
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class UserWalletDAO {

    private final JdbcTemplate jdbcTemplate;

    public UserWalletDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 钱包表行映射器
     */
    @SuppressWarnings("null")
    private final RowMapper<UserWallet> walletRowMapper = (rs, rowNum) -> {
        UserWallet wallet = new UserWallet();
        wallet.setWalletId(rs.getLong("wallet_id"));
        wallet.setUserId(rs.getLong("user_id"));
        wallet.setBalance(rs.getDouble("balance"));
        wallet.setTotalRecharged(rs.getDouble("total_recharged"));
        if (rs.getTimestamp("created_at") != null)
            wallet.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        if (rs.getTimestamp("updated_at") != null)
            wallet.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return wallet;
    };

    /**
     * 初始化新钱包记录 (通常在用户注册时调用)
     */
    public Long insert(UserWallet wallet) {
        String sql = "INSERT INTO user_wallets (user_id, balance, total_recharged) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, wallet.getUserId(), wallet.getBalance(), wallet.getTotalRecharged());
        return null;
    }

    /**
     * 根据用户(User)ID查找钱包明细
     * 
     * @param userId 所属用户ID
     */
    @SuppressWarnings("null")
    public UserWallet findByUserId(Long userId) {
        String sql = "SELECT * FROM user_wallets WHERE user_id = ?";
        List<UserWallet> list = jdbcTemplate.query(sql, walletRowMapper, userId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 根据钱包主键查询
     */
    @SuppressWarnings("null")
    public UserWallet findById(Long walletId) {
        String sql = "SELECT * FROM user_wallets WHERE wallet_id = ?";
        List<UserWallet> list = jdbcTemplate.query(sql, walletRowMapper, walletId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 更新钱包余额及其累计充值额度
     */
    public boolean update(UserWallet wallet) {
        String sql = "UPDATE user_wallets SET balance=?, total_recharged=? WHERE wallet_id=?";
        return jdbcTemplate.update(sql, wallet.getBalance(), wallet.getTotalRecharged(), wallet.getWalletId()) > 0;
    }
}
