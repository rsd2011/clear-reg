package com.example.policy.datapolicy;

import com.example.common.jpa.PrimaryKeyEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "org_group_member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgGroupMember extends PrimaryKeyEntity {

  @Column(length = 100, nullable = false)
  private String groupCode;

  @Column(length = 100, nullable = false)
  private String orgId; // DW 조직 ID

  @Column(length = 255)
  private String orgName; // 참조용 표시

  @Column(length = 100)
  private String leaderPermGroupCode;

  @Column(length = 100)
  private String responsiblePermGroupCode;

  @Column(length = 100)
  private String memberPermGroupCode;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 100;
}
