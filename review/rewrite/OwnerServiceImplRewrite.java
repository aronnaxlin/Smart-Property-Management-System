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

@Service
public class OwnerServiceImplRewrite implements OwnerService {
    private final UserDAO userDAO;
    private final PropertyDAO propertyDAO;

    public OwnerServiceImplRewrite(UserDAO userDAO, PropertyDAO propertyDAO) {
        this.userDAO = userDAO;
        this.propertyDAO = propertyDAO;
    }

    @Override
    public List<Map<String, Object>> searchOwners(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<User> users = userDAO.searchByKeyword(keyword);
        for (User user : users) {
            List<Property> properties = propertyDAO.findByUserId(user.getUserId());

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

            List<Property> properties = propertyDAO.findByUserId(userId);

            result.put("properties", properties);
        }
        return result;
    }

    @Override
    public boolean updatePropertyOwner(Long propertyId, Long newOwnerId) {
        Property property = propertyDAO.findById(propertyId);
        if (property == null) {
            return false;
        }

        User newOwner = userDAO.findById(newOwnerId);
        if (newOwner == null) {
            return false;
        }

        property.setUserId(newOwnerId);
        return propertyDAO.update(property);
    }

}
