package site.aronnax.service;

import java.util.List;

import jakarta.servlet.http.HttpSession;
import site.aronnax.entity.WalletTransaction;

/**
 * 电子钱包业务接口
 * 系统核心金融层，处理充值、缴费、卡片转账及交易流水追踪。
 *
 * @author Aronnax (Li Linhan)
 * @version 1.0
 */
public interface WalletService {

    /**
     * 钱包充值
     * 业主向其电子钱包充值余额。
     *
     * @param userId 用户 ID
     * @param amount 金额
     * @return 是否成功
     */
    boolean rechargeWallet(Long userId, Double amount);

    /**
     * 使用钱包余额缴纳指定账单
     * 事务性操作：扣减余额 -> 更新账单状态 -> 记录交易流水。
     *
     * 【权限控制】：
     * - 业主可缴纳物业费、取暖费
     * - 管理员可代缴物业费、取暖费，但不能代缴水电费
     *
     * @param feeId   账单 ID
     * @param session HTTP会话，用于获取用户角色
     * @return 是否支付成功
     */
    boolean payFeeFromWallet(Long feeId, HttpSession session);

    /**
     * 从钱包余额向水电卡充值（转账）
     *
     * 【核心风控逻辑】：
     * 执行前必须调用 checkWalletArrears 检查该用户是否存在未结清的物业/取暖费。
     * 欠费用户禁止将余额转入水电卡消费（实现“欠费锁定”）。
     *
     * @param userId 用户 ID
     * @param cardId 目标水电卡 ID
     * @param amount 转账金额
     * @return 是否处理成功
     */
    boolean topUpCardFromWallet(Long userId, Long cardId, Double amount);

    /**
     * 获取钱包当前可用余额
     */
    Double getWalletBalance(Long userId);

    /**
     * 检索钱包的交易历史记录（充值、扣款等）
     */
    List<WalletTransaction> getTransactionHistory(Long userId);

    /**
     * 为新注册用户初始化空钱包
     */
    boolean createWallet(Long userId);
}
