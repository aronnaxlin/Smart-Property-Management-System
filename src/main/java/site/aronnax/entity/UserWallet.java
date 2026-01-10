package site.aronnax.entity;

import java.time.LocalDateTime;

/**
 * 用户钱包实体类
 * 对应数据库 user_wallets 表
 * 每个用户拥有一个唯一的钱包，用于存储余额及充值统计。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class UserWallet {

    private Long walletId; // 钱包主键ID
    private Long userId; // 关联的用户ID (与用户表一对一关系)
    private Double balance; // 当前钱包可用余额
    private Double totalRecharged; // 历史充值总额度 (累计值)
    private LocalDateTime createdAt; // 钱包创建时间
    private LocalDateTime updatedAt; // 钱包最近更新时间

    // 无参构造器
    public UserWallet() {
    }

    // 全参构造器
    public UserWallet(Long walletId, Long userId, Double balance, Double totalRecharged,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.totalRecharged = totalRecharged;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }

    public Double getTotalRecharged() {
        return totalRecharged;
    }

    public void setTotalRecharged(Double totalRecharged) {
        this.totalRecharged = totalRecharged;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserWallet{" +
                "walletId=" + walletId +
                ", userId=" + userId +
                ", balance=" + balance +
                ", totalRecharged=" + totalRecharged +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
