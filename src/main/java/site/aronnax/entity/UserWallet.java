package site.aronnax.entity;

import java.time.LocalDateTime;

/**
 * User Wallet Entity
 * Corresponds to user_wallets table
 * One wallet per user
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class UserWallet {

    private Long walletId; // Primary key
    private Long userId; // User ID (one-to-one relationship)
    private Double balance; // Current balance
    private Double totalRecharged; // Total amount ever recharged
    private LocalDateTime createdAt; // Creation time
    private LocalDateTime updatedAt; // Update time

    // No-arg constructor
    public UserWallet() {
    }

    // Full constructor
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
