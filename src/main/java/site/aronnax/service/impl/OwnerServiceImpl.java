package site.aronnax.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import site.aronnax.dao.PropertyDAO;
import site.aronnax.dao.UserDAO;
import site.aronnax.entity.Property;
import site.aronnax.entity.User;
import site.aronnax.service.OwnerService;

/**
 * 业主服务实现类
 * 维护系统核心的“人-房”关联关系，支持管理后台的人员检索。
 *
 * @author Aronnax (Li Linhan)
 */
@Service
public class OwnerServiceImpl implements OwnerService {

    private final UserDAO userDAO;
    private final PropertyDAO propertyDAO;

    public OwnerServiceImpl(UserDAO userDAO, PropertyDAO propertyDAO) {
        this.userDAO = userDAO;
        this.propertyDAO = propertyDAO;
    }

    /**
     * 业主组合搜索逻辑
     * 实现：先从用户表检索人员，再反向查询每个人的房产情况。
     */
    @Override
    public List<Map<String, Object>> searchOwners(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        // 1. 根据关键词在 User 表中进行模糊匹配（姓名或手机号）
        List<User> users = userDAO.searchByKeyword(keyword);

        for (User user : users) {
            // 2. 溯访该用户名下的所有房产档案
            List<Property> properties = propertyDAO.findByUserId(user.getUserId());

            // 注：若用户已注册但尚未录入房产，则暂时不作为“业主”出现在此列表中
            for (Property property : properties) {
                Map<String, Object> ownerInfo = new HashMap<>();
                ownerInfo.put("user_id", user.getUserId());
                ownerInfo.put("name", user.getName());
                ownerInfo.put("phone", user.getPhone());
                ownerInfo.put("gender", user.getGender());
                ownerInfo.put("property_id", property.getpId());
                ownerInfo.put("building_no", property.getBuildingNo());
                ownerInfo.put("unit_no", property.getUnitNo());
                ownerInfo.put("room_no", property.getRoomNo());
                ownerInfo.put("area", property.getArea());
                ownerInfo.put("status", property.getpStatus());

                results.add(ownerInfo);
            }
        }
        return results;
    }

    /**
     * 获取指定业主名下的资产全景图
     */
    @Override
    public Map<String, Object> getOwnerWithProperties(Long userId) {
        Map<String, Object> result = new HashMap<>();

        User owner = userDAO.findById(userId);
        if (owner != null) {
            result.put("user_id", owner.getUserId());
            result.put("user_name", owner.getUserName());
            result.put("name", owner.getName());
            result.put("phone", owner.getPhone());
            result.put("gender", owner.getGender());
            result.put("user_type", owner.getUserType());

            // 同步拉取该业主的所有房产实体
            List<Property> properties = propertyDAO.findByUserId(userId);
            result.put("properties", properties);
        }

        return result;
    }

    /**
     * 变更房产归属（过户业务）
     */
    @Override
    public boolean updatePropertyOwner(Long propertyId, Long newOwnerId) {
        Property property = propertyDAO.findById(propertyId);
        if (property == null) {
            return false;
        }

        // 校验：目标业主 ID 必须存在于 User 表中
        User newOwner = userDAO.findById(newOwnerId);
        if (newOwner == null) {
            return false;
        }

        property.setUserId(newOwnerId);
        return propertyDAO.update(property);
    }
}
