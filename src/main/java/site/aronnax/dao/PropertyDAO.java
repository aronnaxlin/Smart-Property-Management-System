package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.Property;

/**
 * Property Data Access Object
 * Implemented with Spring JdbcTemplate
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class PropertyDAO {

    private final JdbcTemplate jdbcTemplate;

    public PropertyDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("null")
    private final RowMapper<Property> propertyRowMapper = (rs, rowNum) -> {
        Property property = new Property();
        property.setpId(rs.getLong("p_id"));
        property.setBuildingNo(rs.getString("building_no"));
        property.setUnitNo(rs.getString("unit_no"));
        property.setRoomNo(rs.getString("room_no"));
        property.setArea(rs.getDouble("area"));
        property.setpStatus(rs.getString("p_status"));
        property.setUserId(rs.getLong("user_id"));
        if (rs.wasNull()) {
            property.setUserId(null);
        }
        return property;
    };

    public Long insert(Property property) {
        String sql = "INSERT INTO properties (building_no, unit_no, room_no, area, p_status, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, property.getBuildingNo(), property.getUnitNo(), property.getRoomNo(),
                property.getArea(), property.getpStatus(), property.getUserId());
        return null; // ID retrieval omitted for simplicity
    }

    public boolean deleteById(Long pId) {
        String sql = "DELETE FROM properties WHERE p_id = ?";
        return jdbcTemplate.update(sql, pId) > 0;
    }

    public boolean update(Property property) {
        String sql = "UPDATE properties SET building_no=?, unit_no=?, room_no=?, area=?, p_status=?, user_id=? WHERE p_id=?";
        return jdbcTemplate.update(sql, property.getBuildingNo(), property.getUnitNo(), property.getRoomNo(),
                property.getArea(), property.getpStatus(), property.getUserId(), property.getpId()) > 0;
    }

    public Property findById(Long pId) {
        String sql = "SELECT * FROM properties WHERE p_id = ?";
        List<Property> list = jdbcTemplate.query(sql, propertyRowMapper, pId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<Property> findAll() {
        String sql = "SELECT * FROM properties";
        return jdbcTemplate.query(sql, propertyRowMapper);
    }

    public List<Property> findByUserId(Long userId) {
        String sql = "SELECT * FROM properties WHERE user_id = ?";
        return jdbcTemplate.query(sql, propertyRowMapper, userId);
    }

    public Property findByRoomInfo(String buildingNo, String unitNo, String roomNo) {
        String sql = "SELECT * FROM properties WHERE building_no = ? AND unit_no = ? AND room_no = ?";
        List<Property> list = jdbcTemplate.query(sql, propertyRowMapper, buildingNo, unitNo, roomNo);
        return list.isEmpty() ? null : list.get(0);
    }
}
