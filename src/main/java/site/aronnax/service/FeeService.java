package site.aronnax.service;

import java.util.List;
import java.util.Map;

import site.aronnax.entity.Fee;

/**
 * Fee Service Interface
 * Provides business logic for fee and billing management
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface FeeService {

    /**
     * Create a single fee bill
     *
     * @param propertyId Property ID
     * @param feeType    Fee type (e.g., PROPERTY_FEE, HEATING_FEE)
     * @param amount     Amount
     * @return Generated fee ID
     */
    Long createFee(Long propertyId, String feeType, Double amount);

    /**
     * Batch create fees for multiple properties
     *
     * @param propertyIds List of property IDs
     * @param feeType     Fee type
     * @param amount      Amount per property
     * @return Number of fees created
     */
    int batchCreateFees(List<Long> propertyIds, String feeType, Double amount);

    /**
     * Pay a fee (update payment status)
     *
     * @param feeId Fee ID
     * @return true if successful
     */
    boolean payFee(Long feeId);

    /**
     * Get all unpaid fees
     *
     * @return List of unpaid fees
     */
    List<Fee> getUnpaidFees();

    /**
     * Generate arrears list with owner and property details
     *
     * @return List of maps containing arrears information
     */
    List<Map<String, Object>> getArrearsList();

    /**
     * CRITICAL: Check if a property has any unpaid fees
     * This is used for utility card top-up interception
     *
     * @param propertyId Property ID
     * @return true if there are unpaid fees (blocked)
     */
    boolean checkArrears(Long propertyId);

    /**
     * Check if a property has any unpaid WALLET-paid fees
     *
     * @param propertyId Property ID
     * @return true if there are unpaid wallet fees
     */
    boolean checkWalletArrears(Long propertyId);
}
