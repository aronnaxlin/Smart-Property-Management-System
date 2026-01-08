package site.aronnax.entity;

import java.time.LocalDateTime;

/**
 * Wallet Transaction Entity
 * Corresponds to wallet_transactions table
 * Records all wallet transaction history
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class WalletTransaction {

    private Long transId; // Transaction ID
    private Long walletId; // Wallet ID
    private String transType; // Transaction type: RECHARGE, PAY_FEE, TOPUP_CARD
    private Double amount; // Transaction amount
    private Double balanceAfter; // Balance after transaction
    private Long relatedId; // Related ID (fee_id or card_id)
    private String description; // Transaction description
    private LocalDateTime transTime; // Transaction time

    // No-arg constructor
    public WalletTransaction() {
    }

    // Full constructor
    public WalletTransaction(Long transId, Long walletId, String transType, Double amount,
            Double balanceAfter, Long relatedId, String description, LocalDateTime transTime) {
        this.transId = transId;
        this.walletId = walletId;
        this.transType = transType;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.relatedId = relatedId;
        this.description = description;
        this.transTime = transTime;
    }

    // Getters and Setters
    public Long getTransId() {
        return transId;
    }

    public void setTransId(Long transId) {
        this.transId = transId;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(Long relatedId) {
        this.relatedId = relatedId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTransTime() {
        return transTime;
    }

    public void setTransTime(LocalDateTime transTime) {
        this.transTime = transTime;
    }

    @Override
    public String toString() {
        return "WalletTransaction{" +
                "transId=" + transId +
                ", walletId=" + walletId +
                ", transType='" + transType + '\'' +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", relatedId=" + relatedId +
                ", description='" + description + '\'' +
                ", transTime=" + transTime +
                '}';
    }
}
