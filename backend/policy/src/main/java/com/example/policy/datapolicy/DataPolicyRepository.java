package com.example.policy.datapolicy;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DataPolicyRepository extends JpaRepository<DataPolicy, Long> {
    List<DataPolicy> findByActiveTrueOrderByPriorityAsc();
}
