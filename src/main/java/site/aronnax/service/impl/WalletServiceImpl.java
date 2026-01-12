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
    private final site.aronnax.service.FeeService feeService;

    public WalletServiceImpl(UserWalletDAO walletDAO, WalletTransactionDAO transactionDAO, FeeDAO feeDAO,
            UtilityCardDAO cardDAO, PropertyDAO propertyDAO, site.aronnax.service.FeeService feeService) {
        this.walletDAO = walletDAO;
        this.transactionDAO = transactionDAO;
        this.feeDAO = feeDAO;
        this.cardDAO = cardDAO;
        this.propertyDAO = propertyDAO;
        this.feeService = feeService;
    }

    /**
     * 钱包充值逻辑
     * 实现用户资金注入。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rechargeWallet(Long userId, Double amount) {
        // 数据完整性校验：拦截非法金额
        if (amount == null || amount <= 0) {
            return false;
        }

        // 获取或初始化钱包（延迟创建模式）
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null) {
            if (!createWallet(userId)) {
                return false;
            }
            wallet = walletDAO.findByUserId(userId);
            if (wallet == null) {
                return false;
            }
        }

        // 资金入账
        Double currentBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        Double newBalance = currentBalance + amount;
        wallet.setBalance(newBalance);

        // 维护审计字段：累计充值额度
        Double totalRecharged = wallet.getTotalRecharged() != null ? wallet.getTotalRecharged() : 0.0;
        wallet.setTotalRecharged(totalRecharged + amount);

        boolean success = walletDAO.update(wallet);

        if (success) {
            // 生成充值凭证流水
            recordTransaction(wallet.getWalletId(), "RECHARGE", amount, newBalance, null,
                    "在线账户资金注入: +" + amount + "元");
        }

        return success;
    }

    /**
     * 内部余额支付（缴费）
     *
     * 逻辑闭环：
     * 1. 验证账单是否处于待缴状态 -> 2. 验证角色权限 -> 3. 验证余额充足性 -> 4. 执行双向更新（余额减、账单结） -> 5. 存证。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payFeeFromWallet(Long feeId, jakarta.servlet.http.HttpSession session) {
        // 第一步：单据状态前校验
        Fee fee = feeDAO.findById(feeId);
        if (fee == null || fee.getIsPaid() == 1) {
            return false;
        }

        // 第二步：【新增】角色权限验证
        String userType = (String) session.getAttribute("userType");
        if ("ADMIN".equals(userType)) {
            // 管理员不能代缴水电费
            if ("WATER_FEE".equals(fee.getFeeType()) || "ELECTRICITY_FEE".equals(fee.getFeeType())) {
                throw new IllegalArgumentException("管理员不能代缴水费和电费，请指导业主使用水电卡缴费");
            }
        }

        // 第三步：验证 payment_method（必须为 WALLET）
        if (!"WALLET".equals(fee.getPaymentMethod())) {
            return false;
        }

        // 第四步：余额充足性判定
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

        // 第五阶段：资金扣减与单据标记（受强事务保护）
        Double newBalance = currentBalance - fee.getAmount();
        wallet.setBalance(newBalance);

        boolean walletUpdated = walletDAO.update(wallet);
        if (!walletUpdated)
            return false;

        fee.setIsPaid(1);
        fee.setPayDate(LocalDateTime.now());
        boolean feeUpdated = feeDAO.update(fee);

        if (feeUpdated) {
            // 流水归档
            recordTransaction(wallet.getWalletId(), "PAY_FEE", -fee.getAmount(), newBalance, feeId,
                    "生活缴费支付: " + fee.getFeeType() + " -" + fee.getAmount() + "元");
        }

        return feeUpdated;
    }

    /**
     * 钱包转账向水电卡（核心风控业务）
     *
     * 【重要规则】：系统在此处执行“欠费静默锁定”。
     * 如果用户存在物业/取暖欠费，其内部资金流转路径将被切断。
     *
     * @throws IllegalStateException 触发拦截规则时抛出异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean topUpCardFromWallet(Long userId, Long cardId, Double amount) {
        if (amount == null || amount <= 0) {
            return false;
        }

        // 【安全拦截】检查该用户在全区名下的财务健康度
        // 遍历用户所有房产，检查是否存在欠费
        List<Property> properties = propertyDAO.findByUserId(userId);
        boolean hasArrears = false;
        for (Property property : properties) {
            if (feeService.checkWalletArrears(property.getpId())) {
                hasArrears = true;
                break;
            }
        }
        if (hasArrears) {
            throw new IllegalStateException("【缴费受阻】检测到您当前存在逾期未缴的物业费/取暖费单项，系统已锁定水电卡充值通道。");
        }

        // 账户状态核验
        UserWallet wallet = walletDAO.findByUserId(userId);
        if (wallet == null)
            return false;

        Double currentWalletBalance = wallet.getBalance() != null ? wallet.getBalance() : 0.0;
        if (currentWalletBalance < amount)
            return false;

        UtilityCard card = cardDAO.findById(cardId);
        if (card == null)
            return false;

        // 资产归属校验：防止由于接口构造错误导致向他人房产转账
        Property property = propertyDAO.findById(card.getpId());
        if (property == null || !property.getUserId().equals(userId))
            return false;

        // 跨模块资金搬运
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
                    "内部转账-卡片充值: -" + amount + "元");
        }

        return cardUpdated;
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

    /**
     * 初始化财务账户
     */
    @Override
    public boolean createWallet(Long userId) {
        UserWallet wallet = new UserWallet();
        wallet.setUserId(userId);
        wallet.setBalance(0.0);
        wallet.setTotalRecharged(0.0);
        return walletDAO.insert(wallet) != null;
    }

    /**
     * 内部存证方法
     * 确保每一分钱的去向都有迹可循，用于年底财务对账。
     */
    private void recordTransaction(Long walletId, String transType, Double amount, Double balanceAfter, Long relatedId,
            String description) {
        WalletTransaction transaction = new WalletTransaction();
        transaction.setWalletId(walletId);
        transaction.setTransType(transType);
        transaction.setAmount(Math.abs(amount)); // 金额以绝对值存入，类型由 transType 区分
        transaction.setBalanceAfter(balanceAfter);
        transaction.setRelatedId(relatedId);
        transaction.setDescription(description);
        transactionDAO.insert(transaction);
    }
}
