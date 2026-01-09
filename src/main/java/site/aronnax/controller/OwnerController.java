package site.aronnax.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.service.OwnerService;

/**
 * 业主管理控制器
 * 提供业主信息查询、房产查询和产权变更功能
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/owner")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    /**
     * 搜索业主信息
     * 支持按姓名、电话等关键词模糊搜索
     *
     * @param keyword 搜索关键词（可选，为空时返回所有业主）
     * @return 符合条件的业主列表
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        // 参数验证：防止恶意超长查询
        if (keyword != null && keyword.length() > 50) {
            return Result.error("搜索关键词过长");
        }

        try {
            // 执行搜索
            List<Map<String, Object>> results = ownerService.searchOwners(keyword);

            // 返回结果（如果为空则返回空列表）
            return Result.success(results != null ? results : List.of());
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 获取业主详细信息
     * 包括业主基本信息和名下所有房产
     *
     * @param id 业主ID
     * @return 业主详细信息
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOwnerDetail(@PathVariable("id") Long id) {
        // 参数验证：业主ID不能为空
        if (id == null || id <= 0) {
            return Result.error("业主ID无效");
        }

        try {
            // 查询业主及其房产信息
            Map<String, Object> ownerDetail = ownerService.getOwnerWithProperties(id);

            if (ownerDetail == null || ownerDetail.isEmpty()) {
                return Result.error("业主不存在");
            }

            return Result.success(ownerDetail);
        } catch (Exception e) {
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 房产产权变更
     * 将指定房产的所有权转移给新业主
     *
     * @param propertyId 房产ID
     * @param newOwnerId 新业主ID
     * @return 变更结果
     */
    @PostMapping("/property/transfer")
    public Result<String> transferProperty(@RequestParam("propertyId") Long propertyId,
            @RequestParam("newOwnerId") Long newOwnerId) {
        // 参数验证：房产ID不能为空
        if (propertyId == null || propertyId <= 0) {
            return Result.error("房产ID无效");
        }

        // 参数验证：新业主ID不能为空
        if (newOwnerId == null || newOwnerId <= 0) {
            return Result.error("新业主ID无效");
        }

        try {
            // 执行产权变更
            boolean success = ownerService.updatePropertyOwner(propertyId, newOwnerId);

            if (success) {
                return Result.success("产权变更成功");
            }

            return Result.error("产权变更失败，请检查房产ID和业主ID是否有效");
        } catch (Exception e) {
            return Result.error("变更失败：" + e.getMessage());
        }
    }
}
