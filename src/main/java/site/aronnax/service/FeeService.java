package site.aronnax.service;

import java.util.List;
import java.util.Map;

import site.aronnax.entity.Fee;

/**
 * 费用与账单业务接口
 * 定义房产计费、缴费状态维护及欠费拦截的核心契约。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface FeeService {

    /**
     * 生成单笔费用账单
     *
     * @param propertyId 房产 ID
     * @param feeType    费用类型 (如 PROPERTY_FEE: 物业费, HEATING_FEE: 取暖费)
     * @param amount     金额
     * @return 生成的账单主键 ID
     */
    Long createFee(Long propertyId, String feeType, Double amount);

    /**
     * 批量生成账单
     * 系统根据选定的房产范围，统一派发特定类型的账单。
     *
     * @param propertyIds 房产 ID 列表
     * @param feeType     费用类型
     * @param amount      单价/金额
     * @return 成功生成的单据数量
     */
    int batchCreateFees(List<Long> propertyIds, String feeType, Double amount);

    /**
     * 缴纳费用（更新支付状态）
     * 通常由控制层调用，标记该笔流水已结清。
     *
     * @param feeId 账单 ID
     * @return 是否处理成功
     */
    boolean payFee(Long feeId);

    /**
     * 获取全系统所有处于“待缴费”状态的原始单据
     */
    List<Fee> getUnpaidFees();

    /**
     * 生成欠费汇总报表
     * 整合业主姓名、电话、房号及欠费周期等数据，用于管理后台展示。
     *
     * @return 包含增强信息的关联数据视图
     */
    List<Map<String, Object>> getArrearsList();

    /**
     * 【核心业务拦截器】检查指定房产是否存在欠费
     * 用于水电卡充值逻辑：若返回 true，充值动作将被硬拦截。
     *
     * @param propertyId 房产 ID
     * @return true 表示存在未缴费项目（充值失效）
     */
    boolean checkArrears(Long propertyId);

    /**
     * 检查是否存在“钱包类”欠费
     * 仅针对需要余额支付的费用（如物业费、取暖费）进行校验。
     */
    boolean checkWalletArrears(Long propertyId);
}
