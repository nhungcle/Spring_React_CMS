package com.framework.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.framework.model.CmsFile;
import com.framework.model.CmsPage;
import com.framework.service.CmsService;
import com.framework.service.FileStorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/cms")
public class CmsController {

    private final CmsService cmsService;
    private final FileStorageService fileStorageService;

    public CmsController(CmsService cmsService, FileStorageService fileStorageService) {
        this.cmsService = cmsService;
        this.fileStorageService = fileStorageService;
    }

    // ─── Site Settings ───────────────────────────────────────────────

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> getAllSettings() {
        return ResponseEntity.ok(cmsService.getAllSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> updateSettings(@RequestBody Map<String, String> settings) {
        cmsService.updateSettings(settings);
        return ResponseEntity.ok(cmsService.getAllSettings());
    }

    @GetMapping("/settings/{key}")
    public ResponseEntity<Map<String, String>> getSetting(@PathVariable String key) {
        String value = cmsService.getSetting(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("key", key, "value", value));
    }

    // ─── Page Rendering ──────────────────────────────────────────────

    @GetMapping("/pages/{slug}/render")
    public ResponseEntity<Map<String, Object>> renderPage(@PathVariable String slug) throws JsonProcessingException {
        return ResponseEntity.ok(cmsService.renderPage(slug, false));
    }

    @GetMapping("/pages/{slug}/preview")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> previewPage(@PathVariable String slug) throws JsonProcessingException {
        return ResponseEntity.ok(cmsService.renderPage(slug, true));
    }

    // ─── Page Publishing ─────────────────────────────────────────────

    @PutMapping("/pages/{id}/publish")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<CmsPage> publishPage(@PathVariable Long id) {
        return ResponseEntity.ok(cmsService.publishPage(id));
    }

    @PutMapping("/pages/{id}/unpublish")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<CmsPage> unpublishPage(@PathVariable Long id) {
        return ResponseEntity.ok(cmsService.unpublishPage(id));
    }

    // ─── Menu Reorder ────────────────────────────────────────────────

    @PutMapping("/menu/{menuType}/reorder")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> reorderMenu(
            @PathVariable String menuType,
            @RequestBody List<Map<String, Object>> orders) {
        cmsService.reorderMenu(menuType, orders);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    // ─── File Upload ─────────────────────────────────────────────────

    @PostMapping("/upload")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<CmsFile> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        CmsFile cmsFile = fileStorageService.store(file);
        return ResponseEntity.ok(cmsFile);
    }

    @DeleteMapping("/upload/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable Long id) throws IOException {
        fileStorageService.delete(id);
        return ResponseEntity.ok(Map.of("status", "deleted"));
    }
}
