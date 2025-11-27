package com.example.admin.codemanage;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.application.DwCommonCodeDirectoryService;

@ExtendWith(MockitoExtension.class)
class CodeManageQueryServiceBranchTest {

    @Mock DwCommonCodeDirectoryService dwService;
    @Mock SystemCommonCodeService systemService;

    CodeManageQueryService service;

    @BeforeEach
    void setUp() {
        service = new CodeManageQueryService(dwService, systemService);
    }

    @Test
    @DisplayName("codeType이 null이면 IllegalArgumentException")
    void normalizeNullThrows() {
        assertThatThrownBy(() -> service.aggregate(null, true, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

}
