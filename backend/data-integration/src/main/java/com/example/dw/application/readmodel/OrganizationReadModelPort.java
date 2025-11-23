package com.example.dw.application.readmodel;

import java.util.Optional;

/**
 * 외부 저장소(예: Redis)에 저장된 조직 Read Model 접근을 위한 Port.
 */
public interface OrganizationReadModelPort {

    /**
     * Read Model 기능이 활성화되어 있는지 여부.
     */
    boolean isEnabled();

    /**
     * 저장된 조직 Read Model 을 조회한다.
     *
     * @return Optional read model snapshot
     */
    Optional<OrganizationTreeReadModel> load();

    /**
     * 소스 데이터를 기반으로 Read Model 을 재생성하고 저장한다.
     *
     * @return 재생성된 read model
     */
    OrganizationTreeReadModel rebuild();

    /**
     * 저장소에서 Read Model 을 삭제한다.
     */
    void evict();
}
