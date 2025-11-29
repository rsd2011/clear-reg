package com.example.common.masking;

/**
 * 마스킹 가능한 값 객체를 나타내는 인터페이스.
 *
 * <p>민감 데이터를 담는 값 객체는 이 인터페이스를 구현하여
 * 원본 값과 마스킹된 값에 대한 접근을 표준화한다.
 *
 * @param <T> 원본 값의 타입
 */
public interface Maskable<T> {

    /**
     * 원본 값을 반환한다.
     * <p>
     * 이 메서드는 권한이 확인된 경우에만 호출해야 한다.
     *
     * @return 원본 값
     */
    T raw();

    /**
     * 마스킹된 값을 반환한다.
     *
     * @return 마스킹된 값 (문자열)
     */
    String masked();

    /**
     * 이 값 객체의 데이터 종류를 반환한다.
     * <p>
     * 기본 구현은 {@link DataKind#DEFAULT}를 반환한다.
     *
     * @return 데이터 종류
     */
    default DataKind dataKind() {
        return DataKind.DEFAULT;
    }
}
