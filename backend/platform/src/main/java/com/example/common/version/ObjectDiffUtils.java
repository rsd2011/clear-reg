package com.example.common.version;

import java.util.List;
import java.util.Map;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;

/**
 * 객체 비교 유틸리티.
 * <p>
 * Javers 라이브러리를 래핑하여 버전 비교 기능을 제공합니다.
 * SCD Type 2 이력관리에서 버전 간 차이점을 분석할 때 사용합니다.
 * </p>
 *
 * <pre>{@code
 * // 사용 예시
 * Map<String, String> labels = Map.of("name", "이름", "active", "활성화");
 * List<FieldDiff> diffs = ObjectDiffUtils.compareFields(oldObj, newObj, labels);
 * }</pre>
 */
public final class ObjectDiffUtils {

    private static final Javers JAVERS = JaversBuilder.javers().build();

    private ObjectDiffUtils() {
        // 유틸리티 클래스
    }

    /**
     * 두 객체의 필드를 비교하여 변경된 필드 목록을 반환합니다.
     *
     * @param oldObject   이전 객체
     * @param newObject   새 객체
     * @param fieldLabels 필드명 → 라벨 매핑 (포함된 필드만 비교 결과에 포함)
     * @return 변경된 필드 목록
     */
    public static List<FieldDiff> compareFields(Object oldObject, Object newObject,
                                                 Map<String, String> fieldLabels) {
        Diff diff = JAVERS.compare(oldObject, newObject);

        return diff.getChangesByType(ValueChange.class).stream()
                .filter(change -> fieldLabels.containsKey(change.getPropertyName()))
                .map(change -> new FieldDiff(
                        change.getPropertyName(),
                        fieldLabels.get(change.getPropertyName()),
                        change.getLeft(),
                        change.getRight(),
                        DiffType.MODIFIED
                ))
                .toList();
    }

    /**
     * 두 객체를 비교하여 Javers Diff 객체를 반환합니다.
     * <p>
     * 더 세밀한 제어가 필요한 경우 이 메서드를 사용하세요.
     * </p>
     *
     * @param oldObject 이전 객체
     * @param newObject 새 객체
     * @return Javers Diff 객체
     */
    public static Diff compare(Object oldObject, Object newObject) {
        return JAVERS.compare(oldObject, newObject);
    }

    /**
     * 두 컬렉션을 비교하여 Javers Diff 객체를 반환합니다.
     *
     * @param oldCollection 이전 컬렉션
     * @param newCollection 새 컬렉션
     * @param itemClass     컬렉션 요소 타입
     * @param <T>           요소 타입
     * @return Javers Diff 객체
     */
    public static <T> Diff compareCollections(List<T> oldCollection, List<T> newCollection,
                                               Class<T> itemClass) {
        return JAVERS.compareCollections(oldCollection, newCollection, itemClass);
    }
}
