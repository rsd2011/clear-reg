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
}
