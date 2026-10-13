package com.framework.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DynamicCrudService {

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    private final Set<String> allowedTables;
    private final Set<String> reservedPaths = Set.of(
            "auth", "menu", "adminMenu", "adminmenu", "ws", "h2-console", "cms", "uploads"
    );

    // Cache table column names to avoid repeated metadata lookups
    private final Map<String, Set<String>> tableColumnCache = new ConcurrentHashMap<>();

    public DynamicCrudService(
            JdbcTemplate jdbcTemplate,
            DataSource dataSource,
            @Value("${app.crud.allowed-tables:}") List<String> allowedTablesList) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
        this.allowedTables = new HashSet<>(allowedTablesList);
    }

    /**
     * Validate that the table name is allowed and not a reserved path.
     */
    public void validateTable(String table) {
        if (reservedPaths.contains(table.toLowerCase())) {
            throw new IllegalArgumentException("Invalid table name: " + table);
        }
        if (!allowedTables.isEmpty() && !allowedTables.contains(table)) {
            throw new IllegalArgumentException("Table not allowed: " + table);
        }
        // Only allow alphanumeric, underscore, and hyphen
        if (!table.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid table name format: " + table);
        }
    }

    /**
     * List all records from a table.
     */
    public List<Map<String, Object>> findAll(String table) {
        validateTable(table);
        return jdbcTemplate.queryForList("SELECT * FROM " + sanitize(table));
    }

    /**
     * Find a single record by ID.
     */
    public Map<String, Object> findById(String table, Long id) {
        validateTable(table);
        return jdbcTemplate.queryForMap("SELECT * FROM " + sanitize(table) + " WHERE id = ?", id);
    }

    /**
     * Create a new record. Auto-populates audit fields if they exist.
     */
    public Map<String, Object> create(String table, Map<String, Object> data) {
        validateTable(table);
        String sanitized = sanitize(table);

        // Inject audit fields
        Set<String> columns = getTableColumns(sanitized);
        String currentUser = getCurrentUser();
        Timestamp now = Timestamp.from(Instant.now());

        Map<String, Object> insertData = new HashMap<>(data);
        if (columns.contains("created_at")) {
            insertData.put("created_at", now);
        }
        if (columns.contains("updated_at")) {
            insertData.put("updated_at", now);
        }
        if (columns.contains("created_by") && currentUser != null) {
            insertData.put("created_by", currentUser);
        }
        if (columns.contains("updated_by") && currentUser != null) {
            insertData.put("updated_by", currentUser);
        }

        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName(sanitized)
                .usingGeneratedKeyColumns("id");

        Number newId = insert.executeAndReturnKey(insertData);

        return findById(table, newId.longValue());
    }

    /**
     * Update a record by ID. Auto-populates updated_at and updated_by.
     */
    public Map<String, Object> update(String table, Long id, Map<String, Object> data) {
        validateTable(table);
        String sanitized = sanitize(table);

        // Inject audit fields
        Set<String> columns = getTableColumns(sanitized);
        String currentUser = getCurrentUser();
        Timestamp now = Timestamp.from(Instant.now());

        Map<String, Object> updateData = new HashMap<>(data);
        // Remove id from update data if present
        updateData.remove("id");

        if (columns.contains("updated_at")) {
            updateData.put("updated_at", now);
        }
        if (columns.contains("updated_by") && currentUser != null) {
            updateData.put("updated_by", currentUser);
        }

        if (updateData.isEmpty()) {
            return findById(table, id);
        }

        StringBuilder sql = new StringBuilder("UPDATE " + sanitized + " SET ");
        List<Object> params = new ArrayList<>();

        Iterator<Map.Entry<String, Object>> it = updateData.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            sql.append(sanitize(entry.getKey())).append(" = ?");
            params.add(entry.getValue());
            if (it.hasNext()) {
                sql.append(", ");
            }
        }
        sql.append(" WHERE id = ?");
        params.add(id);

        jdbcTemplate.update(sql.toString(), params.toArray());

        return findById(table, id);
    }

    /**
     * Delete a record by ID.
     */
    public void delete(String table, Long id) {
        validateTable(table);
        jdbcTemplate.update("DELETE FROM " + sanitize(table) + " WHERE id = ?", id);
    }

    /**
     * Associate secondary records via a junction table.
     * Used by EditSQLRow for checkbox/many-to-many relationships.
     * Convention: junction table is named {table}_{secondaryTable} with columns {table}_id and secondary_id.
     */
    public void associateSecondary(String table, Long primaryId, String csvIds) {
        validateTable(table);
        // Parse the IDs
        String[] idParts = csvIds.split(",");
        List<Long> secondaryIds = new ArrayList<>();
        for (String part : idParts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                secondaryIds.add(Long.parseLong(trimmed));
            }
        }

        // The junction table naming convention: {table}_sec
        String junctionTable = sanitize(table) + "_sec";
        String primaryCol = sanitize(table) + "_id";

        // Clear existing associations
        jdbcTemplate.update("DELETE FROM " + junctionTable + " WHERE " + primaryCol + " = ?", primaryId);

        // Insert new associations
        for (Long secId : secondaryIds) {
            jdbcTemplate.update(
                    "INSERT INTO " + junctionTable + " (" + primaryCol + ", secondary_id) VALUES (?, ?)",
                    primaryId, secId
            );
        }
    }

    /**
     * Get column names for a table from database metadata. Results are cached.
     */
    private Set<String> getTableColumns(String table) {
        return tableColumnCache.computeIfAbsent(table, t -> {
            Set<String> cols = new HashSet<>();
            try (Connection conn = dataSource.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();
                // Try both lower and upper case table names (H2 uses upper)
                ResultSet rs = meta.getColumns(null, null, t.toUpperCase(), null);
                while (rs.next()) {
                    cols.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
                if (cols.isEmpty()) {
                    rs = meta.getColumns(null, null, t, null);
                    while (rs.next()) {
                        cols.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            } catch (Exception e) {
                // Return empty set if metadata lookup fails
            }
            return cols;
        });
    }

    /**
     * Get the current authenticated user's email.
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() != null) {
            return auth.getName();
        }
        return null;
    }

    /**
     * Basic sanitization — only allow alphanumeric, underscore, hyphen.
     */
    private String sanitize(String name) {
        if (!name.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid name: " + name);
        }
        return name;
    }
}
