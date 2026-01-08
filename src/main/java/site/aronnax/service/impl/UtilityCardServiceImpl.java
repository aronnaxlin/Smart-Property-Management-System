package site.aronnax.service.impl;

import java.time.LocalDateTime;

import site.aronnax.dao.UtilityCardDAO;
import site.aronnax.entity.UtilityCard;
import site.aronnax.service.FeeService;
import site.aronnax.service.UtilityCardService;

/**
 * Utility Card Service Implementation
 * Implements utility card management with arrears checking
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class UtilityCardServiceImpl implements UtilityCardService {

    private final UtilityCardDAO utilityCardDAO;
    private final FeeService feeService;

    public UtilityCardServiceImpl() {
        this.utilityCardDAO = new UtilityCardDAO();
        this.feeService = new FeeServiceImpl(); // Dependency on FeeService
    }

    @Override
    public boolean topUp(Long cardId, Double amount) {
        // Get card information
        UtilityCard card = utilityCardDAO.findById(cardId);
        if (card == null) {
            System.err.println("❌ 水电卡不存在: " + cardId);
            return false;
        }

        // CRITICAL: Check for wallet arrears before allowing top-up
        boolean hasArrears = feeService.checkWalletArrears(card.getpId());
        if (hasArrears) {
            System.err.println("❌ 欠费拦截: 房产ID " + card.getpId() + " 存在未缴的物业费/取暖费，无法充值！");
            throw new IllegalStateException("您有未缴的物业费/取暖费，请先缴清欠款后再充值");
        }

        // Proceed with top-up
        Double currentBalance = card.getBalance() != null ? card.getBalance() : 0.0;
        card.setBalance(currentBalance + amount);
        card.setLastTopup(LocalDateTime.now());

        boolean success = utilityCardDAO.update(card);
        if (success) {
            System.out.println("✅ 充值成功: 水电卡ID " + cardId + ", 充值金额 " + amount + ", 当前余额 " + card.getBalance());
        }

        return success;
    }

    @Override
    public Double getCardBalance(Long cardId) {
        UtilityCard card = utilityCardDAO.findById(cardId);
        return card != null ? card.getBalance() : null;
    }
}
