package com.example.server.readmodel;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.dw.application.readmodel.MenuItem;
import com.example.dw.application.readmodel.MenuReadModel;
import com.example.dw.application.readmodel.MenuReadModelSource;

/**
 * 임시 기본 메뉴 Read Model 소스. 실제 메뉴/권한 데이터 연동은 후속 단계에서 교체한다.
 */
@Component
public class DefaultMenuReadModelSource implements MenuReadModelSource {

    private final Clock clock;

    public DefaultMenuReadModelSource(Clock clock) {
        this.clock = clock;
    }

    @Override
    public MenuReadModel snapshot() {
        return new MenuReadModel(
                UUID.randomUUID().toString(),
                OffsetDateTime.now(clock),
                List.of(new MenuItem("ROOT", "Root", "DASHBOARD", "READ", "/"))
        );
    }
}
