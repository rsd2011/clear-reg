package com.example.draft.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.draft.domain.ApprovalGroupMember;

public interface ApprovalGroupMemberRepository extends JpaRepository<ApprovalGroupMember, UUID> {

    List<ApprovalGroupMember> findByApprovalGroupIdAndActiveTrue(UUID approvalGroupId);
}
