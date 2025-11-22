package com.example.common.masking;

/**
 * 정책 기반 마스킹을 적용할 수 있는 값 객체용 인터페이스.
 * raw()는 원문, masked()는 기본 마스킹 표현을 반환한다.
 */
public interface Maskable {
    String raw();
    String masked();
}
