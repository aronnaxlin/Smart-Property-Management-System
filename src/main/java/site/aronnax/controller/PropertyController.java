package site.aronnax.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import site.aronnax.common.Result;
import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserDAO;
import site.aronnax.dao.UtilityCardDAO;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.entity.UtilityCard;

/**
 * 房产管理控制器
 * 提供房产档案的完整CRUD接口,面向管理端操作。
 *
 * @author Aronnax (Li Linhan)
 */
@RestController
@RequestMapping("/api/property")
public class PropertyController {

    private final PropertyDAO propertyDAO;
    private final UserDAO userDAO;
    private final UtilityCardDAO utilityCardDAO;

    public PropertyController(PropertyDAO propertyDAO, UserDAO userDAO, UtilityCardDAO utilityCardDAO) {
        this.propertyDAO = propertyDAO;
        this.userDAO = userDAO;
        this.utilityCardDAO = utilityCardDAO;
    }

    /**
     * 获取所有房产列表(包含业主姓名)
     *
     * @return 房产列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getAllProperties() {
        try {
            List<Property> properties = propertyDAO.findAll();
            List<Map<String, Object>> enrichedProperties = properties.stream().map(this::enrichPropertyWithOwnerName)
                    .collect(Collectors.toList());
            return Result.success(enrichedProperties != null ? enrichedProperties : List.of());
        } catch (Exception e) {
            return Result.error("获取房产列表失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID获取单个房产详情
     *
     * @param id 房产ID
     * @return 房产详情
     */
    @GetMapping("/{id}")
    public Result<Property> getPropertyById(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("房产ID无效");
        }

        try {
            Property property = propertyDAO.findById(id);
            if (property == null) {
                return Result.error("房产不存在");
            }
            return Result.success(property);
        } catch (Exception e) {
            return Result.error("获取房产详情失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定业主的所有房产
     *
     * @param userId 业主(用户)ID
     * @return 房产列表
     */
    @GetMapping("/owner/{userId}")
    public Result<List<Property>> getPropertiesByOwner(@PathVariable("userId") Long userId) {
        if (userId == null || userId <= 0) {
            return Result.error("业主ID无效");
        }

        try {
            List<Property> properties = propertyDAO.findByUserId(userId);
            return Result.success(properties != null ? properties : List.of());
        } catch (Exception e) {
            return Result.error("获取业主房产失败：" + e.getMessage());
        }
    }

    /**
     * 搜索房产
     * 支持按楼栋、单元、房号精确或模糊搜索
     *
     * @param building 楼栋号(可选)
     * @param unit     单元号(可选)
     * @param room     房号(可选)
     * @return 匹配的房产列表
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> searchProperties(
            @RequestParam(value = "building", required = false) String building,
            @RequestParam(value = "unit", required = false) String unit,
            @RequestParam(value = "room", required = false) String room) {

        try {
            // 如果提供了完整的楼-单元-房号,使用精确查询
            if (building != null && !building.isEmpty() &&
                    unit != null && !unit.isEmpty() &&
                    room != null && !room.isEmpty()) {
                Property property = propertyDAO.findByRoomInfo(building, unit, room);
                if (property != null) {
                    return Result.success(List.of(enrichPropertyWithOwnerName(property)));
                }
                return Result.success(List.of());
            }

            // 否则获取所有房产并过滤
            List<Property> allProperties = propertyDAO.findAll();
            List<Property> filtered = allProperties.stream()
                    .filter(p -> (building == null || building.isEmpty()
                            || p.getBuildingNo().contains(building))
                            && (unit == null || unit.isEmpty() || p.getUnitNo().contains(unit))
                            && (room == null || room.isEmpty() || p.getRoomNo().contains(room)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> enriched = filtered.stream()
                    .map(this::enrichPropertyWithOwnerName)
                    .collect(Collectors.toList());

            return Result.success(enriched);
        } catch (Exception e) {
            return Result.error("搜索房产失败：" + e.getMessage());
        }
    }

    /**
     * 创建新房产
     *
     * @param property 房产信息
     * @return 创建的房产ID
     */
    @PostMapping("/create")
    public Result<String> createProperty(@RequestBody Property property) {
        // 参数校验
        if (property.getBuildingNo() == null || property.getBuildingNo().trim().isEmpty()) {
            return Result.error("楼栋号不能为空");
        }
        if (property.getUnitNo() == null || property.getUnitNo().trim().isEmpty()) {
            return Result.error("单元号不能为空");
        }
        if (property.getRoomNo() == null || property.getRoomNo().trim().isEmpty()) {
            return Result.error("房号不能为空");
        }
        if (property.getArea() == null || property.getArea() <= 0) {
            return Result.error("面积必须大于0");
        }
        if (property.getpStatus() == null || property.getpStatus().trim().isEmpty()) {
            return Result.error("房产状态不能为空");
        }

        // 验证状态值
        String status = property.getpStatus().toUpperCase();
        if (!status.equals("SOLD") && !status.equals("UNSOLD") && !status.equals("RENTED")) {
            return Result.error("房产状态必须是SOLD、UNSOLD或RENTED");
        }
        property.setpStatus(status);

        try {
            // 检查房产是否已存在
            Property existing = propertyDAO.findByRoomInfo(
                    property.getBuildingNo(),
                    property.getUnitNo(),
                    property.getRoomNo());
            if (existing != null) {
                return Result.error("该房产已存在(楼栋-单元-房号重复)");
            }

            // 插入新房产
            propertyDAO.insert(property);

            // 查询刚创建的房产ID（通过楼栋-单元-房号查询）
            Property newProperty = propertyDAO.findByRoomInfo(
                    property.getBuildingNo(),
                    property.getUnitNo(),
                    property.getRoomNo());

            if (newProperty != null && newProperty.getpId() != null) {
                // 自动创建水卡
                UtilityCard waterCard = new UtilityCard();
                waterCard.setpId(newProperty.getpId());
                waterCard.setCardType("WATER");
                waterCard.setBalance(0.00);
                waterCard.setLastTopup(null);
                utilityCardDAO.insert(waterCard);

                // 自动创建电卡
                UtilityCard electricityCard = new UtilityCard();
                electricityCard.setpId(newProperty.getpId());
                electricityCard.setCardType("ELECTRICITY");
                electricityCard.setBalance(0.00);
                electricityCard.setLastTopup(null);
                utilityCardDAO.insert(electricityCard);
            }

            return Result.success("创建成功，已自动生成水卡和电卡");
        } catch (Exception e) {
            return Result.error("创建房产失败：" + e.getMessage());
        }
    }

    /**
     * 更新房产信息
     *
     * @param property 房产信息(必须包含pId)
     * @return 是否成功
     */
    @PutMapping("/update")
    public Result<String> updateProperty(@RequestBody Property property) {
        // 参数校验
        if (property.getpId() == null || property.getpId() <= 0) {
            return Result.error("房产ID无效");
        }
        if (property.getBuildingNo() == null || property.getBuildingNo().trim().isEmpty()) {
            return Result.error("楼栋号不能为空");
        }
        if (property.getUnitNo() == null || property.getUnitNo().trim().isEmpty()) {
            return Result.error("单元号不能为空");
        }
        if (property.getRoomNo() == null || property.getRoomNo().trim().isEmpty()) {
            return Result.error("房号不能为空");
        }
        if (property.getArea() == null || property.getArea() <= 0) {
            return Result.error("面积必须大于0");
        }
        if (property.getpStatus() == null || property.getpStatus().trim().isEmpty()) {
            return Result.error("房产状态不能为空");
        }

        // 验证状态值
        String status = property.getpStatus().toUpperCase();
        if (!status.equals("SOLD") && !status.equals("UNSOLD") && !status.equals("RENTED")) {
            return Result.error("房产状态必须是SOLD、UNSOLD或RENTED");
        }
        property.setpStatus(status);

        try {
            // 检查房产是否存在
            Property existingProperty = propertyDAO.findById(property.getpId());
            if (existingProperty == null) {
                return Result.error("房产不存在");
            }

            // 如果修改了楼栋/单元/房号,检查是否与其他房产冲突
            if (!existingProperty.getBuildingNo().equals(property.getBuildingNo())
                    || !existingProperty.getUnitNo().equals(property.getUnitNo())
                    || !existingProperty.getRoomNo().equals(property.getRoomNo())) {
                Property duplicate = propertyDAO.findByRoomInfo(
                        property.getBuildingNo(),
                        property.getUnitNo(),
                        property.getRoomNo());
                if (duplicate != null && !duplicate.getpId().equals(property.getpId())) {
                    return Result.error("该房产已被其他记录使用(楼栋-单元-房号重复)");
                }
            }

            // 执行更新
            boolean success = propertyDAO.update(property);
            if (success) {
                return Result.success("更新成功");
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            return Result.error("更新房产失败：" + e.getMessage());
        }
    }

    /**
     * 删除房产
     *
     * @param id 房产ID
     * @return 是否成功
     */
    @DeleteMapping("/{id}")
    public Result<String> deleteProperty(@PathVariable("id") Long id) {
        if (id == null || id <= 0) {
            return Result.error("房产ID无效");
        }

        try {
            // 检查房产是否存在
            Property property = propertyDAO.findById(id);
            if (property == null) {
                return Result.error("房产不存在");
            }

            // 执行删除
            boolean success = propertyDAO.deleteById(id);
            if (success) {
                return Result.success("删除成功");
            }
            return Result.error("删除失败");
        } catch (Exception e) {
            return Result.error("删除房产失败：" + e.getMessage());
        }
    }

    /**
     * 辅助方法: 为房产信息enrichi业主姓名
     */
    private Map<String, Object> enrichPropertyWithOwnerName(Property property) {
        Map<String, Object> map = new HashMap<>();
        map.put("pId", property.getpId());
        map.put("buildingNo", property.getBuildingNo());
        map.put("unitNo", property.getUnitNo());
        map.put("roomNo", property.getRoomNo());
        map.put("area", property.getArea());
        map.put("pStatus", property.getpStatus());
        map.put("userId", property.getUserId());

        // 查询业主姓名
        if (property.getUserId() != null) {
            User owner = userDAO.findById(property.getUserId());
            map.put("ownerName", owner != null ? owner.getName() : "未知业主");
        } else {
            map.put("ownerName", "-");
        }

        return map;
    }
}
