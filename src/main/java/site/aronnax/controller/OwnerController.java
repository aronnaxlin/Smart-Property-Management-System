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
 * 处理业主档案检索、详情查看及房产过户业务。
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
     * 条件搜索业主
     * 支持姓名、手机号等模糊匹配。
     *
     * @param keyword 检索关键字
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(
            @RequestParam(value = "keyword", defaultValue = "") String keyword) {
        // 安全前置：限制关键字长度，防止扫描探测
        if (keyword != null && keyword.length() > 50) {
            return Result.error("搜索词超出长度限制");
        }

        try {
            List<Map<String, Object>> results = ownerService.searchOwners(keyword);
            return Result.success(results != null ? results : List.of());
        } catch (Exception e) {
            return Result.error("检索链路异常：" + e.getMessage());
        }
    }

    /**
     * 业主档案详情
     * 返回业主基本资料及其名下的资产配置（房产列表）。
     *
     * @param id 用户唯一标识
     */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> getOwnerDetail(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("业主 ID 获取失败");
        }

        try {
            Map<String, Object> ownerDetail = ownerService.getOwnerWithProperties(id);

            if (ownerDetail == null || ownerDetail.isEmpty()) {
                return Result.error("该业主档案不存在或已被注销");
            }

            return Result.success(ownerDetail);
        } catch (Exception e) {
            return Result.error("详情加载失败：" + e.getMessage());
        }
    }

    /**
     * 房产过户业务
     * 变更房产档案的归属人。
     *
     * @param propertyId 目标房产 ID
     * @param newOwnerId 接收方业主 ID
     */
    @PostMapping("/property/transfer")
    public Result<String> transferProperty(@RequestParam("propertyId") Long propertyId,
            @RequestParam("newOwnerId") Long newOwnerId) {
        if (propertyId == null || propertyId <= 0) {
            return Result.error("请选择有效的房产");
        }

        if (newOwnerId == null || newOwnerId <= 0) {
            return Result.error("请选择有效的新业主");
        }

        try {
            // 执行业务变更
            boolean success = ownerService.updatePropertyOwner(propertyId, newOwnerId);

            if (success) {
                return Result.success("房产权属变更已生效");
            }

            return Result.error("过户失败：系统核验不通过，请确认人员与房产状态");
        } catch (Exception e) {
            return Result.error("服务端处理异常：" + e.getMessage());
        }
    }
}
