package site.aronnax.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import site.aronnax.common.Result;
import site.aronnax.entity.WalletTransaction;
import site.aronnax.service.WalletService;

/**
 * 电子钱包控制器
 * 提供账户余额查券、线上充值、在线缴费及交易流水调阅功能。
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
     * 获取钱包账户概览
     *
     * @param userId 业主唯一标识
     */
    @GetMapping("/info")
    public Result<Double> getWalletBalance(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("用户凭据校验失败");
        }

        Double balance = walletService.getWalletBalance(userId);

        // 容错处理：若为新用户且尚未触发自动开户，默认返回零余额
        if (balance == null) {
            return Result.success(0.0);
        }

        return Result.success(balance);
    }

    /**
     * 钱包资金注入
     * 模拟第三方支付（如微信/支付宝）回调后的入账过程。
     *
     * @param userId 用户 ID
     * @param amount 充值标额
     */
    @PostMapping("/recharge")
    public Result<String> recharge(@RequestParam("userId") Long userId, @RequestParam("amount") Double amount) {
        if (userId == null || userId <= 0) {
            return Result.error("账户验证失败");
        }

        if (amount == null || amount <= 0) {
            return Result.error("充值额度需大于零");
        }

        // 风控阈值：防止极端金额导致的系统溢出或合规风控
        if (amount > 1000000) {
            return Result.error("已触发单笔超限额保护（100万元）");
        }

        boolean success = walletService.rechargeWallet(userId, amount);
        if (success) {
            return Result.success("资金已安全到达电子钱包账户");
        }

        return Result.error("充值处理失败，由于系统链路故障，请点击重新提交");
    }

    /**
     * 历史交易流水调阅
     * 每一笔充值、扣款、转账记录均可追溯。
     */
    @GetMapping("/transactions")
    public Result<List<WalletTransaction>> getTransactions(@RequestParam("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("未授权的查询请求");
        }

        List<WalletTransaction> transactions = walletService.getTransactionHistory(userId);
        return Result.success(transactions != null ? transactions : List.of());
    }

    /**
     * 余额代扣（生活缴费）
     * 允许业主通过钱包余额结清物业费或取暖费。
     * 【权限控制】管理员不能代缴水电费
     *
     * @param feeId 账单单号
     */
    @PostMapping("/pay-fee")
    public Result<String> payFee(@RequestParam("feeId") Long feeId, HttpSession session) {
        if (feeId == null || feeId <= 0) {
            return Result.error("账单流水号无效");
        }

        try {
            // 执行双向原子扣款（含角色验证）
            boolean success = walletService.payFeeFromWallet(feeId, session);
            if (success) {
                return Result.success("账单缴清，系统已实时更新房产财务状态");
            }
            return Result.error("缴费失败：可能由于账户余额不足或该账单已被锁定");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }
}
