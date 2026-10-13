package com.framework.service;

import com.framework.dto.MenuItemDto;
import com.framework.model.AdminMenuItem;
import com.framework.model.MainMenuItem;
import com.framework.model.MenuItem;
import com.framework.repository.AdminMenuRepository;
import com.framework.repository.MainMenuRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private final MainMenuRepository mainMenuRepository;
    private final AdminMenuRepository adminMenuRepository;

    public MenuService(MainMenuRepository mainMenuRepository, AdminMenuRepository adminMenuRepository) {
        this.mainMenuRepository = mainMenuRepository;
        this.adminMenuRepository = adminMenuRepository;
    }

    /**
     * Get main menu items filtered by user roles and built as a tree.
     */
    public List<MenuItemDto> getMainMenu(Set<String> userRoles) {
        List<MainMenuItem> items = mainMenuRepository.findAllByOrderByOrderAsc();
        return buildMenuTree(items, userRoles);
    }

    /**
     * Get admin menu items filtered by user roles and built as a tree.
     */
    public List<MenuItemDto> getAdminMenu(Set<String> userRoles) {
        List<AdminMenuItem> items = adminMenuRepository.findAllByOrderByOrderAsc();
        return buildMenuTree(items, userRoles);
    }

    /**
     * Build a hierarchical menu tree from flat menu items, filtered by roles.
     */
    private <T extends MenuItem> List<MenuItemDto> buildMenuTree(List<T> items, Set<String> userRoles) {
        // Filter by roles
        List<T> accessible = items.stream()
                .filter(item -> hasAccess(item, userRoles))
                .collect(Collectors.toList());

        // Build parent-child map
        Map<Long, MenuItemDto> dtoMap = new LinkedHashMap<>();
        List<MenuItemDto> roots = new ArrayList<>();

        for (T item : accessible) {
            MenuItemDto dto = toDto(item);
            dtoMap.put(item.getId(), dto);
        }

        for (T item : accessible) {
            MenuItemDto dto = dtoMap.get(item.getId());
            if (item.getParentId() == null) {
                roots.add(dto);
            } else {
                MenuItemDto parent = dtoMap.get(item.getParentId());
                if (parent != null) {
                    parent.getItems().add(dto);
                } else {
                    // Parent not accessible or doesn't exist, add as root
                    roots.add(dto);
                }
            }
        }

        return roots;
    }

    /**
     * Check if a menu item is accessible to the given roles.
     * If roles field is null or empty, the item is accessible to everyone.
     */
    private boolean hasAccess(MenuItem item, Set<String> userRoles) {
        String rolesStr = item.getRoles();
        if (rolesStr == null || rolesStr.isBlank()) {
            return true;
        }
        String[] requiredRoles = rolesStr.split(",");
        for (String required : requiredRoles) {
            if (userRoles.contains(required.trim())) {
                return true;
            }
        }
        return false;
    }

    private MenuItemDto toDto(MenuItem item) {
        List<String> roles = null;
        if (item.getRoles() != null && !item.getRoles().isBlank()) {
            roles = Arrays.stream(item.getRoles().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        return new MenuItemDto(item.getName(), item.getLink(), item.getIcon(), roles);
    }
}
