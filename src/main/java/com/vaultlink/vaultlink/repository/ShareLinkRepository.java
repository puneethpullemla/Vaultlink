package com.vaultlink.vaultlink.repository;

import com.vaultlink.vaultlink.model.ShareLink;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class ShareLinkRepository {

    private final JdbcTemplate jdbcTemplate;

    public ShareLinkRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ShareLink save(ShareLink link) {
        String sql = "INSERT INTO share_links (file_id, token, expires_at, is_revoked) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, link.getFileId());
            ps.setString(2, link.getToken());
            ps.setTimestamp(3, Timestamp.valueOf(link.getExpiresAt()));
            ps.setBoolean(4, link.isRevoked());
            return ps;
        }, keyHolder);

        link.setId(keyHolder.getKey().longValue());
        return link;
    }

    public Optional<ShareLink> findByToken(String token) {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        List<ShareLink> results = jdbcTemplate.query(sql, this::mapRow, token);
        return results.stream().findFirst();
    }

    public void revoke(String token) {
        jdbcTemplate.update("UPDATE share_links SET is_revoked = TRUE WHERE token = ?", token);
    }

    private ShareLink mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        ShareLink link = new ShareLink();
        link.setId(rs.getLong("id"));
        link.setFileId(rs.getLong("file_id"));
        link.setToken(rs.getString("token"));
        link.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        link.setRevoked(rs.getBoolean("is_revoked"));
        link.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return link;
    }
}