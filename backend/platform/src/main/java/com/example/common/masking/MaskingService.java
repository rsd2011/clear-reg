package com.example.common.masking;

/**
 * 주어진 Maskable에 대해 정책/대상별로 raw 또는 masked 표현을 선택한다.
 */
public class MaskingService {

    private final MaskingStrategy strategy;
    private final UnmaskAuditSink auditSink;

    public MaskingService(MaskingStrategy strategy) {
        this(strategy, null);
    }

    public MaskingService(MaskingStrategy strategy, UnmaskAuditSink auditSink) {
        this.strategy = strategy;
        this.auditSink = auditSink;
    }

    public String render(Maskable maskable, MaskingTarget target) {
        if (maskable == null) return null;
        return render(maskable, target, null);
    }

    public String render(Maskable maskable, MaskingTarget target, String fieldName) {
        if (maskable == null) return null;
        String result = strategy.apply(maskable.raw(), target, maskable.masked(), fieldName);
        if (auditSink != null && target != null && !result.equals(maskable.masked())) {
            auditSink.handle(UnmaskAuditEvent.builder()
                    .eventTime(java.time.Instant.now())
                    .subjectType(target.getSubjectType())
                    .dataKind(target.getDataKind())
                    .fieldName(fieldName)
                    .rowId(target.getRowId())
                    .requesterRoles(target.getRequesterRoles())
                    .build());
        }
        return result;
    }
}
