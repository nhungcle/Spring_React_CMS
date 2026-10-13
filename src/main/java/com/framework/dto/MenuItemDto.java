package com.framework.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for menu items. The frontend expects items to always be an initialized list.
 */
public class MenuItemDto {

    private String name;
    private String link;
    private String icon;
    private List<String> roles;
    private List<MenuItemDto> items = new ArrayList<>();

    public MenuItemDto() {}

    public MenuItemDto(String name, String link, String icon, List<String> roles) {
        this.name = name;
        this.link = link;
        this.icon = icon;
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public List<MenuItemDto> getItems() {
        return items;
    }

    public void setItems(List<MenuItemDto> items) {
        this.items = items;
    }
}
