package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.UtilityCard;

/**
 * 水电卡数据访问对象 (DAO)
 * 实现对业主水电卡的增删改查。水电卡作为一种虚拟资产，与房产 (Property) 绑定。
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class UtilityCardDAO {

    private final JdbcTemplate jdbcTemplate;

    public UtilityCardDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 水电卡表行映射器
     */
    @SuppressWarnings("null")
    private final RowMapper<UtilityCard> cardRowMapper = (rs, rowNum) -> {
        UtilityCard card = new UtilityCard();
        card.setCardId(rs.getLong("card_id"));
        card.setpId(rs.getLong("p_id"));
        card.setCardType(rs.getString("card_type"));
        card.setBalance(rs.getDouble("balance"));
        if (rs.getTimestamp("last_topup") != null) {
            card.setLastTopup(rs.getTimestamp("last_topup").toLocalDateTime());
        }
        return card;
    };

    /**
     * 插入新卡记录 (通常在后台为房产配卡时调用)
     */
    public Long insert(UtilityCard card) {
        String sql = "INSERT INTO utility_cards (p_id, card_type, balance, last_topup) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, card.getpId(), card.getCardType(), card.getBalance(), card.getLastTopup());
        return null;
    }

    /**
     * 注销或删除卡片
     */
    public boolean deleteById(Long cardId) {
        String sql = "DELETE FROM utility_cards WHERE card_id = ?";
        return jdbcTemplate.update(sql, cardId) > 0;
    }

    /**
     * 更新卡片信息 (核心业务：充值后更新余额与充值时间)
     */
    public boolean update(UtilityCard card) {
        String sql = "UPDATE utility_cards SET p_id=?, card_type=?, balance=?, last_topup=? WHERE card_id=?";
        return jdbcTemplate.update(sql, card.getpId(), card.getCardType(), card.getBalance(), card.getLastTopup(),
                card.getCardId()) > 0;
    }

    /**
     * 根据卡号(主键)查询
     */
    @SuppressWarnings("null")
    public UtilityCard findById(Long cardId) {
        String sql = "SELECT * FROM utility_cards WHERE card_id = ?";
        List<UtilityCard> list = jdbcTemplate.query(sql, cardRowMapper, cardId);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 获取系统内所有水电卡记录
     */
    @SuppressWarnings("null")
    public List<UtilityCard> findAll() {
        String sql = "SELECT * FROM utility_cards";
        return jdbcTemplate.query(sql, cardRowMapper);
    }

    /**
     * 查询指定房产关联的所有水电卡
     * 
     * @param pId 房产ID
     */
    @SuppressWarnings("null")
    public List<UtilityCard> findByPropertyId(Long pId) {
        String sql = "SELECT * FROM utility_cards WHERE p_id = ?";
        return jdbcTemplate.query(sql, cardRowMapper, pId);
    }
}
