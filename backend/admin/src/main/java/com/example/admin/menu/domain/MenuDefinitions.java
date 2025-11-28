package com.example.admin.menu.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 메뉴 정의 파일의 루트 클래스.
 */
public class MenuDefinitions {

    private List<MenuDefinition> menus = new ArrayList<>();

    public List<MenuDefinition> getMenus() {
        return menus;
    }

    public void setMenus(List<MenuDefinition> menus) {
        this.menus = menus != null ? menus : new ArrayList<>();
    }
}
