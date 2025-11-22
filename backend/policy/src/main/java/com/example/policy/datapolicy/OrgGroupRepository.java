package com.example.policy.datapolicy;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrgGroupRepository extends JpaRepository<OrgGroupMember, java.util.UUID> {

    @Query("select distinct m.groupCode from OrgGroupMember m where m.orgId in :orgIds order by m.priority asc")
    Set<String> findGroupsByOrgIds(List<String> orgIds);

    List<OrgGroupMember> findByOrgIdInOrderByPriorityAsc(List<String> orgIds);

    OrgGroup findTopByOrderByPriorityDesc();
}
