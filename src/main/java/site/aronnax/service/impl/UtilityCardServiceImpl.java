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
    private final site.aronnax.dao.PropertyDAO propertyDAO;

    public UtilityCardServiceImpl(UtilityCardDAO utilityCardDAO, FeeService feeService,
            site.aronnax.dao.PropertyDAO propertyDAO) {
        this.utilityCardDAO = utilityCardDAO;
        this.feeService = feeService;
        this.propertyDAO = propertyDAO;
    }

    /**
     * 水电卡充值功能实现
     *
     * 【核心风控环节】：
     * 1. 身份/卡号核验。
     * 2. 调用 FeeService.checkWalletArrears 进行欠费渗透检查。
     * 3. 若存在物业费、取暖费欠缴，即便支付渠道通畅，系统也将抛出 IllegalStateException 阻止入账。
     *
     * @param cardId 卡片 ID
     * @param amount 充值金额
     * @return 是否充值成功
     * @throws IllegalStateException 当检测到该房产名下有待缴物业费用时，触发行政锁定
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean topUp(Long cardId, Double amount) {
        // 第一阶段：卡片合法性核验
        UtilityCard card = utilityCardDAO.findById(cardId);
        if (card == null) {
            throw new RuntimeException("系统未找到指定的水电卡档案");
        }

        // 第二阶段：【关键拦截】执行“欠费锁定”策略
        // 逻辑：如果存在逾期未缴的物业费或取暖费，则硬拦截充值
        boolean hasArrears = feeService.checkWalletArrears(card.getpId());
        if (hasArrears) {
            throw new IllegalStateException("【操作拦截】检索到您名下仍有未结清的物业费/取暖费单项。根据规定，请先缴清欠款后再操作水电充值。");
        }

        // 第三阶段：资金结算
        Double currentBalance = card.getBalance() != null ? card.getBalance() : 0.0;
        card.setBalance(currentBalance + amount);
        card.setLastTopup(LocalDateTime.now()); // 更新最后充值时间，用于审计

        return utilityCardDAO.update(card);
    }

    /**
     * 获取指定水电卡的当前余额
     */
    @Override
    public Double getCardBalance(Long cardId) {
        UtilityCard card = utilityCardDAO.findById(cardId);
        return card != null ? card.getBalance() : null;
    }

    /**
     * 获取用户所有水电卡信息
     * 包含房产位置、卡类型、余额等信息
     */
    @Override
    public java.util.List<java.util.Map<String, Object>> getUserCards(Long userId) {
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        // 1. 查询用户所有房产
        java.util.List<site.aronnax.entity.Property> properties = propertyDAO.findByUserId(userId);

        // 2. 对每套房产，查询其水电卡
        for (site.aronnax.entity.Property property : properties) {
            java.util.List<UtilityCard> cards = utilityCardDAO.findByPropertyId(property.getpId());

            // 3. 构建返回信息
            for (UtilityCard card : cards) {
                java.util.Map<String, Object> cardInfo = new java.util.HashMap<>();
                cardInfo.put("cardId", card.getCardId());
                cardInfo.put("cardType", card.getCardType());
                cardInfo.put("balance", card.getBalance());
                cardInfo.put("propertyId", property.getpId());
                cardInfo.put("buildingNo", property.getBuildingNo());
                cardInfo.put("unitNo", property.getUnitNo());
                cardInfo.put("roomNo", property.getRoomNo());
                result.add(cardInfo);
            }
        }

        return result;
    }

    /**
     * 根据卡片ID查询水电卡
     */
    @Override
    public UtilityCard findById(Long cardId) {
        return utilityCardDAO.findById(cardId);
    }
}
