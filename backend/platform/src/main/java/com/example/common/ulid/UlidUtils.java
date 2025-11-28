package com.example.common.ulid;

import java.util.UUID;

import com.github.f4b6a3.ulid.Ulid;

/**
 * ULID ↔ UUID 변환 유틸리티.
 *
 * <p>API 계층에서 ULID 문자열(26자)과 UUID 간 변환을 지원합니다.</p>
 *
 * <h3>지원 형식</h3>
 * <ul>
 *   <li>ULID: {@code "01ARZ3NDEKTSV4RRFFQ69G5FAV"} (26자, Crockford Base32)</li>
 *   <li>UUID: {@code "550e8400-e29b-41d4-a716-446655440000"} (36자, 8-4-4-4-12)</li>
 * </ul>
 *
 * <h3>ULID의 장점</h3>
 * <ul>
 *   <li>URL 길이 28% 감소 (36자 → 26자)</li>
 *   <li>혼동 문자 제외 (I, L, O, U) - 사용자 입력 오류 감소</li>
 *   <li>대소문자 무관 (Case Insensitive)</li>
 *   <li>시간순 정렬 가능 (Lexicographically Sortable)</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>{@code
 * // ULID 문자열 → UUID
 * UUID uuid = UlidUtils.fromString("01ARZ3NDEKTSV4RRFFQ69G5FAV");
 *
 * // UUID → ULID 문자열
 * String ulid = UlidUtils.toUlidString(uuid);
 *
 * // 하위 호환: UUID 문자열도 지원
 * UUID uuid2 = UlidUtils.fromString("550e8400-e29b-41d4-a716-446655440000");
 * }</pre>
 *
 * @see <a href="https://github.com/ulid/spec">ULID Specification</a>
 */
public final class UlidUtils {

    /** ULID 문자열 길이 (Crockford Base32) */
    public static final int ULID_LENGTH = 26;

    /** UUID 문자열 길이 (하이픈 포함) */
    public static final int UUID_LENGTH = 36;

    /** Crockford Base32 정규식 (I, L, O, U 제외) */
    private static final String ULID_PATTERN = "^[0-9A-HJKMNP-TV-Z]{26}$";

    private UlidUtils() {
        // 유틸리티 클래스
    }

    /**
     * ULID(26자) 또는 UUID(36자) 문자열을 UUID로 변환합니다.
     *
     * <p>하위 호환성을 위해 두 형식 모두 지원합니다.</p>
     *
     * @param input ULID 또는 UUID 형식 문자열
     * @return 변환된 UUID, null 또는 빈 문자열이면 null 반환
     * @throws IllegalArgumentException 유효하지 않은 형식인 경우
     */
    public static UUID fromString(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String trimmed = input.trim().toUpperCase();

        if (trimmed.length() == ULID_LENGTH) {
            return parseUlid(trimmed);
        } else if (trimmed.length() == UUID_LENGTH) {
            return parseUuid(input.trim());
        }

        throw new IllegalArgumentException(
                "Invalid ID format. Expected ULID (26 chars) or UUID (36 chars), got " +
                trimmed.length() + " chars: '" + input + "'");
    }

    /**
     * UUID를 ULID 문자열(26자)로 변환합니다.
     *
     * @param uuid UUID
     * @return ULID 문자열, null이면 null 반환
     */
    public static String toUlidString(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return Ulid.from(uuid).toString();
    }

    /**
     * 문자열이 유효한 ULID 형식인지 검증합니다.
     *
     * @param input 검증할 문자열
     * @return ULID 형식이면 true
     */
    public static boolean isValidUlid(String input) {
        if (input == null || input.length() != ULID_LENGTH) {
            return false;
        }
        return input.toUpperCase().matches(ULID_PATTERN);
    }

    /**
     * 문자열이 유효한 UUID 형식인지 검증합니다.
     *
     * @param input 검증할 문자열
     * @return UUID 형식이면 true
     */
    public static boolean isValidUuid(String input) {
        if (input == null || input.length() != UUID_LENGTH) {
            return false;
        }
        try {
            UUID.fromString(input);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 문자열이 유효한 ULID 또는 UUID 형식인지 검증합니다.
     *
     * @param input 검증할 문자열
     * @return ULID 또는 UUID 형식이면 true
     */
    public static boolean isValidId(String input) {
        return isValidUlid(input) || isValidUuid(input);
    }

    private static UUID parseUlid(String ulid) {
        try {
            return Ulid.from(ulid).toUuid();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid ULID format: '" + ulid + "'. " +
                    "Expected 26-character Crockford Base32 string (e.g., 01ARZ3NDEKTSV4RRFFQ69G5FAV).",
                    e);
        }
    }

    private static UUID parseUuid(String uuid) {
        try {
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid UUID format: '" + uuid + "'. " +
                    "Expected format: 8-4-4-4-12 (e.g., 550e8400-e29b-41d4-a716-446655440000).",
                    e);
        }
    }
}
