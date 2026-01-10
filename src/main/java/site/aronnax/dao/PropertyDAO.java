package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.Property;

/**
 * 房产资源数据访问对象 (DAO)
 * 实现房产档案的基础管理（录入、修改、关联查询等）。
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class PropertyDAO {

    private final JdbcTemplate jdbcTemplate;

    public PropertyDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 房产表行映射器
     * 处理 user_id 字段时通过 wasNull() 特别判断了空值情况，因为房产可能尚未售出（无关联业主）。
     */
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
            property.setUserId(null); // 明确设置为 null，表示待售或空置
        }
        return property;
    };

    /**
     * 插入房产记录
     * 
     * @return 此处简略返回 null，实际项目中可能需要获取生成 ID
     */
    public Long insert(Property property) {
        String sql = "INSERT INTO properties (building_no, unit_no, room_no, area, p_status, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql, property.getBuildingNo(), property.getUnitNo(), property.getRoomNo(),
                property.getArea(), property.getpStatus(), property.getUserId());
        return null;
    }

    /**
     * 根据ID物理删除房产记录
     */
    public boolean deleteById(Long pId) {
        String sql = "DELETE FROM properties WHERE p_id = ?";
        return jdbcTemplate.update(sql, pId) > 0;
    }

    /**
     * 更新房产档案信息（如面积调整、状态变动或业主变更）
     */
    public boolean update(Property property) {
        String sql = "UPDATE properties SET building_no=?, unit_no=?, room_no=?, area=?, p_status=?, user_id=? WHERE p_id=?";
        return jdbcTemplate.update(sql, property.getBuildingNo(), property.getUnitNo(), property.getRoomNo(),
                property.getArea(), property.getpStatus(), property.getUserId(), property.getpId()) > 0;
    }

    /**
     * 按照主键ID精确查询房产
     */
    @SuppressWarnings("null")
    public Property findById(Long pId) {
        String sql = "SELECT * FROM properties WHERE p_id = ?";
        List<Property> list = jdbcTemplate.query(sql, propertyRowMapper, pId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取小区所有房产列表
     */
    @SuppressWarnings("null")
    public List<Property> findAll() {
        String sql = "SELECT * FROM properties";
        return jdbcTemplate.query(sql, propertyRowMapper);
    }

    /**
     * 根据业主(User)ID查询其名下的所有房产
     * 
     * @param userId 关联的用户ID
     */
    @SuppressWarnings("null")
    public List<Property> findByUserId(Long userId) {
        String sql = "SELECT * FROM properties WHERE user_id = ?";
        return jdbcTemplate.query(sql, propertyRowMapper, userId);
    }

    /**
     * 根据楼、单元、房号组合查询房产（常用于业主绑定或唯一性校验）
     */
    @SuppressWarnings("null")
    public Property findByRoomInfo(String buildingNo, String unitNo, String roomNo) {
        String sql = "SELECT * FROM properties WHERE building_no = ? AND unit_no = ? AND room_no = ?";
        List<Property> list = jdbcTemplate.query(sql, propertyRowMapper, buildingNo, unitNo, roomNo);
        return list.isEmpty() ? null : list.get(0);
    }
}
