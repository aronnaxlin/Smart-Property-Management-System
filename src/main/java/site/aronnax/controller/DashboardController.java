package site.aronnax.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.dao.FeeDAO;

/**
 * 看板数据控制器
 * 为管理首页提供多维度的财务统计展示。
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final FeeDAO feeDAO;

    public DashboardController(FeeDAO feeDAO) {
        this.feeDAO = feeDAO;
    }

    /**
     * 加载看板核心统计指标
     * 包含：年度缴费率、收入构成分布、欠费楼栋排行。
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        // 同步从 DAO 层提取多维聚合数据
        stats.put("collectionRate", feeDAO.getCollectionRate());
        stats.put("incomeDistribution", feeDAO.getIncomeDistribution());
        stats.put("arrearsByBuilding", feeDAO.getArrearsByBuilding());

        return Result.success(stats);
    }
}
