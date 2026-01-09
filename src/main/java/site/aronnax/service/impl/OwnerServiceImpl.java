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
 * Owner Service Implementation
 * Implements owner management business logic
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

    @Override
    public List<Map<String, Object>> searchOwners(String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Search users by name or phone
        List<User> users = userDAO.searchByKeyword(keyword);

        for (User user : users) {
            // Get properties owned by this user
            List<Property> properties = propertyDAO.findByUserId(user.getUserId());
            if (properties.isEmpty()) {
                // Should we list owners without properties? Requirements said "Owner Management
                // of residents".
                // Let's assume yes, or we can just list empty properties.
                // But the loop below depends on properties.
                // Let's add user info even if no property found?
                // The map keys suggest property info is expected.
                // If the user has no property, they might not be an "owner" strictly speaking
                // in some contexts, but they are in User table.
                // Let's just follow existing logic: loop properties. If no properties, no
                // result added.
            }

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

        // Verify new owner exists
        User newOwner = userDAO.findById(newOwnerId);
        if (newOwner == null) {
            return false;
        }

        property.setUserId(newOwnerId);
        return propertyDAO.update(property);
    }
}
