package com.example.server.commoncode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.application.DwCommonCodeDirectoryService;
import com.example.dw.application.DwCommonCodeSnapshot;
import com.example.server.commoncode.model.CommonCodeKind;
import com.example.server.commoncode.model.CommonCodeSource;
import com.example.server.commoncode.model.SystemCommonCode;

@ExtendWith(MockitoExtension.class)
class CommonCodeQueryServiceBranchTest {

    @Mock DwCommonCodeDirectoryService dwService;
    @Mock SystemCommonCodeService systemService;

    CommonCodeQueryService service;

    @BeforeEach
    void setUp() {
        service = new CommonCodeQueryService(dwService, systemService);
    }

    @Test
    @DisplayName("codeType이 null이면 IllegalArgumentException")
    void normalizeNullThrows() {
        assertThatThrownBy(() -> service.aggregate(null, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
