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
 * 面向业主提供水费、电费的自动化充值与查询服务。
 *
 * 【重要拦截点】：本类是前端触发“欠费锁定”异常的核心入口。
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
     * 水电卡实时余额查询
     */
    @GetMapping("/card/{id}")
    public Result<Double> getCardBalance(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("卡号无效");
        }

        Double balance = utilityCardService.getCardBalance(id);
        if (balance == null) {
            return Result.error("未找到对应的卡片档案");
        }

        return Result.success(balance);
    }

    /**
     * 在线渠道充值
     *
     * 【核心风控环节】：
     * 用户在此处提交充值请求时，Service 层会穿透检查该房产的物业费缴纳情况。
     * 若捕获到 IllegalStateException，则表明触发了“欠费锁定”限制。
     */
    @PostMapping("/card/topup")
    public Result<String> topUp(@RequestParam("cardId") Long cardId, @RequestParam("amount") Double amount) {
        // 1. 基础报文校验
        if (cardId == null || cardId <= 0) {
            return Result.error("充值目标卡号非法");
        }
        if (amount == null || amount <= 0) {
            return Result.error("充值金额必须大于零");
        }

        // 2. 额度保护：防止套现或错误大额输入
        if (amount > 10000) {
            return Result.error("超过单次充值限额（10000元）");
        }

        try {
            // 执行业务流（内含欠费拦截逻辑）
            boolean success = utilityCardService.topUp(cardId, amount);
            return success ? Result.success("充值款项已入账") : Result.error("账户状态异常，充值未生效");
        } catch (IllegalStateException e) {
            // 【关键反馈】：捕获 Service 层抛出的“欠费锁定”异常，回传给前端显示具体的催缴信息
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("充值通道负载中，请稍后重试：" + e.getMessage());
        }
    }

    /**
     * 电子钱包余额转充水电卡
     *
     * 此操作涉及内部资金流转，风控优先级最高。
     */
    @PostMapping("/card/topup-wallet")
    public Result<String> topUpFromWallet(@RequestParam("userId") Long userId, @RequestParam("cardId") Long cardId,
            @RequestParam("amount") Double amount) {
        if (userId == null || userId <= 0) {
            return Result.error("用户信息校验失败");
        }
        if (cardId == null || cardId <= 0) {
            return Result.error("目标卡号无效");
        }
        if (amount == null || amount <= 0) {
            return Result.error("转账金额必须非负");
        }

        if (amount > 10000) {
            return Result.error("超过单次转账限额");
        }

        try {
            // 执行内部转账流水
            boolean success = walletService.topUpCardFromWallet(userId, cardId, amount);
            return success ? Result.success("钱包扣款并充值成功") : Result.error("操作失败：可能由于余额不足或权限受阻");
        } catch (IllegalStateException e) {
            // 欠费锁定或资金冻结反馈
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("转账处理时发生系统级异常：" + e.getMessage());
        }
    }
}
