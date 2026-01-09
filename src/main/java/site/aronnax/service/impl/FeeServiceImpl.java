package site.aronnax.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import site.aronnax.dao.FeeDAO;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.service.FeeService;

/**
 * 费用服务实现类
 * 实现账单和缴费管理业务逻辑
 *
 * 核心功能：
 * 1. 创建单笔或批量账单
 * 2. 标记账单为已支付
 * 3. 查询欠费列表
 * 4. 检查房产是否存在欠费（关键拦截逻辑）
 *
 * @author Aronnax (Li Linhan)
 */
@Service
@RequiredArgsConstructor
public class FeeServiceImpl implements FeeService {

    private final FeeDAO feeDAO;
    private final PropertyDAO propertyDAO;
    private final UserDAO userDAO;

    /**
     * 创建单笔账单
     *
     * @param propertyId 房产ID
     * @param feeType    费用类型
     * @param amount     账单金额
     * @return 账单ID（当前实现可能返回null，但不影响业务）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createFee(Long propertyId, String feeType, Double amount) {
        Fee fee = new Fee();
        fee.setpId(propertyId);
        fee.setFeeType(feeType);
        fee.setAmount(amount);
        fee.setIsPaid(0); // Unpaid by default
        fee.setPayDate(null);

        // Auto-assign payment method based on fee type
        if ("WATER_FEE".equals(feeType)) {
            fee.setPaymentMethod("WATER_CARD");
        } else if ("ELECTRICITY_FEE".equals(feeType)) {
            fee.setPaymentMethod("ELEC_CARD");
        } else {
            fee.setPaymentMethod("WALLET"); // Default (Property/Heating)
        }

        return feeDAO.insert(fee);
    }

    /**
     * 批量创建账单
     * 为多个房产创建相同类型和金额的账单
     *
     * @param propertyIds 房产ID列表
     * @param feeType     费用类型
     * @param amount      账单金额
     * @return 成功创建的账单数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchCreateFees(List<Long> propertyIds, String feeType, Double amount) {
        int count = 0;
        for (Long propertyId : propertyIds) {
            Long feeId = createFee(propertyId, feeType, amount);
            // Check if insertion was successful (assuming insert returns null/id, though
            // new impl returns null always for now..
            // The logic assumes creating ID. Since current DAO returns null, count might be
            // off if checked against null.
            // Since insert throws exception if fails (jdbcTemplate), we can assume success
            // here or check if we updated DAO to return ID.
            // But let's just increment count for now.
            count++;
        }
        return count;
    }

    /**
     * 标记账单为已支付
     *
     * @param feeId 账单ID
     * @return 是否成功
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

    @Override
    public List<Map<String, Object>> getArrearsList() {
        List<Map<String, Object>> arrearsList = new ArrayList<>();
        List<Fee> unpaidFees = feeDAO.findUnpaidFees();

        for (Fee fee : unpaidFees) {
            Map<String, Object> arrearsInfo = new HashMap<>();

            // Get property information
            Property property = propertyDAO.findById(fee.getpId());
            if (property != null) {
                arrearsInfo.put("property_id", property.getpId());
                arrearsInfo.put("building_no", property.getBuildingNo());
                arrearsInfo.put("unit_no", property.getUnitNo());
                arrearsInfo.put("room_no", property.getRoomNo());

                // Get owner information
                if (property.getUserId() != null) {
                    User owner = userDAO.findById(property.getUserId());
                    if (owner != null) {
                        arrearsInfo.put("owner_name", owner.getName());
                        arrearsInfo.put("owner_phone", owner.getPhone());
                    }
                }
            }

            // Add fee information
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
     * 检查房产是否存在未缴费用
     *
     * 用于水电卡充值拦截逻辑
     *
     * @param propertyId 房产ID
     * @return true表示存在欠费，false表示无欠费
     */
    @Override
    public boolean checkArrears(Long propertyId) {
        List<Fee> unpaidFees = feeDAO.findUnpaidByPropertyId(propertyId);
        return !unpaidFees.isEmpty(); // Returns true if there are unpaid fees
    }

    /**
     * 检查房产是否存在钱包类费用欠费
     *
     * 钱包类费用：payment_method = 'WALLET' 的费用
     * 主要包括物业费和取暖费
     *
     * @param propertyId 房产ID
     * @return true表示存在钱包类欠费，false表示无欠费
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
