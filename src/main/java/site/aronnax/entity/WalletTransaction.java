package site.aronnax.entity;

import java.time.LocalDateTime;

/**
 * 钱包交易流水实体类
 * 对应数据库 wallet_transactions 表
 * 用于详细记录钱包的所有收支行为，如充值、缴纳物业费、充值水电卡等。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class WalletTransaction {

    private Long transId; // 交易流水号 (主键)
    private Long walletId; // 关联的钱包ID
    private String transType; // 交易类型: RECHARGE(充值), PAY_FEE(缴费), TOPUP_CARD(购水电卡)
    private Double amount; // 交易金额
    private Double balanceAfter; // 交易后钱包余额 (用于对账)
    private Long relatedId; // 业务关联ID (如关联的账单 f_id 或 水电卡 card_id)
    private String description; // 交易备注说明
    private LocalDateTime transTime; // 交易发生的时间

    // 无参构造器
    public WalletTransaction() {
    }

    // 全参构造器
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
