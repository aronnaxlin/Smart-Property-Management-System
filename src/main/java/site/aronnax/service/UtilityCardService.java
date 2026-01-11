package site.aronnax.service;

/**
 * 水电卡业务接口
 * 负责虚拟卡片的余额充值、消耗及状态查询。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface UtilityCardService {

    /**
     * 水电卡充值
     * 【重要业务拦截】：充值动作执行前，必须通过 FeeService 校验该房产是否存在欠费。
     * 若存在欠费，充值功能将处于锁定状态。
     *
     * @param cardId 卡片 ID
     * @param amount 充值金额
     * @return 是否充值成功
     */
    boolean topUp(Long cardId, Double amount);

    /**
     * 获取指定卡片的当前余额
     *
     * @param cardId 卡片 ID
     * @return 实时余额
     */
    Double getCardBalance(Long cardId);

    /**
     * 获取用户所有水电卡信息
     * 返回用户名下所有房产的水电卡，包含房产位置信息
     *
     * @param userId 用户 ID
     * @return 水电卡信息列表（包含卡号、类型、余额、房产位置）
     */
    java.util.List<java.util.Map<String, Object>> getUserCards(Long userId);

    /**
     * 根据卡片ID查询水电卡
     *
     * @param cardId 卡片 ID
     * @return 水电卡实体
     */
    site.aronnax.entity.UtilityCard findById(Long cardId);
}
