package com.example.server.readmodel;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.dw.application.readmodel.PermissionMenuItem;
import com.example.dw.application.readmodel.PermissionMenuReadModel;
import com.example.dw.application.readmodel.PermissionMenuReadModelSource;

/**
 * 임시 Permission Menu ReadModel 소스. 실제 권한/메뉴 데이터 연동은 후속 단계에서 교체한다.
 */
@Component
public class DefaultPermissionMenuReadModelSource implements PermissionMenuReadModelSource {

    private final Clock clock;

    public DefaultPermissionMenuReadModelSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public PermissionMenuReadModel snapshot(String principalId) {
        return new PermissionMenuReadModel(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(clock),
                List.of(new PermissionMenuItem("PM_ROOT", "Root", "DASHBOARD", "READ", "/"))
        );
    }
}
