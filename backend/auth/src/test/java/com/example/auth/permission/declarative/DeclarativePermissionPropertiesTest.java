package com.example.auth.permission.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeclarativePermissionPropertiesTest {

    @Test
    @DisplayName("기본값이 활성 상태이며 위치와 failOnMissingFile 기본값을 가진다")
    void defaultsAreSet() {
        DeclarativePermissionProperties props = new DeclarativePermissionProperties();

        assertThat(props.isEnabled()).isTrue();
        assertThat(props.getLocation()).contains("permission-groups.yml");
        assertThat(props.isFailOnMissingFile()).isFalse();
    }

    @Test
    @DisplayName("프로퍼티 세터로 값을 변경할 수 있다")
    void settersChangeValues() {
        DeclarativePermissionProperties props = new DeclarativePermissionProperties();
        props.setEnabled(false);
        props.setLocation("file:/tmp/perm.yml");
        props.setFailOnMissingFile(true);

        assertThat(props.isEnabled()).isFalse();
        assertThat(props.getLocation()).isEqualTo("file:/tmp/perm.yml");
        assertThat(props.isFailOnMissingFile()).isTrue();
    }
}

