package com.framework.controller;

import com.framework.dto.MenuItemDto;
import com.framework.model.User;
import com.framework.service.AuthService;
import com.framework.service.MenuService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
public class MenuController {

    private final MenuService menuService;
    private final AuthService authService;

    public MenuController(MenuService menuService, AuthService authService) {
        this.menuService = menuService;
        this.authService = authService;
    }

    /**
     * GET /menu — Get main menu items for the current user.
     */
    @GetMapping("/menu")
    public ResponseEntity<List<MenuItemDto>> getMainMenu(HttpServletRequest request) {
        Set<String> roles = getUserRoles(request);
        return ResponseEntity.ok(menuService.getMainMenu(roles));
    }

    /**
     * GET /adminMenu — Get admin menu items for the current user.
     */
    @GetMapping("/adminMenu")
    public ResponseEntity<List<MenuItemDto>> getAdminMenu(HttpServletRequest request) {
        Set<String> roles = getUserRoles(request);
        return ResponseEntity.ok(menuService.getAdminMenu(roles));
    }

    private Set<String> getUserRoles(HttpServletRequest request) {
        String email = (String) request.getAttribute("userEmail");
        if (email == null) {
            return Set.of(); // No auth — return empty roles (shows only public menu items)
        }
        try {
            User user = authService.getUserByEmail(email);
            return user != null ? user.getRoleNames() : Set.of();
        } catch (Exception e) {
            return Set.of();
        }
    }
}
