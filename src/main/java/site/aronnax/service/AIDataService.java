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
 * AI数据分析服务
 * 为AI助手提供数据查询和分析能力
 *
 * 功能：
 * 1. 查询用户欠费信息
 * 2. 查询账单详情
 * 3. 查询钱包和水电卡余额
 * 4. 统计数据分析（管理员专用）
 *
 * @author Aronnax (Li Linhan)
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
     * 查询用户的欠费信息
     *
     * @param userId 用户ID
     * @return 欠费信息Map
     */
    public Map<String, Object> getUserArrears(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户的所有房产
        List<Property> properties = propertyDAO.findByUserId(userId);

        if (properties.isEmpty()) {
            result.put("hasArrears", false);
            result.put("message", "该用户没有房产");
            return result;
        }

        // 查询所有房产的欠费
        List<Map<String, Object>> arrearsList = properties.stream()
                .flatMap(p -> feeDAO.findUnpaidByPropertyId(p.getpId()).stream())
                .map(fee -> {
                    Map<String, Object> feeInfo = new HashMap<>();
                    feeInfo.put("feeId", fee.getfId());
                    feeInfo.put("feeType", translateFeeType(fee.getFeeType()));
                    feeInfo.put("amount", fee.getAmount());
                    feeInfo.put("createdAt", fee.getCreatedAt());
                    return feeInfo;
                })
                .collect(Collectors.toList());

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
     * 查询用户钱包余额
     *
     * @param userId 用户ID
     * @return 钱包余额信息
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
     * 查询用户水电卡余额
     *
     * @param userId 用户ID
     * @return 水电卡余额信息
     */
    public Map<String, Object> getUserUtilityCards(Long userId) {
        Map<String, Object> result = new HashMap<>();

        // 获取用户的所有房产
        List<Property> properties = propertyDAO.findByUserId(userId);

        List<Map<String, Object>> cards = properties.stream()
                .flatMap(p -> {
                    // 查询该房产的水电卡
                    List<UtilityCard> utilityCards = utilityCardDAO.findByPropertyId(p.getpId());
                    return utilityCards.stream().map(card -> {
                        Map<String, Object> cardInfo = new HashMap<>();
                        cardInfo.put("cardId", card.getCardId());
                        cardInfo.put("cardType", translateCardType(card.getCardType()));
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
     * 获取全局欠费统计（管理员专用）
     *
     * @return 欠费统计信息
     */
    public Map<String, Object> getGlobalArrearsStatistics() {
        Map<String, Object> result = new HashMap<>();

        // 获取所有未缴费用
        List<Fee> unpaidFees = feeDAO.findUnpaidFees();

        // 按费用类型统计
        Map<String, Double> feeTypeStats = unpaidFees.stream()
                .collect(Collectors.groupingBy(
                        Fee::getFeeType,
                        Collectors.summingDouble(Fee::getAmount)));

        // 转换为中文
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

        // 获取欠费最多的楼栋
        List<Map<String, Object>> buildingArrears = feeDAO.getArrearsByBuilding();
        result.put("topArrearsBuildings", buildingArrears);

        return result;
    }

    /**
     * 获取收费率统计（管理员专用）
     *
     * @return 收费率信息
     */
    public Map<String, Object> getCollectionRateStatistics() {
        return feeDAO.getCollectionRate();
    }

    /**
     * 翻译费用类型为中文
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
     * 翻译卡片类型为中文
     */
    private String translateCardType(String cardType) {
        Map<String, String> typeMap = Map.of(
                "WATER", "水卡",
                "ELECTRICITY", "电卡");
        return typeMap.getOrDefault(cardType, cardType);
    }
}
