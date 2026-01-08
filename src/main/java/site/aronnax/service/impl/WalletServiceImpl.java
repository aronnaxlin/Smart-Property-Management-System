package site.aronnax.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import site.aronnax.dao.FeeDAO;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserWalletDAO;
import site.aronnax.dao.UtilityCardDAO;
import site.aronnax.dao.WalletTransactionDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.Property;
import site.aronnax.entity.UserWallet;
import site.aronnax.entity.UtilityCard;
import site.aronnax.entity.WalletTransaction;
import site.aronnax.service.WalletService;

/**
 * Wallet Service Implementation
 * Implements wallet management business logic with critical arrears checking
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class WalletServiceImpl implements WalletService {

    private final UserWalletDAO walletDAO;
    private final WalletTransactionDAO transactionDAO;
    private final FeeDAO feeDAO;
    private final UtilityCardDAO cardDAO;
    private final PropertyDAO propertyDAO;

    public WalletServiceImpl() {
        this.walletDAO = new UserWalletDAO();
        this.transactionDAO = new WalletTransactionDAO();
        this.feeDAO = new FeeDAO();
        this.cardDAO = new UtilityCardDAO();
        this.propertyDAO = new PropertyDAO();
    }

    @Override
    public boolean rechargeWallet(Long userId, Double amount) {
        if (amount <= 0) {
            System.err.println("❌ 充值金额必须大于0");
            return false;
        }

        // Get or create wallet
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            // Create wallet if not exists
            if (!createWallet(userId)) {
                return false;
            }
            wallet = walletDAO.findByUserId(userId);
        }

        // Update balance
        Double currentBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        Double newBalance = currentBalance + amount;
        wallet.setBalance(newBalance);

        // Update total recharged
        Double totalRecharged = wallet.getTotalRecharged() != null ? wallet.getTotalRecharged() : 0.0;
        wallet.setTotalRecharged(totalRecharged + amount);

        boolean success = walletDAO.update(wallet);

        if (success) {
            // Record transaction
            recordTransaction(wallet.getWalletId(), "RECHARGE", amount, newBalance, null,
                    "钱包充值: +" + amount + "元");
            System.out.println("✅ 钱包充值成功: 用户ID " + userId + ", 充值金额 " + amount + "元, 当前余额 " + newBalance + "元");
        }

        return success;
    }

    @Override
    public boolean payFeeFromWallet(Long feeId) {
        // Get fee information
        Fee fee = feeDAO.findById(feeId);
        if (fee == null) {
            System.err.println("❌ 账单不存在: " + feeId);
            return false;
        }

        if (fee.getIsPaid() == 1) {
            System.err.println("❌ 账单已缴费");
            return false;
        }

        // Verify this fee should be paid by wallet
        if (!"WALLET".equals(fee.getPaymentMethod())) {
            System.err.println("❌ 此账单不使用钱包支付，支付方式: " + fee.getPaymentMethod());
            return false;
        }

        // Get property and user ID
        Property property = propertyDAO.findById(fee.getpId());
        if (property == null || property.getUserId() == null) {
            System.err.println("❌ 房产信息无效");
            return false;
        }

        Long userId = property.getUserId();
        UserWallet wallet = walletDAO.findByUserId(userId);

        if (wallet == null) {
            System.err.println("❌ 用户钱包不存在");
            return false;
        }

        // Check balance
        Double currentBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        if (currentBalance < fee.getAmount()) {
            System.err.println("❌ 钱包余额不足: 需要 " + fee.getAmount() + "元, 当前 " + currentBalance + "元");
            return false;
        }

        // Deduct from wallet
        Double newBalance = currentBalance - fee.getAmount();
        wallet.setBalance(newBalance);

        boolean walletUpdated = walletDAO.update(wallet);
        if (!walletUpdated) {
            return false;
        }

        // Mark fee as paid
        fee.setIsPaid(1);
        fee.setPayDate(LocalDateTime.now());
        boolean feeUpdated = feeDAO.update(fee);

        if (feeUpdated) {
            // Record transaction
            recordTransaction(wallet.getWalletId(), "PAY_FEE", -fee.getAmount(), newBalance, feeId,
                    "缴费: " + fee.getFeeType() + " -" + fee.getAmount() + "元");
            System.out.println("✅ 缴费成功: 账单ID " + feeId + ", 金额 " + fee.getAmount() + "元");
        }

        return feeUpdated;
    }

    @Override
    public boolean topUpCardFromWallet(Long userId, Long cardId, Double amount) {
        if (amount <= 0) {
            System.err.println("❌ 充值金额必须大于0");
            return false;
        }

        // CRITICAL: Check wallet arrears before allowing top-up
        boolean hasArrears = checkWalletArrears(userId);
        if (hasArrears) {
            System.err.println("❌ 欠费拦截: 用户ID " + userId + " 存在未缴的钱包支付费用，无法充值水电卡！");
            throw new IllegalStateException("您有未缴的物业费/取暖费，请先缴清欠款后再充值水电卡");
        }

        // Get wallet
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            System.err.println("❌ 用户钱包不存在");
            return false;
        }

        // Check wallet balance
        Double currentWalletBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        if (currentWalletBalance < amount) {
            System.err.println("❌ 钱包余额不足: 需要 " + amount + "元, 当前 " + currentWalletBalance + "元");
            return false;
        }

        // Get card
        UtilityCard card = cardDAO.findById(cardId);
        if (card == null) {
            System.err.println("❌ 水电卡不存在: " + cardId);
            return false;
        }

        // Verify card belongs to user's property
        Property property = propertyDAO.findById(card.getpId());
        if (property == null || !property.getUserId().equals(userId)) {
            System.err.println("❌ 此卡不属于该用户");
            return false;
        }

        // Deduct from wallet
        Double newWalletBalance = currentWalletBalance - amount;
        wallet.setBalance(newWalletBalance);
        boolean walletUpdated = walletDAO.update(wallet);

        if (!walletUpdated) {
            return false;
        }

        // Add to card
        Double currentCardBalance = card.getBalance() != null ? card.getBalance() : 0.0;
        card.setBalance(currentCardBalance + amount);
        card.setLastTopup(LocalDateTime.now());
        boolean cardUpdated = cardDAO.update(card);

        if (cardUpdated) {
            // Record transaction
            recordTransaction(wallet.getWalletId(), "TOPUP_CARD", -amount, newWalletBalance, cardId,
                    "充值" + card.getCardType() + "卡: -" + amount + "元");
            System.out.println("✅ 水电卡充值成功: 卡ID " + cardId + ", 充值金额 " + amount + "元");
        }

        return cardUpdated;
    }

    @Override
    public boolean checkWalletArrears(Long userId) {
        // Get all properties owned by this user
        List<Property> properties = propertyDAO.findByUserId(userId);

        for (Property property : properties) {
            // Check unpaid wallet-payment fees for each property
            List<Fee> fees = feeDAO.findByPropertyId(property.getpId());
            for (Fee fee : fees) {
                if (fee.getIsPaid() == 0 && "WALLET".equals(fee.getPaymentMethod())) {
                    return true; // Found unpaid wallet fee
                }
            }
        }

        return false; // No unpaid wallet fees
    }

    @Override
    public Double getWalletBalance(Long userId) {
        UserWallet wallet = walletDAO.findByUserId(userId);
        return wallet != null ? wallet.getBalance() : null;
    }

    @Override
    public List<WalletTransaction> getTransactionHistory(Long userId) {
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            return List.of();
        }
        return transactionDAO.findByWalletId(wallet.getWalletId());
    }

    @Override
    public boolean createWallet(Long userId) {
        UserWallet wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setTotalRecharged(0.0);

        Long walletId = walletDAO.insert(wallet);
        if (walletId != null) {
            System.out.println("✅ 钱包创建成功: 用户ID " + userId + ", 钱包ID " + walletId);
            return true;
        }
        return false;
    }

    /**
     * Helper method to record transaction
     */
    private void recordTransaction(Long walletId, String transType, Double amount,
            Double balanceAfter, Long relatedId, String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWalletId(walletId);
        transaction.setTransType(transType);
        transaction.setAmount(Math.abs(amount)); // Store absolute value
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedId(relatedId);
        transaction.setDescription(description);

        transactionDAO.insert(transaction);
    }
}
