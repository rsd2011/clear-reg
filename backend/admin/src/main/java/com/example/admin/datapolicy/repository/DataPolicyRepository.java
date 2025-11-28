package com.example.admin.datapolicy.repository;

import java.util.List;

import com.example.admin.datapolicy.domain.DataPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataPolicyRepository extends JpaRepository<DataPolicy, Long> {
    List<DataPolicy> findByActiveTrueOrderByPriorityAsc();
}
