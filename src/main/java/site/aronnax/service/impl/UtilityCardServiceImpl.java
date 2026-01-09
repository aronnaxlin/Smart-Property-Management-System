package site.aronnax.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.aronnax.dao.UtilityCardDAO;
import site.aronnax.entity.UtilityCard;
import site.aronnax.service.FeeService;
import site.aronnax.service.UtilityCardService;

/**
 * 水电卡服务实现类
 * 实现水电卡管理业务逻辑，包含欠费拦截机制
 *
 * 【核心业务逻辑】：
 * 水电卡充值前必须检查是否存在未缴的物业费或取暖费
 * 这是系统的关键业务规则
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class UtilityCardServiceImpl implements UtilityCardService {

    private final UtilityCardDAO utilityCardDAO;
    private final FeeService feeService;

    public UtilityCardServiceImpl(UtilityCardDAO utilityCardDAO, FeeService feeService) {
        this.utilityCardDAO = utilityCardDAO;
        this.feeService = feeService;
    }

    /**
     * 水电卡充值
     *
     * 【核心业务逻辑】：充值前必须检查欠费状态
     *
     * @param cardId 水电卡ID
     * @param amount 充值金额
     * @return 充值是否成功
     * @throws IllegalStateException 当存在欠费时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean topUp(Long cardId, Double amount) {
        // Get card information
        UtilityCard card = utilityCardDAO.findById(cardId);
        if (card == null) {
            throw new RuntimeException("水电卡不存在");
        }

        // 【关键拦截】检查该房产是否存在钱包类欠费
        // 如果存在未缴的物业费或取暖费，禁止充值
        boolean hasArrears = feeService.checkWalletArrears(card.getpId());
        if (hasArrears) {
            throw new IllegalStateException("您有未缴的物业费/取暖费，请先缴清欠款后再充值");
        }

        // Proceed with top-up
        Double currentBalance = card.getBalance() != null ? card.getBalance() : 0.0;
        card.setBalance(currentBalance + amount);
        card.setLastTopup(LocalDateTime.now());

        return utilityCardDAO.update(card);
    }

    /**
     * 查询水电卡余额
     *
     * @param cardId 水电卡ID
     * @return 卡片余额，不存在时返回null
     */
    @Override
    public Double getCardBalance(Long cardId) {
        UtilityCard card = utilityCardDAO.findById(cardId);
        return card != null ? card.getBalance() : null;
    }
}
