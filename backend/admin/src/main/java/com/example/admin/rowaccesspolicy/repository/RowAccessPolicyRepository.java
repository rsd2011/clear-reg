package com.example.admin.rowaccesspolicy.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.admin.rowaccesspolicy.domain.RowAccessPolicy;

public interface RowAccessPolicyRepository extends JpaRepository<RowAccessPolicy, UUID> {

    List<RowAccessPolicy> findByActiveTrueOrderByPriorityAsc();
}
