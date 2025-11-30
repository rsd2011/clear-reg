package com.example.admin.orggroup.repository;

import java.util.List;
import java.util.UUID;

import com.example.admin.orggroup.domain.OrgGroup;
import com.example.admin.orggroup.domain.OrgGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrgGroupMemberRepository extends JpaRepository<OrgGroupMember, UUID> {

    /**
     * 조직그룹으로 멤버 조회.
     */
    List<OrgGroupMember> findByOrgGroup(OrgGroup orgGroup);

    /**
     * 조직그룹 코드로 멤버 조회.
     */
    @Query("SELECT m FROM OrgGroupMember m WHERE m.orgGroup.code = :groupCode")
    List<OrgGroupMember> findByGroupCode(@Param("groupCode") String groupCode);

    /**
     * 조직 ID 목록으로 멤버 조회 (표시순서 오름차순).
     */
    @Query("SELECT m FROM OrgGroupMember m WHERE m.orgId IN :orgIds ORDER BY m.displayOrder ASC")
    List<OrgGroupMember> findByOrgIdInOrderByDisplayOrderAsc(@Param("orgIds") List<String> orgIds);

    /**
     * 조직그룹 코드로 멤버 조회 (표시순서 오름차순).
     */
    @Query("SELECT m FROM OrgGroupMember m WHERE m.orgGroup.code = :groupCode ORDER BY m.displayOrder ASC")
    List<OrgGroupMember> findByGroupCodeOrderByDisplayOrderAsc(@Param("groupCode") String groupCode);

    /**
     * 조직그룹으로 멤버 조회 (표시순서 오름차순).
     */
    List<OrgGroupMember> findByOrgGroupOrderByDisplayOrderAsc(OrgGroup orgGroup);

    /**
     * 조직그룹의 멤버 삭제.
     */
    void deleteByOrgGroup(OrgGroup orgGroup);
}
