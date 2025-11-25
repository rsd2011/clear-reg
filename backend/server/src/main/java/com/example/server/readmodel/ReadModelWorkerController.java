package com.example.server.readmodel;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Read model 강제 재생성용 기술용 엔드포인트 (운영시 보호 필요). readmodel.worker.enabled=true 일 때만 노출.
 */
@RestController
@RequestMapping("/internal/read-model")
@Tag(name = "ReadModel Worker", description = "읽기모델 강제 재생성 내부 API")
@ConditionalOnProperty(prefix = "readmodel.worker", name = "enabled", havingValue = "true")
public class ReadModelWorkerController {

    private final ReadModelWorker worker;

    public ReadModelWorkerController(ReadModelWorker worker) {
        this.worker = worker;
    }

    @PostMapping("/organization/rebuild")
    public ResponseEntity<Void> rebuildOrganization() {
        worker.rebuildOrganization();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/menu/rebuild")
    public ResponseEntity<Void> rebuildMenu() {
        worker.rebuildMenu();
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/permission-menu/{principalId}/rebuild")
    public ResponseEntity<Void> rebuildPermissionMenu(@PathVariable String principalId) {
        worker.rebuildPermissionMenu(principalId);
        return ResponseEntity.accepted().build();
    }
}
