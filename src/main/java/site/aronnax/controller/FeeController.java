package site.aronnax.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.service.FeeService;

/**
 * 费用管理控制器
 * 面向物业管理端，提供账单的精准派发、批量操作及欠费全局监控。
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/fee")
public class FeeController {

    private final FeeService feeService;

    public FeeController(FeeService feeService) {
        this.feeService = feeService;
    }

    /**
     * 下发单笔费用单据
     * 适用于针对特定房产的临时收费（如特殊维护费）。
     *
     * @param propertyId 房产主键 ID
     * @param feeType    待生成的费用类型
     * @param amount     账单面额
     */
    @PostMapping("/create")
    public Result<String> createFee(@RequestParam("propertyId") Long propertyId,
            @RequestParam("feeType") String feeType,
            @RequestParam("amount") Double amount) {
        // 参数前置校验：确保数据有效性
        if (propertyId == null || propertyId <= 0) {
            return Result.error("无效的房产标识");
        }
        if (feeType == null || feeType.trim().isEmpty()) {
            return Result.error("费用类型未指定");
        }
        if (amount == null || amount <= 0) {
            return Result.error("计费金额必须大于零");
        }

        // 风控拦截：防止逻辑溢出或非法输入
        if (amount > 100000) {
            return Result.error("单张账单金额已超过系统上限（10万元）");
        }

        try {
            // 路由至业务层，执行入库逻辑
            feeService.createFee(propertyId, feeType, amount);
            return Result.success("费用下发成功");
        } catch (Exception e) {
            return Result.error("下发失败，请检查数据一致性：" + e.getMessage());
        }
    }

    /**
     * 批量创建功能
     * 典型场景：物业费年度统扣、取暖季统扣。
     *
     * @param params 映射列表，需包含 propertyIds, feeType 和 amount
     */
    @PostMapping("/batch-create")
    public Result<String> batchCreate(@RequestBody Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return Result.error("报文负载缺失");
        }

        // 提取待处理的房产 ID 集合
        @SuppressWarnings("unchecked")
        List<Long> propertyIds = (List<Long>) params.get("propertyIds");
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Result.error("目标房产范围不能为空");
        }

        String feeType = (String) params.get("feeType");
        if (feeType == null || feeType.trim().isEmpty()) {
            return Result.error("费用分类未定义");
        }

        // 金额类型适应性解析（兼容 JSON 数值的不同表现形式）
        Double amount;
        try {
            Object amountObj = params.get("amount");
            if (amountObj instanceof Integer) {
                amount = ((Integer) amountObj).doubleValue();
            } else if (amountObj instanceof Double) {
                amount = (Double) amountObj;
            } else {
                return Result.error("计费额度格式解析异常");
            }
        } catch (Exception e) {
            return Result.error("金额数据类型错误");
        }

        if (amount <= 0) {
            return Result.error("计费值必须为正数");
        }

        try {
            // 调用业务层执行批量插入事务
            int count = feeService.batchCreateFees(propertyIds, feeType, amount);
            return Result.success("批量计费完成，已生成 " + count + " 个房产的应缴账单");
        } catch (Exception e) {
            return Result.error("批量执行失败，事务已回滚：" + e.getMessage());
        }
    }

    /**
     * 欠费全局清册接口
     * 提供已逾期未缴纳的所有单据视图，含业主联系信息。
     */
    @GetMapping("/arrears")
    public Result<List<Map<String, Object>>> getArrearsList() {
        try {
            List<Map<String, Object>> arrearsList = feeService.getArrearsList();
            return Result.success(arrearsList != null ? arrearsList : List.of());
        } catch (Exception e) {
            return Result.error("获取统计视图失败：" + e.getMessage());
        }
    }

    /**
     * 标记人工缴费
     * 用于物业前台线下收银后，手动同步系统状态。
     */
    @PostMapping("/pay/{feeId}")
    public Result<String> payFee(@PathVariable("feeId") Long feeId) {
        if (feeId == null || feeId <= 0) {
            return Result.error("账单流水号非法");
        }

        try {
            boolean success = feeService.payFee(feeId);
            if (success) {
                return Result.success("支付状态同步成功");
            }
            return Result.error("同步失败，未查询到该账单号");
        } catch (Exception e) {
            return Result.error("服务端异常：" + e.getMessage());
        }
    }
}
