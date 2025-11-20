package com.example.server.notice;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NoticeNumberGenerator {

    private final NoticeSequenceRepository sequenceRepository;

    public NoticeNumberGenerator(NoticeSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String nextDisplayNumber(OffsetDateTime now) {
        int year = now.getYear();
        NoticeSequence sequence = sequenceRepository.findBySequenceYear(year)
                .orElseGet(() -> sequenceRepository.save(new NoticeSequence(year)));
        int value = sequence.next();
        return "%d-%04d".formatted(year, value);
    }
}
