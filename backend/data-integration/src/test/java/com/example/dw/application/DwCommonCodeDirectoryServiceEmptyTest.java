package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;

class DwCommonCodeDirectoryServiceEmptyTest {

    DwCommonCodeRepository repository = Mockito.mock(DwCommonCodeRepository.class);
    DwCommonCodeDirectoryService service = new DwCommonCodeDirectoryService(repository);

    @Test
    @DisplayName("코드 타입이 없으면 빈 리스트를 반환하고 캐시에 저장되지 않는다(unless=isEmpty)")
    void emptyResultNotCached() {
        when(repository.findByCodeTypeAndActiveTrueOrderByDisplayOrderAscCodeValueAsc("NOPE"))
                .thenReturn(Collections.emptyList());

        var records = service.findActive("NOPE");

        assertThat(records).isEmpty();
    }
}
