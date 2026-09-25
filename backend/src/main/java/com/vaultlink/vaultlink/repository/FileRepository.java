package com.vaultlink.vaultlink.repository;

import com.vaultlink.vaultlink.model.FileEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class FileRepository {

    private final JdbcTemplate jdbcTemplate;

    public FileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public FileEntity save(FileEntity file) {
        String sql = "INSERT INTO files (original_name, stored_name, file_path, file_size, content_type, owner_id) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, file.getOriginalName());
            ps.setString(2, file.getStoredName());
            ps.setString(3, file.getFilePath());
            ps.setLong(4, file.getFileSize());
            ps.setString(5, file.getContentType());
            ps.setLong(6, file.getOwnerId());
            return ps;
        }, keyHolder);

        file.setId(keyHolder.getKey().longValue());
        return file;
    }

    public Optional<FileEntity> findById(Long id) {
        String sql = "SELECT * FROM files WHERE id = ?";
        List<FileEntity> results = jdbcTemplate.query(sql, this::mapRow, id);
        return results.stream().findFirst();
    }

    public List<FileEntity> findByOwnerId(Long ownerId) {
        String sql = "SELECT * FROM files WHERE owner_id = ?";
        return jdbcTemplate.query(sql, this::mapRow, ownerId);
    }

    public void deleteById(Long id) {
        jdbcTemplate.update("DELETE FROM files WHERE id = ?", id);
    }

    private FileEntity mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        FileEntity file = new FileEntity();
        file.setId(rs.getLong("id"));
        file.setOriginalName(rs.getString("original_name"));
        file.setStoredName(rs.getString("stored_name"));
        file.setFilePath(rs.getString("file_path"));
        file.setFileSize(rs.getLong("file_size"));
        file.setContentType(rs.getString("content_type"));
        file.setOwnerId(rs.getLong("owner_id"));
        file.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return file;
    }
}