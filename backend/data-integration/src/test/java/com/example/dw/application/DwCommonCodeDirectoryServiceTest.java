package com.example.dw.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dw.domain.DwCommonCodeEntity;
import com.example.dw.infrastructure.persistence.DwCommonCodeRepository;

@ExtendWith(MockitoExtension.class)
class DwCommonCodeDirectoryServiceTest {

    @Mock
    private DwCommonCodeRepository repository;

    private DwCommonCodeDirectoryService service;

    @BeforeEach
    void setUp() {
        service = new DwCommonCodeDirectoryService(repository);
    }

    @Test
    void givenCodeType_whenFindActive_thenReturnOrderedSnapshots() {
        DwCommonCodeEntity entity = DwCommonCodeEntity.create(
                "CATEGORY",
                "A",
                "Alpha",
                10,
                true,
                null,
                null,
                null,
                java.util.UUID.randomUUID(),
                java.time.OffsetDateTime.now()
        );
        given(repository.findByCodeTypeAndActiveTrueOrderByDisplayOrderAscCodeValueAsc("CATEGORY"))
                .willReturn(List.of(entity));

        List<DwCommonCodeSnapshot> snapshots = service.findActive("CATEGORY");

        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.getFirst().codeValue()).isEqualTo("A");
        verify(repository).findByCodeTypeAndActiveTrueOrderByDisplayOrderAscCodeValueAsc("CATEGORY");
    }

    @Test
    void whenEvictMethodsInvoked_thenNoException() {
        service.evict("CATEGORY");
        service.evictAll();
    }
}
