package com.example.server.drm;

import java.time.Instant;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.example.audit.AuditMode;
import com.example.audit.drm.DrmAuditEvent;
import com.example.audit.drm.DrmAuditService;
import com.example.audit.drm.DrmEventType;

/**
 * DRM 해제/다운로드(엑셀 업/다운로드 등) 시 감사 이벤트를 남기기 위한 스켈레톤 서비스.
 * 실제 DRM 라이브러리 연동부는 추후 추가한다.
 */
@Service
public class DrmReleaseService {

    private final DrmAuditService drmAuditService;

    public DrmReleaseService(DrmAuditService drmAuditService) {
        this.drmAuditService = drmAuditService;
    }

    /** DRM 해제 요청 */
    public void requestRelease(String assetId,
                               String reasonCode,
                               String reasonText,
                               String requestorId,
                               String organizationCode,
                               String route,
                               Set<String> tags) {
        record(DrmEventType.REQUEST, assetId, reasonCode, reasonText, requestorId, null, organizationCode, route, tags, null);
    }

    /** DRM 해제 승인 */
    public void approveRelease(String assetId,
                               String approverId,
                               String reasonCode,
                               String reasonText,
                               String organizationCode,
                               String route,
                               Set<String> tags) {
        record(DrmEventType.APPROVAL, assetId, reasonCode, reasonText, approverId, approverId, organizationCode, route, tags, null);
    }

    /** DRM 해제 실행/다운로드 */
    public void executeRelease(String assetId,
                               String actorId,
                               String organizationCode,
                               String route,
                               Set<String> tags,
                               Instant expiresAt) {
        record(DrmEventType.EXECUTE, assetId, null, null, actorId, null, organizationCode, route, tags, expiresAt);
    }

    private void record(DrmEventType type,
                        String assetId,
                        String reasonCode,
                        String reasonText,
                        String requestorId,
                        String approverId,
                        String organizationCode,
                        String route,
                        Set<String> tags,
                        Instant expiresAt) {
        DrmAuditEvent drm = DrmAuditEvent.builder()
                .assetId(assetId)
                .eventType(type)
                .reasonCode(reasonCode)
                .reasonText(reasonText)
                .requestorId(requestorId)
                .approverId(approverId)
                .organizationCode(organizationCode)
                .route(route)
                .tags(tags)
                .expiresAt(expiresAt)
                .build();
        drmAuditService.record(drm, AuditMode.ASYNC_FALLBACK);
    }
}
