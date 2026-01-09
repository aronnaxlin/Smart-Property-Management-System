package site.aronnax.dao;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import site.aronnax.entity.UtilityCard;

/**
 * Utility Card Data Access Object
 * Implemented with Spring JdbcTemplate
 *
 * @author Aronnax (Li Linhan)
 */
@Repository
public class UtilityCardDAO {

    private final JdbcTemplate jdbcTemplate;

    public UtilityCardDAO(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    public Long insert(UtilityCard card) {
        String sql = "INSERT INTO utility_cards (p_id, card_type, balance, last_topup) VALUES (?, ?, ?, ?)";
        jdbcTemplate.update(sql, card.getpId(), card.getCardType(), card.getBalance(), card.getLastTopup());
        return null; // ID retrieval omitted
    }

    public boolean deleteById(Long cardId) {
        String sql = "DELETE FROM utility_cards WHERE card_id = ?";
        return jdbcTemplate.update(sql, cardId) > 0;
    }

    public boolean update(UtilityCard card) {
        String sql = "UPDATE utility_cards SET p_id=?, card_type=?, balance=?, last_topup=? WHERE card_id=?";
        return jdbcTemplate.update(sql, card.getpId(), card.getCardType(), card.getBalance(), card.getLastTopup(),
                card.getCardId()) > 0;
    }

    public UtilityCard findById(Long cardId) {
        String sql = "SELECT * FROM utility_cards WHERE card_id = ?";
        List<UtilityCard> list = jdbcTemplate.query(sql, cardRowMapper, cardId);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<UtilityCard> findAll() {
        String sql = "SELECT * FROM utility_cards";
        return jdbcTemplate.query(sql, cardRowMapper);
    }

    public List<UtilityCard> findByPropertyId(Long pId) {
        String sql = "SELECT * FROM utility_cards WHERE p_id = ?";
        return jdbcTemplate.query(sql, cardRowMapper, pId);
    }
}
