package com.framework.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.framework.model.CmsPage;
import com.framework.model.CmsTemplate;
import com.framework.model.SiteSetting;
import com.framework.repository.CmsPageRepository;
import com.framework.repository.CmsTemplateRepository;
import com.framework.repository.SiteSettingRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.TreeMap;
import java.util.*;

@Service
public class CmsService {

    private final SiteSettingRepository siteSettingRepository;
    private final CmsTemplateRepository cmsTemplateRepository;
    private final CmsPageRepository cmsPageRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CmsService(SiteSettingRepository siteSettingRepository,
                      CmsTemplateRepository cmsTemplateRepository,
                      CmsPageRepository cmsPageRepository,
                      JdbcTemplate jdbcTemplate,
                      ObjectMapper objectMapper) {
        this.siteSettingRepository = siteSettingRepository;
        this.cmsTemplateRepository = cmsTemplateRepository;
        this.cmsPageRepository = cmsPageRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    // ─── Site Settings ───────────────────────────────────────────────

    public Map<String, String> getAllSettings() {
        Map<String, String> result = new LinkedHashMap<>();
        siteSettingRepository.findAll().forEach(s ->
                result.put(s.getSettingKey(), s.getSettingValue()));
        return result;
    }

    public void updateSettings(Map<String, String> settings) {
        settings.forEach((key, value) -> {
            Optional<SiteSetting> existing = siteSettingRepository.findBySettingKey(key);
            if (existing.isPresent()) {
                SiteSetting s = existing.get();
                s.setSettingValue(value);
                s.setUpdatedAt(Instant.now());
                siteSettingRepository.save(s);
            } else {
                SiteSetting s = new SiteSetting(key, value);
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
                siteSettingRepository.save(s);
            }
        });
    }

    public String getSetting(String key) {
        return siteSettingRepository.findBySettingKey(key)
                .map(SiteSetting::getSettingValue)
                .orElse(null);
    }

    // ─── Page Rendering ──────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public Map<String, Object> renderPage(String slug, boolean preview) throws JsonProcessingException {
        CmsPage page = cmsPageRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + slug));

        if (!preview && !"published".equals(page.getStatus())) {
            throw new IllegalArgumentException("Page not published: " + slug);
        }

        CmsTemplate template = cmsTemplateRepository.findById(page.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + page.getTemplateId()));

        // Parse template structure
        Map<String, Object> structure = objectMapper.readValue(template.getStructure(),
                new TypeReference<Map<String, Object>>() {});

        // Parse page content
        Map<String, String> content = new LinkedHashMap<>();
        if (page.getContent() != null && !page.getContent().isEmpty()) {
            content = objectMapper.readValue(page.getContent(),
                    new TypeReference<Map<String, String>>() {});
        }

        // Merge content values into structure
        List<Map<String, Object>> sections = (List<Map<String, Object>>) structure.get("sections");
        if (sections != null) {
            for (Map<String, Object> section : sections) {
                String sectionKey = (String) section.get("key");
                List<Map<String, Object>> blocks = (List<Map<String, Object>>) section.get("blocks");
                if (blocks != null) {
                    for (Map<String, Object> block : blocks) {
                        String blockKey = (String) block.get("key");
                        String blockType = (String) block.get("type");

                        if ("repeater".equals(blockType)) {
                            // Collect repeater items: keys like "section.block.0.field"
                            String prefix = sectionKey + "." + blockKey + ".";
                            Map<Integer, Map<String, String>> items = new TreeMap<>();
                            for (Map.Entry<String, String> entry : content.entrySet()) {
                                if (entry.getKey().startsWith(prefix)) {
                                    String rest = entry.getKey().substring(prefix.length());
                                    int dotIdx = rest.indexOf('.');
                                    if (dotIdx > 0) {
                                        try {
                                            int index = Integer.parseInt(rest.substring(0, dotIdx));
                                            String field = rest.substring(dotIdx + 1);
                                            items.computeIfAbsent(index, k -> new LinkedHashMap<>())
                                                    .put(field, entry.getValue());
                                        } catch (NumberFormatException ignored) {}
                                    }
                                }
                            }
                            block.put("value", new ArrayList<>(items.values()));
                        } else {
                            String contentKey = sectionKey + "." + blockKey;
                            block.put("value", content.getOrDefault(contentKey, ""));
                        }
                    }
                }
            }
        }

        // Build response
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", page.getId());
        result.put("title", page.getTitle());
        result.put("slug", page.getSlug());
        result.put("status", page.getStatus());
        result.put("metaTitle", page.getMetaTitle());
        result.put("metaDescription", page.getMetaDescription());
        result.put("bgColor", content.getOrDefault("_page.bg_color", ""));
        result.put("templateName", template.getName());
        result.put("sections", sections);
        return result;
    }

    // ─── Page Publishing ─────────────────────────────────────────────

    public CmsPage publishPage(Long id) {
        CmsPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));
        page.setStatus("published");
        page.setPublishedAt(Instant.now());
        page.setUpdatedAt(Instant.now());
        return cmsPageRepository.save(page);
    }

    public CmsPage unpublishPage(Long id) {
        CmsPage page = cmsPageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + id));
        page.setStatus("draft");
        page.setPublishedAt(null);
        page.setUpdatedAt(Instant.now());
        return cmsPageRepository.save(page);
    }

    // ─── Menu Reorder ────────────────────────────────────────────────

    public void reorderMenu(String menuType, List<Map<String, Object>> orders) {
        String table;
        if ("main".equals(menuType)) {
            table = "main_menu_item";
        } else if ("admin".equals(menuType)) {
            table = "admin_menu_item";
        } else {
            throw new IllegalArgumentException("Invalid menu type: " + menuType);
        }

        for (Map<String, Object> item : orders) {
            Long id = Long.valueOf(item.get("id").toString());
            Integer order = Integer.valueOf(item.get("item_order").toString());
            jdbcTemplate.update("UPDATE " + table + " SET item_order = ? WHERE id = ?", order, id);
        }
    }
}
