package com.example.dw.application.export;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExportFailureEventTest {

    @Test
    @DisplayName("동일 필드는 equals/hashCode 가 동일하다")
    void equalsSameFields() {
        ExportFailureEvent a = new ExportFailureEvent("csv", "a.csv", 10, "ERR1");
        ExportFailureEvent b = new ExportFailureEvent("csv", "a.csv", 10, "ERR1");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
        assertThat(a.toString()).contains("csv").contains("a.csv");
    }

    @Test
    @DisplayName("다른 필드가 있으면 equals 가 false")
    void equalsDifferentFields() {
        ExportFailureEvent base = new ExportFailureEvent("csv", "a.csv", 10, "ERR1");

        assertThat(base).isNotEqualTo(new ExportFailureEvent("csv", "b.csv", 10, "ERR1"));
        assertThat(base).isNotEqualTo(new ExportFailureEvent("pdf", "a.csv", 10, "ERR1"));
        assertThat(base).isNotEqualTo(new ExportFailureEvent("csv", "a.csv", 11, "ERR1"));
        assertThat(base).isNotEqualTo(new ExportFailureEvent("csv", "a.csv", 10, "DIFF"));
        assertThat(base).isNotEqualTo(null);
        assertThat(base).isNotEqualTo("string");
    }

    @Test
    @DisplayName("null 필드를 가진 이벤트끼리 비교")
    void equalsWithNullFields() {
        // 모든 필드가 null인 경우
        ExportFailureEvent allNull1 = new ExportFailureEvent(null, null, 0, null);
        ExportFailureEvent allNull2 = new ExportFailureEvent(null, null, 0, null);
        assertThat(allNull1).isEqualTo(allNull2);
        assertThat(allNull1.hashCode()).isEqualTo(allNull2.hashCode());

        // exportType만 null vs non-null
        ExportFailureEvent nullType = new ExportFailureEvent(null, "a.csv", 10, "OK");
        ExportFailureEvent nonNullType = new ExportFailureEvent("csv", "a.csv", 10, "OK");
        assertThat(nullType).isNotEqualTo(nonNullType);
        assertThat(nonNullType).isNotEqualTo(nullType);

        // fileName만 null vs non-null
        ExportFailureEvent nullName = new ExportFailureEvent("csv", null, 10, "OK");
        ExportFailureEvent nonNullName = new ExportFailureEvent("csv", "a.csv", 10, "OK");
        assertThat(nullName).isNotEqualTo(nonNullName);
        assertThat(nonNullName).isNotEqualTo(nullName);

        // resultCode만 null vs non-null
        ExportFailureEvent nullCode = new ExportFailureEvent("csv", "a.csv", 10, null);
        ExportFailureEvent nonNullCode = new ExportFailureEvent("csv", "a.csv", 10, "OK");
        assertThat(nullCode).isNotEqualTo(nonNullCode);
        assertThat(nonNullCode).isNotEqualTo(nullCode);
    }

    @Test
    @DisplayName("같은 객체를 비교하면 true")
    void equalsSameObject() {
        ExportFailureEvent event = new ExportFailureEvent("csv", "a.csv", 10, "ERR1");
        assertThat(event).isEqualTo(event);
    }

    @Test
    @DisplayName("getter 메서드 테스트")
    void getterMethods() {
        ExportFailureEvent event = new ExportFailureEvent("CSV", "test.csv", 100L, "SUCCESS");

        assertThat(event.getExportType()).isEqualTo("CSV");
        assertThat(event.getFileName()).isEqualTo("test.csv");
        assertThat(event.getRecordCount()).isEqualTo(100L);
        assertThat(event.getResultCode()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("canEqual 분기 테스트 - 하위 클래스 시뮬레이션")
    void canEqualBranch() {
        ExportFailureEvent event = new ExportFailureEvent("csv", "a.csv", 10, "ERR1");
        // canEqual은 Lombok이 생성한 메서드로, 다른 클래스와 비교 시 false 반환
        Object notAnEvent = new Object() {
            @Override
            public boolean equals(Object obj) {
                return false;
            }
        };
        assertThat(event).isNotEqualTo(notAnEvent);
    }
}
