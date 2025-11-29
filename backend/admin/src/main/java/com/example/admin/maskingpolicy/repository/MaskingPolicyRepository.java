package com.example.admin.maskingpolicy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.maskingpolicy.domain.MaskingPolicy;

public interface MaskingPolicyRepository extends JpaRepository<MaskingPolicy, UUID> {

    List<MaskingPolicy> findByActiveTrueOrderByPriorityAsc();
}
