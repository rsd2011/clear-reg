package com.example.dw.application.readmodel;

/**
 * 메뉴 계층의 단일 항목을 표현한다.
 */
public record MenuItem(
        String code,
        String name,
        String featureCode,
        String actionCode,
        String path
) {
}
