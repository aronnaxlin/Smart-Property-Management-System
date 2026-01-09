package site.aronnax.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 钱包服务实现类
 * 实现钱包管理业务逻辑，包含关键的欠费检查机制
 *
 * 核心功能：
 * 1. 钱包充值
 * 2. 使用钱包余额缴费
 * 3. 钱包转账到水电卡（含欠费拦截）
 * 4. 欠费状态检查
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class WalletServiceImpl implements WalletService {

    private final UserWalletDAO walletDAO;
    private final WalletTransactionDAO transactionDAO;
    private final FeeDAO feeDAO;
    private final UtilityCardDAO cardDAO;
    private final PropertyDAO propertyDAO;

    public WalletServiceImpl(UserWalletDAO walletDAO, WalletTransactionDAO transactionDAO, FeeDAO feeDAO,
            UtilityCardDAO cardDAO, PropertyDAO propertyDAO) {
        this.walletDAO = walletDAO;
        this.transactionDAO = transactionDAO;
        this.feeDAO = feeDAO;
        this.cardDAO = cardDAO;
        this.propertyDAO = propertyDAO;
    }

    /**
     * 钱包充值
     *
     * @param userId 用户ID
     * @param amount 充值金额
     * @return 充值是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rechargeWallet(Long userId, Double amount) {
        // 参数验证：金额必须为正数
        if (amount == null || amount <= 0) {
            return false;
        }

        // 获取或创建钱包
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            // 首次使用，自动创建钱包
            if (!createWallet(userId)) {
                return false;
            }
            wallet = walletDAO.findByUserId(userId);
            if (wallet == null) {
                // 创建失败
                return false;
            }
        }

        // 更新余额
        Double currentBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        Double newBalance = currentBalance + amount;
        wallet.setBalance(newBalance);

        // 更新累计充值金额
        Double totalRecharged = wallet.getTotalRecharged() != null ? wallet.getTotalRecharged() : 0.0;
        wallet.setTotalRecharged(totalRecharged + amount);

        // 执行数据库更新
        boolean success = walletDAO.update(wallet);

        if (success) {
            // 记录充值交易
            recordTransaction(wallet.getWalletId(), "RECHARGE", amount, newBalance, null,
                    "钱包充值: +" + amount + "元");
        }

        return success;
    }

    /**
     * 使用钱包余额缴纳费用
     *
     * 事务操作：同时更新钱包余额和费用状态
     *
     * @param feeId 费用ID
     * @return 缴费是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payFeeFromWallet(Long feeId) {
        Fee fee = feeDAO.findById(feeId);
        if (fee == null || fee.getIsPaid() == 1 || !"WALLET".equals(fee.getPaymentMethod())) {
            return false;
        }

        Property property = propertyDAO.findById(fee.getpId());
        if (property == null || property.getUserId() == null) {
            return false;
        }

        Long userId = property.getUserId();
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null)
            return false;

        Double currentBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        if (currentBalance < fee.getAmount())
            return false;

        Double newBalance = currentBalance - fee.getAmount();
        wallet.setBalance(newBalance);

        boolean walletUpdated = walletDAO.update(wallet);
        if (!walletUpdated)
            return false;

        fee.setIsPaid(1);
        fee.setPayDate(LocalDateTime.now());
        boolean feeUpdated = feeDAO.update(fee);

        if (feeUpdated) {
            recordTransaction(wallet.getWalletId(), "PAY_FEE", -fee.getAmount(), newBalance, feeId,
                    "缴费: " + fee.getFeeType() + " -" + fee.getAmount() + "元");
        }

        return feeUpdated;
    }

    /**
     * 使用钱包余额为水电卡充值
     *
     * 【核心业务逻辑】：充值前必须检查欠费状态
     * 事务操作：同时更新钱包和水电卡余额
     *
     * @param userId 用户ID
     * @param cardId 水电卡ID
     * @param amount 充值金额
     * @return 充值是否成功
     * @throws IllegalStateException 当存在未缴费用时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean topUpCardFromWallet(Long userId, Long cardId, Double amount) {
        // 参数验证
        if (amount == null || amount <= 0) {
            return false;
        }

        // 【关键拦截】检查是否存在未缴的钱包类费用
        if (checkWalletArrears(userId)) {
            throw new IllegalStateException("您有未缴的物业费/取暖费，请先缴清欠款后再充值");
        }

        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null)
            return false;

        Double currentWalletBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        if (currentWalletBalance < amount)
            return false;

        UtilityCard card = cardDAO.findById(cardId);
        if (card == null)
            return false;

        Property property = propertyDAO.findById(card.getpId());
        if (property == null || !property.getUserId().equals(userId))
            return false;

        Double newWalletBalance = currentWalletBalance - amount;
        wallet.setBalance(newWalletBalance);
        if (!walletDAO.update(wallet))
            return false;

        Double currentCardBalance = card.getBalance() != null ? card.getBalance() : 0.0;
        card.setBalance(currentCardBalance + amount);
        card.setLastTopup(LocalDateTime.now());
        boolean cardUpdated = cardDAO.update(card);

        if (cardUpdated) {
            recordTransaction(wallet.getWalletId(), "TOPUP_CARD", -amount, newWalletBalance, cardId,
                    "充值" + card.getCardType() + "卡: -" + amount + "元");
        }

        return cardUpdated;
    }

    /**
     * 检查用户是否存在钱包类费用欠费
     *
     * 钱包类费用包括：物业费(PROPERTY_FEE)、取暖费(HEATING_FEE)
     * 不包括：水费、电费（这些由水电卡支付）
     *
     * @param userId 用户ID
     * @return true表示存在欠费，false表示无欠费
     */
    @Override
    public boolean checkWalletArrears(Long userId) {
        List<Property> properties = propertyDAO.findByUserId(userId);
        for (Property property : properties) {
            List<Fee> fees = feeDAO.findByPropertyId(property.getpId());
            for (Fee fee : fees) {
                if (fee.getIsPaid() == 0 && "WALLET".equals(fee.getPaymentMethod())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Double getWalletBalance(Long userId) {
        UserWallet wallet = walletDAO.findByUserId(userId);
        return wallet != null ? wallet.getBalance() : null;
    }

    @Override
    public List<WalletTransaction> getTransactionHistory(Long userId) {
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null)
            return List.of();
        return transactionDAO.findByWalletId(wallet.getWalletId());
    }

    @Override
    public boolean createWallet(Long userId) {
        UserWallet wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setTotalRecharged(0.0);
        return walletDAO.insert(wallet) != null;
    }

    private void recordTransaction(Long walletId, String transType, Double amount, Double balanceAfter, Long relatedId,
            String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWalletId(walletId);
        transaction.setTransType(transType);
        transaction.setAmount(Math.abs(amount));
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedId(relatedId);
        transaction.setDescription(description);
        transactionDAO.insert(transaction);
    }
}
