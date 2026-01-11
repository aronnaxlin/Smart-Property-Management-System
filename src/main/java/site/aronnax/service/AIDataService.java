package site.aronnax.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import site.aronnax.dao.FeeDAO;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserWalletDAO;
import site.aronnax.dao.UtilityCardDAO;
import site.aronnax.entity.Fee;
import site.aronnax.entity.Property;
import site.aronnax.entity.UserWallet;
import site.aronnax.entity.UtilityCard;

/**
 * AI 数据分析助手支撑服务
 * 专门为 AI 服务层提供定制化的数据视图，将复杂的底层库操作封装为 AI 易于解析的 Map 结构。
 *
 * 核心任务：
 * 1. 跨 DAO 整合：汇聚房产、账单、钱包多库数据。
 * 2. 数据降维：提取关键统计指标（如总欠费、收费率）。
 * 3. 结果翻译：将数据库的枚举/状态码自动转换为 AI 对话所要求的自然语言描述（中文）。
 */
@Service
public class AIDataService {

    private final FeeDAO feeDAO;
    private final PropertyDAO propertyDAO;
    private final UserWalletDAO walletDAO;
    private final UtilityCardDAO utilityCardDAO;

    public AIDataService(FeeDAO feeDAO, PropertyDAO propertyDAO, UserWalletDAO walletDAO,
            UtilityCardDAO utilityCardDAO) {
        this.feeDAO = feeDAO;
        this.propertyDAO = propertyDAO;
        this.walletDAO = walletDAO;
        this.utilityCardDAO = utilityCardDAO;
    }

    /**
     * 获取指定用户的完整欠费画像
     * 用于 AI 助手精准提醒业主其欠款详情。
     *
     * @param userId 业主用户 ID
     * @return 包含是否有欠费、总额、笔数及明细列表的视图数据
     */
    public Map<String, Object> getUserArrears(Long userId) {
        return getUserArrears(userId, userId, "OWNER");
    }

    /**
     * 获取指定用户的完整欠费画像（带权限控制）
     *
     * @param userId          业主用户 ID
     * @param requestUserId   请求用户 ID
     * @param requestUserType 请求用户类型
     * @return 包含是否有欠费、总额、笔数及明细列表的视图数据
     */
    public Map<String, Object> getUserArrears(Long userId, Long requestUserId, String requestUserType) {
        Map<String, Object> result = new HashMap<>();

        // 权限校验：业主只能查询自己的数据
        if (!"ADMIN".equalsIgnoreCase(requestUserType) && !userId.equals(requestUserId)) {
            result.put("hasArrears", false);
            result.put("message", "权限不足：您只能查询自己的账单信息");
            result.put("permissionDenied", true);
            return result;
        }

        // 1. 穿透房产层：业主可能拥有多套房
        List<Property> properties = propertyDAO.findByUserId(userId);

        if (properties.isEmpty()) {
            result.put("hasArrears", false);
            result.put("message", "系统中未查询到该用户的关联房产档案");
            return result;
        }

        // 2. 扁平化整合：统计各房产下的所有未缴单据
        List<Map<String, Object>> arrearsList = properties.stream()
                .flatMap(p -> feeDAO.findUnpaidByPropertyId(p.getpId()).stream())
                .map(fee -> {
                    Map<String, Object> feeInfo = new HashMap<>();
                    feeInfo.put("feeId", fee.getfId());
                    feeInfo.put("feeType", translateFeeType(fee.getFeeType())); // 自动转换为“物业费”等中文
                    feeInfo.put("amount", fee.getAmount());
                    feeInfo.put("createdAt", fee.getCreatedAt());
                    return feeInfo;
                })
                .collect(Collectors.toList());

        // 3. 计算聚合指标
        double totalArrears = arrearsList.stream()
                .mapToDouble(f -> (Double) f.get("amount"))
                .sum();

        result.put("hasArrears", !arrearsList.isEmpty());
        result.put("totalArrears", totalArrears);
        result.put("arrearsCount", arrearsList.size());
        result.put("arrearsList", arrearsList);

        return result;
    }

    /**
     * 获取业主钱包概况
     */
    public Map<String, Object> getUserWalletBalance(Long userId) {
        Map<String, Object> result = new HashMap<>();
        UserWallet wallet = walletDAO.findByUserId(userId);

        if (wallet == null) {
            result.put("hasWallet", false);
            result.put("balance", 0.0);
        } else {
            result.put("hasWallet", true);
            result.put("balance", wallet.getBalance());
            result.put("totalRecharged", wallet.getTotalRecharged());
        }
        return result;
    }

    /**
     * 获取关联房产的水电卡镜像数据
     * AI 将根据此数据告知业主是否需要充值。
     */
    public Map<String, Object> getUserUtilityCards(Long userId) {
        Map<String, Object> result = new HashMap<>();
        List<Property> properties = propertyDAO.findByUserId(userId);

        List<Map<String, Object>> cards = properties.stream()
                .flatMap(p -> {
                    List<UtilityCard> utilityCards = utilityCardDAO.findByPropertyId(p.getpId());
                    return utilityCards.stream().map(card -> {
                        Map<String, Object> cardInfo = new HashMap<>();
                        cardInfo.put("cardId", card.getCardId());
                        cardInfo.put("cardType", translateCardType(card.getCardType())); // “水卡”或“电卡”
                        cardInfo.put("balance", card.getBalance());
                        cardInfo.put("propertyInfo",
                                p.getBuildingNo() + "栋" + p.getUnitNo() + "单元" + p.getRoomNo() + "室");
                        return cardInfo;
                    });
                })
                .collect(Collectors.toList());

        result.put("cards", cards);
        result.put("cardCount", cards.size());
        return result;
    }

    /**
     * 【宏观分析】获取全区欠费统计（管理员 AI 模式专用）
     * 辅助 AI 识别当前运营风险点。
     *
     * @param requestUserType 请求用户类型
     */
    public Map<String, Object> getGlobalArrearsStatistics(String requestUserType) {
        Map<String, Object> result = new HashMap<>();

        // 权限校验：仅管理员可访问全局统计
        if (!"ADMIN".equalsIgnoreCase(requestUserType)) {
            result.put("permissionDenied", true);
            result.put("message", "此功能仅对管理员开放");
            return result;
        }

        List<Fee> unpaidFees = feeDAO.findUnpaidFees();

        // 维度统计：按费用类型汇总欠费金额
        Map<String, Double> feeTypeStats = unpaidFees.stream()
                .collect(Collectors.groupingBy(
                        Fee::getFeeType,
                        Collectors.summingDouble(Fee::getAmount)));

        // 类型转换与翻译
        Map<String, Double> translatedStats = feeTypeStats.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> translateFeeType(e.getKey()),
                        Map.Entry::getValue));

        double totalUnpaid = unpaidFees.stream()
                .mapToDouble(Fee::getAmount)
                .sum();

        result.put("totalUnpaidAmount", totalUnpaid);
        result.put("unpaidCount", unpaidFees.size());
        result.put("feeTypeStatistics", translatedStats);

        // 关联空间维度：排名欠费严重的楼栋（来自 PropertyDAO 联合查询）
        List<Map<String, Object>> buildingArrears = feeDAO.getArrearsByBuilding();
        result.put("topArrearsBuildings", buildingArrears);

        return result;
    }

    /**
     * 获取收费率全景（带权限控制）
     *
     * @param requestUserType 请求用户类型
     */
    public Map<String, Object> getCollectionRateStatistics(String requestUserType) {
        Map<String, Object> result = new HashMap<>();

        // 权限校验：仅管理员可访问
        if (!"ADMIN".equalsIgnoreCase(requestUserType)) {
            result.put("permissionDenied", true);
            result.put("message", "此功能仅对管理员开放");
            return result;
        }

        return feeDAO.getCollectionRate();
    }

    /**
     * 【管理员专用】获取所有业主概览（脱敏）
     * 返回业主总数、活跃业主等宏观指标，不包含敏感个人信息
     *
     * @param requestUserType 请求用户类型
     */
    public Map<String, Object> getAllOwnersOverview(String requestUserType) {
        Map<String, Object> result = new HashMap<>();

        // 权限校验：仅管理员可访问
        if (!"ADMIN".equalsIgnoreCase(requestUserType)) {
            result.put("permissionDenied", true);
            result.put("message", "业主信息查询仅对管理员开放");
            return result;
        }

        // 获取所有房产信息以统计业主
        List<Property> allProperties = propertyDAO.findAll();
        long totalProperties = allProperties.size();
        long occupiedProperties = allProperties.stream()
                .filter(p -> p.getUserId() != null)
                .count();

        result.put("totalProperties", totalProperties);
        result.put("occupiedProperties", occupiedProperties);
        result.put("vacantProperties", totalProperties - occupiedProperties);
        result.put("occupancyRate",
                totalProperties > 0 ? String.format("%.1f%%", (occupiedProperties * 100.0 / totalProperties)) : "0%");

        return result;
    }

    /**
     * 【管理员专用】获取风险楼栋分析
     * 识别欠费率高的楼栋，辅助管理决策
     *
     * @param requestUserType 请求用户类型
     */
    public Map<String, Object> getRiskBuildingAnalysis(String requestUserType) {
        Map<String, Object> result = new HashMap<>();

        // 权限校验：仅管理员可访问
        if (!"ADMIN".equalsIgnoreCase(requestUserType)) {
            result.put("permissionDenied", true);
            result.put("message", "楼栋分析功能仅对管理员开放");
            return result;
        }

        // 获取欠费楼栋统计
        List<Map<String, Object>> buildingArrears = feeDAO.getArrearsByBuilding();

        result.put("buildingCount", buildingArrears.size());
        result.put("topRiskBuildings", buildingArrears.stream()
                .limit(5) // 仅返回前5个高风险楼栋
                .collect(Collectors.toList()));

        return result;
    }

    /**
     * 业务字典翻译：将后端常量映射为语义化的中文
     */
    private String translateFeeType(String feeType) {
        Map<String, String> typeMap = Map.of(
                "PROPERTY_FEE", "物业费",
                "HEATING_FEE", "取暖费",
                "WATER_FEE", "水费",
                "ELECTRICITY_FEE", "电费");
        return typeMap.getOrDefault(feeType, feeType);
    }

    /**
     * 水电卡类型翻译
     */
    private String translateCardType(String cardType) {
        Map<String, String> typeMap = Map.of(
                "WATER", "水卡",
                "ELECTRICITY", "电卡");
        return typeMap.getOrDefault(cardType, cardType);
    }
}
