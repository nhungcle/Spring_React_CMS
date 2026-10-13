package com.framework.controller;

import com.framework.service.DynamicCrudService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class DynamicCrudController {

    private final DynamicCrudService crudService;

    public DynamicCrudController(DynamicCrudService crudService) {
        this.crudService = crudService;
    }

    /**
     * GET /{table} — List all records from a table.
     */
    @GetMapping("/{table}")
    public ResponseEntity<List<Map<String, Object>>> listAll(@PathVariable String table) {
        return ResponseEntity.ok(crudService.findAll(table));
    }

    /**
     * GET /{table}/{id} — Get a single record by ID.
     */
    @GetMapping("/{table}/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable String table,
            @PathVariable Long id) {
        return ResponseEntity.ok(crudService.findById(table, id));
    }

    /**
     * POST /{table} — Create a new record.
     */
    @PostMapping("/{table}")
    public ResponseEntity<Map<String, Object>> create(
            @PathVariable String table,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(crudService.create(table, data));
    }

    /**
     * PUT /{table}/{id} — Update a record by ID.
     */
    @PutMapping("/{table}/{id}")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable String table,
            @PathVariable Long id,
            @RequestBody Map<String, Object> data) {
        return ResponseEntity.ok(crudService.update(table, id, data));
    }

    /**
     * DELETE /{table}/{id} — Delete a record by ID.
     */
    @DeleteMapping("/{table}/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String table,
            @PathVariable Long id) {
        crudService.delete(table, id);
        return ResponseEntity.ok().build();
    }

    /**
     * GET /{table}/{id}/sec/{ids} — Associate secondary records (many-to-many).
     * Used by EditSQLRow for checkbox relationships.
     */
    @GetMapping("/{table}/{id}/sec/{ids}")
    public ResponseEntity<Void> associateSecondary(
            @PathVariable String table,
            @PathVariable Long id,
            @PathVariable String ids) {
        crudService.associateSecondary(table, id, ids);
        return ResponseEntity.ok().build();
    }
}
