package site.aronnax.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import site.aronnax.dao.FeeDAO;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.service.FeeService;

/**
 * 费用服务实现类
 * 处理各类物业账单的生命周期，包含自动计费策略及关键的欠费拦截校验。
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class FeeServiceImpl implements FeeService {

    private final FeeDAO feeDAO;
    private final PropertyDAO propertyDAO;
    private final UserDAO userDAO;

    public FeeServiceImpl(FeeDAO feeDAO, PropertyDAO propertyDAO, UserDAO userDAO) {
        this.feeDAO = feeDAO;
        this.propertyDAO = propertyDAO;
        this.userDAO = userDAO;
    }

    /**
     * 创建单笔账单
     * 逻辑：根据费用类型（feeType）自动分配支付路径。
     * - 水电费 -> 对应的卡扣模式（WATER_CARD/ELEC_CARD）
     * - 物业/取暖 -> 钱包支付（WALLET）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFee(Long propertyId, String feeType, Double amount) {
        Fee fee = new Fee();
        fee.setpId(propertyId);
        fee.setFeeType(feeType);
        fee.setAmount(amount);
        fee.setIsPaid(0); // 默认未缴费
        fee.setPayDate(null);

        // 计费策略：根据类型自动设定支付方式
        if ("WATER_FEE".equals(feeType)) {
            fee.setPaymentMethod("WATER_CARD");
        } else if ("ELECTRICITY_FEE".equals(feeType)) {
            fee.setPaymentMethod("ELEC_CARD");
        } else {
            fee.setPaymentMethod("WALLET"); // 物业费与取暖费通常由钱包结算
        }

        return feeDAO.insert(fee);
    }

    /**
     * 批量创建账单
     * 使用循环迭代生成，并在事务保护下确保数据一致性。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateFees(List<Long> propertyIds, String feeType, Double amount) {
        int count = 0;
        for (Long propertyId : propertyIds) {
            createFee(propertyId, feeType, amount);
            count++;
        }
        return count;
    }

    /**
     * 标记缴费成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean payFee(Long feeId) {
        Fee fee = feeDAO.findById(feeId);
        if (fee == null) {
            return false;
        }

        fee.setIsPaid(1);
        fee.setPayDate(LocalDateTime.now());
        return feeDAO.update(fee);
    }

    @Override
    public List<Fee> getUnpaidFees() {
        return feeDAO.findUnpaidFees();
    }

    /**
     * 组装欠费汇总表
     * 核心逻辑：执行多库联查。
     * 从 FeeDAO 获取单据 -> 从 PropertyDAO 获取房屋位置 -> 从 UserDAO 获取业主身份。
     */
    @Override
    public List<Map<String, Object>> getArrearsList() {
        List<Map<String, Object>> arrearsList = new ArrayList<>();
        List<Fee> unpaidFees = feeDAO.findUnpaidFees();

        for (Fee fee : unpaidFees) {
            Map<String, Object> arrearsInfo = new HashMap<>();

            // 1. 获取房产位置快照
            Property property = propertyDAO.findById(fee.getpId());
            if (property != null) {
                arrearsInfo.put("property_id", property.getpId());
                arrearsInfo.put("building_no", property.getBuildingNo());
                arrearsInfo.put("unit_no", property.getUnitNo());
                arrearsInfo.put("room_no", property.getRoomNo());

                // 2. 溯产业主联系方式
                if (property.getUserId() != null) {
                    User owner = userDAO.findById(property.getUserId());
                    if (owner != null) {
                        arrearsInfo.put("owner_name", owner.getName());
                        arrearsInfo.put("owner_phone", owner.getPhone());
                    }
                }
            }

            // 3. 填充账单基础信息
            arrearsInfo.put("fee_id", fee.getfId());
            arrearsInfo.put("fee_type", fee.getFeeType());
            arrearsInfo.put("amount", fee.getAmount());
            arrearsInfo.put("payment_method", fee.getPaymentMethod());
            arrearsInfo.put("created_at", fee.getCreatedAt());

            arrearsList.add(arrearsInfo);
        }

        return arrearsList;
    }

    /**
     * 【关键业务点】检查房产是否存在任意欠费项目
     * 该逻辑用于物理拦截：一旦检测到欠费，业主的所有卡片购买动作都将被拒绝。
     */
    @Override
    public boolean checkArrears(Long propertyId) {
        List<Fee> unpaidFees = feeDAO.findUnpaidByPropertyId(propertyId);
        return !unpaidFees.isEmpty();
    }

    /**
     * 检查房产是否存在“钱包支付类”欠费
     * 主要针对物业费和取暖费。系统设计允许一定程度的卡内消费，但阻止在欠费下的充值行为。
     */
    @Override
    public boolean checkWalletArrears(Long propertyId) {
        List<Fee> unpaidFees = feeDAO.findUnpaidByPropertyId(propertyId);
        for (Fee fee : unpaidFees) {
            if ("WALLET".equals(fee.getPaymentMethod())) {
                return true;
            }
        }
        return false;
    }
}
