package com.example.draft.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.DraftHistory;

public interface DraftHistoryRepository extends JpaRepository<DraftHistory, UUID> {

    List<DraftHistory> findByDraftIdOrderByOccurredAtAsc(UUID draftId);
}
