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
 * 提供账单创建、批量创建、欠费查询和缴费功能
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
     * 创建单笔账单
     *
     * @param propertyId 房产ID
     * @param feeType    费用类型（PROPERTY_FEE/HEATING_FEE/WATER_FEE/ELECTRICITY_FEE）
     * @param amount     账单金额
     * @return 创建结果
     */
    @PostMapping("/create")
    public Result<String> createFee(@RequestParam("propertyId") Long propertyId,
            @RequestParam("feeType") String feeType,
            @RequestParam("amount") Double amount) {
        // 参数验证：房产ID不能为空
        if (propertyId == null || propertyId <= 0) {
            return Result.error("房产ID无效");
        }

        // 参数验证：费用类型不能为空
        if (feeType == null || feeType.trim().isEmpty()) {
            return Result.error("费用类型不能为空");
        }

        // 参数验证：金额必须大于0
        if (amount == null || amount <= 0) {
            return Result.error("账单金额必须大于0");
        }

        // 参数验证：金额上限检查
        if (amount > 100000) {
            return Result.error("单笔账单金额不能超过10万元");
        }

        try {
            // 创建账单
            feeService.createFee(propertyId, feeType, amount);

            // 注意：当前DAO实现可能返回null，但只要不抛异常即表示成功
            return Result.success("账单创建成功");
        } catch (Exception e) {
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 批量创建账单
     * 为多个房产创建相同类型和金额的账单
     *
     * @param params 包含propertyIds（房产ID列表）、feeType和amount的参数Map
     * @return 批量创建结果
     */
    @PostMapping("/batch-create")
    public Result<String> batchCreate(@RequestBody Map<String, Object> params) {
        // 参数验证：提取并验证参数
        if (params == null || params.isEmpty()) {
            return Result.error("参数不能为空");
        }

        // 提取房产ID列表
        @SuppressWarnings("unchecked")
        List<Long> propertyIds = (List<Long>) params.get("propertyIds");
        if (propertyIds == null || propertyIds.isEmpty()) {
            return Result.error("房产ID列表不能为空");
        }

        // 提取费用类型
        String feeType = (String) params.get("feeType");
        if (feeType == null || feeType.trim().isEmpty()) {
            return Result.error("费用类型不能为空");
        }

        // 提取金额（处理可能的类型转换）
        Double amount;
        try {
            Object amountObj = params.get("amount");
            if (amountObj instanceof Integer) {
                amount = ((Integer) amountObj).doubleValue();
            } else if (amountObj instanceof Double) {
                amount = (Double) amountObj;
            } else {
                return Result.error("金额格式不正确");
            }
        } catch (Exception e) {
            return Result.error("金额格式不正确");
        }

        // 验证金额
        if (amount <= 0) {
            return Result.error("账单金额必须大于0");
        }

        try {
            // 批量创建账单
            int count = feeService.batchCreateFees(propertyIds, feeType, amount);
            return Result.success("批量创建成功，共创建 " + count + " 条账单");
        } catch (Exception e) {
            return Result.error("批量创建失败：" + e.getMessage());
        }
    }

    /**
     * 查询欠费列表
     * 返回所有未缴纳的账单及相关业主、房产信息
     *
     * @return 欠费列表
     */
    @GetMapping("/arrears")
    public Result<List<Map<String, Object>>> getArrearsList() {
        try {
            List<Map<String, Object>> arrearsList = feeService.getArrearsList();
            return Result.success(arrearsList != null ? arrearsList : List.of());
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 标记账单为已支付
     *
     * @param feeId 账单ID
     * @return 支付结果
     */
    @PostMapping("/pay/{feeId}")
    public Result<String> payFee(@PathVariable("feeId") Long feeId) {
        // 参数验证：账单ID不能为空
        if (feeId == null || feeId <= 0) {
            return Result.error("账单ID无效");
        }

        try {
            // 标记账单为已支付
            boolean success = feeService.payFee(feeId);

            if (success) {
                return Result.success("支付标记成功");
            }

            return Result.error("支付失败，账单不存在");
        } catch (Exception e) {
            return Result.error("支付失败：" + e.getMessage());
        }
    }
}
