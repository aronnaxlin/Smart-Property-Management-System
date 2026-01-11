package site.aronnax.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import site.aronnax.common.Result;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.entity.UtilityCard;
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
    private final PropertyDAO propertyDAO;

    public UtilityCardController(UtilityCardService utilityCardService, WalletService walletService,
            PropertyDAO propertyDAO) {
        this.utilityCardService = utilityCardService;
        this.walletService = walletService;
        this.propertyDAO = propertyDAO;
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
     * 获取当前用户的所有水电卡
     * 业主视角：显示名下所有房产的水电卡
     * 管理员视角：此接口不适用
     */
    @GetMapping("/my-cards")
    public Result<List<Map<String, Object>>> getMyCards(HttpSession session) {
        // 获取当前登录用户
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        Long userId = currentUser.getUserId();

        try {
            // 调用 service 获取用户所有水电卡信息
            List<Map<String, Object>> cards = utilityCardService.getUserCards(userId);
            return Result.success(cards);
        } catch (Exception e) {
            return Result.error("获取水电卡信息失败: " + e.getMessage());
        }
    }

    /**
     * 水电卡钱包充值（唯一充值入口）
     *
     * 权限控制：
     * - 业主：只能充值自己名下房产的卡
     * - 管理员：可充值任何卡，但扣除的是卡片所属业主的钱包
     */
    @PostMapping("/card/topup")
    public Result<String> topUp(@RequestParam("cardId") Long cardId,
            @RequestParam("amount") Double amount,
            HttpSession session) {
        // 1. 获取当前登录用户
        User currentUser = (User) session.getAttribute("user");
        if (currentUser == null) {
            return Result.error("用户未登录");
        }

        // 2. 基础报文校验
        if (cardId == null || cardId <= 0) {
            return Result.error("充值目标卡号非法");
        }
        if (amount == null || amount <= 0) {
            return Result.error("充值金额必须大于零");
        }

        // 3. 额度保护：防止套现或错误大额输入
        if (amount > 10000) {
            return Result.error("超过单次充值限额（10000元）");
        }

        try {
            // 4. 查询卡片信息
            UtilityCard card = utilityCardService.findById(cardId);
            if (card == null) {
                return Result.error("未找到对应的水电卡");
            }

            // 5. 查询卡片所属房产和业主
            Property property = propertyDAO.findById(card.getpId());
            if (property == null || property.getUserId() == null) {
                return Result.error("房产信息异常");
            }
            Long ownerId = property.getUserId();

            // 6. 权限验证
            if ("OWNER".equals(currentUser.getUserType())) {
                // 业主：只能充值自己的卡
                if (!currentUser.getUserId().equals(ownerId)) {
                    return Result.error("您只能为自己的水电卡充值");
                }
            }
            // 管理员：可以充值任何卡

            // 7. 执行充值，扣除业主钱包（不是当前用户）
            boolean success = walletService.topUpCardFromWallet(ownerId, cardId, amount);
            return success ? Result.success("钱包扣款并充值成功") : Result.error("操作失败：可能由于余额不足或欠费未结清");
        } catch (IllegalStateException e) {
            // 【关键反馈】：捕获欠费锁定异常
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("充值通道负载中，请稍后重试：" + e.getMessage());
        }
    }

    /**
     * @deprecated 此endpoint已合并到 /card/topup，保留用于兼容旧版前端
     *             电子钱包余额转充水电卡
     *
     *             此操作涉及内部资金流转，风控优先级最高。
     */
    @Deprecated
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
