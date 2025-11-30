package com.example.admin.orggroup.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.example.admin.orggroup.domain.OrgGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrgGroupRepository extends JpaRepository<OrgGroup, UUID> {

    Optional<OrgGroup> findByCode(String code);

    /**
     * 주어진 orgId 목록에 해당하는 OrgGroup들을 sort 순으로 조회.
     * OrgGroupMember를 통해 OrgGroup을 조인하여 조회한다.
     */
    @Query("""
        select distinct g from OrgGroup g
        join OrgGroupMember m on m.orgGroup = g
        where m.orgId in :orgIds
        order by g.sort asc nulls last
        """)
    List<OrgGroup> findByMemberOrgIdsOrderBySortAsc(List<String> orgIds);

    /**
     * 주어진 orgId 목록에 해당하는 그룹 코드들을 sort 순으로 조회.
     */
    @Query("""
        select distinct g.code from OrgGroupMember m
        join m.orgGroup g
        where m.orgId in :orgIds
        order by g.sort asc nulls last
        """)
    Set<String> findGroupCodesByOrgIds(List<String> orgIds);
}
