package site.aronnax.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.entity.WalletTransaction;
import site.aronnax.service.WalletService;

/**
 * 钱包管理控制器
 * 提供钱包余额查询、充值、缴费等功能
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    /**
     * 查询钱包余额
     *
     * @param userId 用户ID
     * @return 钱包余额（如果钱包不存在则返回0.0）
     */
    @GetMapping("/info")
    public Result<Double> getWalletBalance(@RequestParam("userId") Long userId) {
        // 参数验证：用户ID不能为空
        if (userId == null || userId <= 0) {
            return Result.error("用户ID无效");
        }

        // 查询钱包余额
        Double balance = walletService.getWalletBalance(userId);

        // 如果钱包不存在，返回0.0作为默认值
        // 用户首次使用时会自动创建钱包
        if (balance == null) {
            return Result.success(0.0);
        }

        return Result.success(balance);
    }

    /**
     * 钱包充值
     *
     * @param userId 用户ID
     * @param amount 充值金额（必须大于0）
     * @return 充值结果
     */
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestParam("userId") Long userId, @RequestParam("amount") Double amount) {
        // 参数验证：用户ID不能为空
        if (userId == null || userId <= 0) {
            return Result.error("用户ID无效");
        }

        // 参数验证：金额必须为正数
        if (amount == null || amount <= 0) {
            return Result.error("充值金额必须大于0");
        }

        // 参数验证：金额上限检查（防止异常大额充值）
        if (amount > 1000000) {
            return Result.error("单次充值金额不能超过100万元");
        }

        // 执行充值操作
        boolean success = walletService.rechargeWallet(userId, amount);

        if (success) {
            return Result.success("钱包充值成功");
        }

        return Result.error("充值失败，请稍后重试");
    }

    /**
     * 查询钱包交易记录
     *
     * @param userId 用户ID
     * @return 交易记录列表
     */
    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> getTransactions(@RequestParam("userId") Long userId) {
        // 参数验证：用户ID不能为空
        if (userId == null || userId <= 0) {
            return Result.error("用户ID无效");
        }

        // 查询交易历史
        List<WalletTransaction> transactions = walletService.getTransactionHistory(userId);

        // 返回交易记录（如果为空则返回空列表）
        return Result.success(transactions != null ? transactions : List.of());
    }

    /**
     * 使用钱包余额缴纳费用
     *
     * @param feeId 账单ID
     * @return 缴费结果
     */
    @PostMapping("/pay-fee")
    public Result<String> payFee(@RequestParam("feeId") Long feeId) {
        // 参数验证：账单ID不能为空
        if (feeId == null || feeId <= 0) {
            return Result.error("账单ID无效");
        }

        // 执行缴费操作
        boolean success = walletService.payFeeFromWallet(feeId);

        if (success) {
            return Result.success("缴费成功");
        }

        return Result.error("缴费失败：余额不足或账单无效");
    }
}
