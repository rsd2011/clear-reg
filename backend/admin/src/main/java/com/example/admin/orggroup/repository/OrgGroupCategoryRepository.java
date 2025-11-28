package com.example.admin.orggroup.repository;

import java.util.List;
import java.util.Set;

import com.example.admin.orggroup.domain.OrgGroupCategoryMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrgGroupCategoryRepository extends JpaRepository<OrgGroupCategoryMap, java.util.UUID> {

    @Query("select m.categoryCode from OrgGroupCategoryMap m where m.groupCode in :groupCodes")
    Set<String> findCategoryCodesByGroupCodes(@Param("groupCodes") List<String> groupCodes);
}
