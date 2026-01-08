package site.aronnax.entity;

import java.time.LocalDateTime;

/**
 * 费用账单实体类
 * 对应数据库 fees 表
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class Fee {

    private Long fId; // 主键ID
    private Long pId; // 关联房产ID
    private String feeType; // 费用类型
    private Double amount; // 账单金额
    private Integer isPaid; // 缴费状态: 0-未缴, 1-已缴
    private String paymentMethod; // 支付方式: WALLET, WATER_CARD, ELEC_CARD
    private LocalDateTime payDate; // 缴费时间
    private LocalDateTime createdAt; // 账单生成时间
    private LocalDateTime updatedAt; // 状态更新时间

    // 无参构造器
    public Fee() {
    }

    // 全参构造器
    public Fee(Long fId, Long pId, String feeType, Double amount,
            Integer isPaid, String paymentMethod, LocalDateTime payDate,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.fId = fId;
        this.pId = pId;
        this.feeType = feeType;
        this.amount = amount;
        this.isPaid = isPaid;
        this.paymentMethod = paymentMethod;
        this.payDate = payDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public Long getfId() {
        return fId;
    }

    public void setfId(Long fId) {
        this.fId = fId;
    }

    public Long getpId() {
        return pId;
    }

    public void setpId(Long pId) {
        this.pId = pId;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Integer getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Integer isPaid) {
        this.isPaid = isPaid;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public LocalDateTime getPayDate() {
        return payDate;
    }

    public void setPayDate(LocalDateTime payDate) {
        this.payDate = payDate;
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
        return "Fee{" +
                "fId=" + fId +
                ", pId=" + pId +
                ", feeType='" + feeType + '\'' +
                ", amount=" + amount +
                ", isPaid=" + isPaid +
                ", payDate=" + payDate +
                '}';
    }
}
