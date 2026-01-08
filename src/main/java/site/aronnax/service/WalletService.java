package site.aronnax.service;

import java.util.List;

import site.aronnax.entity.WalletTransaction;

/**
 * Wallet Service Interface
 * Provides business logic for user wallet management
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface WalletService {

    /**
     * Recharge wallet
     *
     * @param userId User ID
     * @param amount Amount to recharge
     * @return true if successful
     */
    boolean rechargeWallet(Long userId, Double amount);

    /**
     * Pay fee from wallet
     * Used for PROPERTY_FEE and HEATING_FEE
     *
     * @param feeId Fee ID
     * @return true if successful
     */
    boolean payFeeFromWallet(Long feeId);

    /**
     * Top up utility card from wallet
     * CRITICAL: Must check wallet arrears before allowing top-up
     *
     * @param userId User ID (wallet owner)
     * @param cardId Card ID
     * @param amount Amount to top up
     * @return true if successful
     * @throws IllegalStateException if user has unpaid wallet fees
     */
    boolean topUpCardFromWallet(Long userId, Long cardId, Double amount);

    /**
     * CRITICAL: Check if user has unpaid wallet-payment fees
     * This is the NEW arrears interception logic
     *
     * @param userId User ID
     * @return true if there are unpaid wallet fees (blocked)
     */
    boolean checkWalletArrears(Long userId);

    /**
     * Get wallet balance
     *
     * @param userId User ID
     * @return Current balance
     */
    Double getWalletBalance(Long userId);

    /**
     * Get transaction history
     *
     * @param userId User ID
     * @return List of transactions
     */
    List<WalletTransaction> getTransactionHistory(Long userId);

    /**
     * Create wallet for new user
     *
     * @param userId User ID
     * @return true if successful
     */
    boolean createWallet(Long userId);
}
