package site.aronnax.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import site.aronnax.dao.FeeDAO;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.service.FeeService;

/**
 * Fee Service Implementation
 * Implements fee and billing management business logic
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public class FeeServiceImpl implements FeeService {

    private final FeeDAO feeDAO;
    private final PropertyDAO propertyDAO;
    private final UserDAO userDAO;

    public FeeServiceImpl() {
        this.feeDAO = new FeeDAO();
        this.propertyDAO = new PropertyDAO();
        this.userDAO = new UserDAO();
    }

    @Override
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

    @Override
    public int batchCreateFees(List<Long> propertyIds, String feeType, Double amount) {
        int count = 0;
        for (Long propertyId : propertyIds) {
            Long feeId = createFee(propertyId, feeType, amount);
            if (feeId != null) {
                count++;
            }
        }
        return count;
    }

    @Override
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

    @Override
    public boolean checkArrears(Long propertyId) {
        List<Fee> unpaidFees = feeDAO.findUnpaidByPropertyId(propertyId);
        return !unpaidFees.isEmpty(); // Returns true if there are unpaid fees
    }

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
