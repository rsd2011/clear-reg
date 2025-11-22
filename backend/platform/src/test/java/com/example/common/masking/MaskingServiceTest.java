package com.example.common.masking;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.common.value.PaymentReference;

class MaskingServiceTest {

    @Test
    @DisplayName("MaskingService chooses masked/raw based on strategy")
    void maskingServiceRender() {
        MaskingStrategy strategy = target -> target != null && target.isDefaultMask();
        MaskingService service = new MaskingService(strategy);
        PaymentReference ref = PaymentReference.of("급여 이체 2025-01");

        MaskingTarget mask = MaskingTarget.builder().defaultMask(true).subjectType(SubjectType.CUSTOMER_INDIVIDUAL).build();
        MaskingTarget unmask = MaskingTarget.builder().defaultMask(false).subjectType(SubjectType.EMPLOYEE).build();
        MaskingTarget override = MaskingTarget.builder().defaultMask(true).forceUnmask(true).subjectType(SubjectType.CUSTOMER_INDIVIDUAL).build();
        MaskingTarget partial = MaskingTarget.builder()
                .defaultMask(true)
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .dataKind("PAYMENT_REFERENCE")
                .forceUnmaskKinds(java.util.Set.of("PAYMENT_REFERENCE"))
                .build();
        MaskingTarget fieldOverride = MaskingTarget.builder()
                .defaultMask(true)
                .subjectType(SubjectType.CUSTOMER_INDIVIDUAL)
                .forceUnmaskFields(java.util.Set.of("reasonText"))
                .build();

        assertThat(service.render(ref, mask)).contains("*");
        assertThat(service.render(ref, unmask)).contains("급여");
        assertThat(service.render(ref, override)).contains("급여");
        assertThat(service.render(ref, partial)).contains("급여");
        assertThat(service.render(ref, fieldOverride, "reasonText")).contains("급여");
        assertThat(service.render(ref, fieldOverride, "otherField")).contains("*");
    }
}
