package com.example.admin.menu.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * YAML 메뉴 정의 파일에서 로드되는 메뉴 정의 클래스.
 */
public class MenuDefinition {

    private String code;
    private String name;
    private String path;
    private String icon;
    private Integer sortOrder;
    private String description;
    private List<CapabilityRef> requiredCapabilities = new ArrayList<>();
    private List<MenuDefinition> children = new ArrayList<>();

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CapabilityRef> getRequiredCapabilities() {
        return requiredCapabilities;
    }

    public void setRequiredCapabilities(List<CapabilityRef> requiredCapabilities) {
        this.requiredCapabilities = requiredCapabilities != null ? requiredCapabilities : new ArrayList<>();
    }

    public List<MenuDefinition> getChildren() {
        return children;
    }

    public void setChildren(List<MenuDefinition> children) {
        this.children = children != null ? children : new ArrayList<>();
    }

    /**
     * 메뉴에 필요한 Capability 참조.
     */
    public static class CapabilityRef {
        private String feature;
        private String action;

        public String getFeature() {
            return feature;
        }

        public void setFeature(String feature) {
            this.feature = feature;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
