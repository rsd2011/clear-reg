package com.example.server.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class NoticeNumberGeneratorTest {

    @Test
    @DisplayName("시퀀스가 없으면 새로 생성하고 번호를 만든다")
    void generatesFromNewSequence() {
        NoticeSequenceRepository repository = Mockito.mock(NoticeSequenceRepository.class);
        when(repository.findBySequenceYear(2025)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NoticeNumberGenerator generator = new NoticeNumberGenerator(repository);

        String number = generator.nextDisplayNumber(OffsetDateTime.parse("2025-01-01T00:00Z"));

        assertThat(number).isEqualTo("2025-0001");
    }
}

