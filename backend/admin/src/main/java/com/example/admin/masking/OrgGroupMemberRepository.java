package com.example.admin.masking;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgGroupMemberRepository extends JpaRepository<OrgGroupMember, UUID> {

    List<OrgGroupMember> findByGroupCode(String groupCode);

    List<OrgGroupMember> findByOrgIdInOrderBySortAsc(List<String> orgIds);

    List<OrgGroupMember> findByGroupCodeOrderBySortAsc(String groupCode);
}
