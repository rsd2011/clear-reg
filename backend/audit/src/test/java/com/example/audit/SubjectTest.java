package com.example.audit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubjectTest {

    @Test
    void builderAndToBuilder() {
        Subject subject = Subject.builder().type("FILE").key("123").build();
        assertThat(subject.getType()).isEqualTo("FILE");
        Subject copy = subject.toBuilder().key("456").build();
        assertThat(copy.getKey()).isEqualTo("456");
        assertThat(copy.getType()).isEqualTo("FILE");
    }
}
