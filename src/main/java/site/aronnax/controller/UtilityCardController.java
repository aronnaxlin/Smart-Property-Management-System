package site.aronnax.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.service.UtilityCardService;
import site.aronnax.service.WalletService;

/**
 * 水电卡管理控制器
 * 提供水电卡余额查询、充值等功能
 * 核心业务：执行欠费拦截逻辑
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/utility")
public class UtilityCardController {

    private final UtilityCardService utilityCardService;
    private final WalletService walletService;

    public UtilityCardController(UtilityCardService utilityCardService, WalletService walletService) {
        this.utilityCardService = utilityCardService;
        this.walletService = walletService;
    }

    /**
     * 查询水电卡余额
     *
     * @param id 水电卡ID
     * @return 卡片余额
     */
    @GetMapping("/card/{id}")
    public Result<Double> getCardBalance(@PathVariable("id") Long id) {
        // 参数验证：卡片ID不能为空
        if (id == null || id <= 0) {
            return Result.error("卡片ID无效");
        }

        // 查询卡片余额
        Double balance = utilityCardService.getCardBalance(id);

        if (balance == null) {
            return Result.error("卡片不存在");
        }

        return Result.success(balance);
    }

    /**
     * 水电卡充值（直接充值）
     *
     * 【核心业务逻辑】：充值前必须检查该房产是否存在欠费
     * 如果存在未缴的物业费或取暖费，系统将拦截充值操作
     *
     * @param cardId 水电卡ID
     * @param amount 充值金额
     * @return 充值结果
     */
    @PostMapping("/card/topup")
    public Result<String> topUp(@RequestParam("cardId") Long cardId, @RequestParam("amount") Double amount) {
        // 参数验证：卡片ID不能为空
        if (cardId == null || cardId <= 0) {
            return Result.error("卡片ID无效");
        }

        // 参数验证：金额必须为正数
        if (amount == null || amount <= 0) {
            return Result.error("充值金额必须大于0");
        }

        // 参数验证：金额上限检查
        if (amount > 10000) {
            return Result.error("单次充值金额不能超过1万元");
        }

        try {
            // 执行充值操作
            // 注意：Service层会自动检查欠费状态
            boolean success = utilityCardService.topUp(cardId, amount);

            if (success) {
                return Result.success("充值成功");
            } else {
                return Result.error("充值失败");
            }
        } catch (IllegalStateException e) {
            // 【关键拦截】：捕获欠费状态异常
            // 当用户存在未缴费用时，Service会抛出此异常
            return Result.error(e.getMessage());
        } catch (Exception e) {
            // 其他系统错误
            return Result.error("系统错误：" + e.getMessage());
        }
    }

    /**
     * 使用钱包余额为水电卡充值
     *
     * 【核心业务逻辑】：同样需要检查欠费状态
     *
     * @param userId 用户ID
     * @param cardId 水电卡ID
     * @param amount 充值金额
     * @return 充值结果
     */
    @PostMapping("/card/topup-wallet")
    public Result<String> topUpFromWallet(@RequestParam("userId") Long userId, @RequestParam("cardId") Long cardId,
            @RequestParam("amount") Double amount) {
        // 参数验证：用户ID不能为空
        if (userId == null || userId <= 0) {
            return Result.error("用户ID无效");
        }

        // 参数验证：卡片ID不能为空
        if (cardId == null || cardId <= 0) {
            return Result.error("卡片ID无效");
        }

        // 参数验证：金额必须为正数
        if (amount == null || amount <= 0) {
            return Result.error("充值金额必须大于0");
        }

        // 参数验证：金额上限检查
        if (amount > 10000) {
            return Result.error("单次充值金额不能超过1万元");
        }

        try {
            // 执行钱包转账充值
            // WalletService会检查欠费状态和钱包余额
            boolean success = walletService.topUpCardFromWallet(userId, cardId, amount);

            if (success) {
                return Result.success("钱包转账充值成功");
            } else {
                return Result.error("充值失败，请检查余额或卡片状态");
            }
        } catch (IllegalStateException e) {
            // 【关键拦截】：欠费或余额不足
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("系统错误：" + e.getMessage());
        }
    }
}
