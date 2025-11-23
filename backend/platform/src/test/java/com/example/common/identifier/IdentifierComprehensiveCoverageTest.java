package com.example.common.identifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * 식별자 값 객체 전반의 성공/실패 경로를 모두 태우기 위한 커버리지 보강 테스트.
 */
@DisplayName("식별자 값 객체 커버리지 보강")
class IdentifierComprehensiveCoverageTest {

    private record IdCase(String name, Function<String, Object> factory, String valid, String alternate, String invalid) {}

    @TestFactory
    @DisplayName("정상 생성·마스킹·동등성 + 실패 케이스")
    List<DynamicTest> identifiers() {
        List<IdCase> cases = List.of(
                new IdCase("CustomerId", CustomerId::of, "CUS12345", "CUS12346", ""),
                new IdCase("EmployeeId", EmployeeId::of, "EMP12345", "EMP22345", "  "),
                new IdCase("OrganizationId", OrganizationId::of, "ORG12345", "ORG22345", " "),
                new IdCase("AccountId", AccountId::of, "1234567890", "1234567891", "ac"),
                new IdCase("PassportId", PassportId::of, "M1234567", "M1234568", "*bad"),
                new IdCase("SecuritiesId", SecuritiesId::of, "SEC123456", "SEC123457", "bad!"),
                new IdCase("EmailAddress", EmailAddress::of, "user@example.com", "other@example.com", "not-an-email"),
                new IdCase("OrganizationName", OrganizationName::of, "MyBank", "YourBank", ""),
                new IdCase("DriverLicenseId", DriverLicenseId::of, "1234567890", "1234567891", "abc"),
                new IdCase("BusinessRegistrationId", BusinessRegistrationId::of, "123-45-67890", "223-45-67890", "123"),
                new IdCase("CorporateRegistrationId", CorporateRegistrationId::of, "1234567890123", "2234567890123", "코드"),
                new IdCase("PersonName", PersonName::of, "홍길동", "김철수", ""),
                new IdCase("ResidentRegistrationId", ResidentRegistrationId::of, "900101-1234567", "900102-1234567", "900101-12"),
                new IdCase("PhoneNumber", PhoneNumber::of, "+821012345678", "+821012345679", "12"),
                new IdCase("CardId", CardId::of, "4111 1111 1111 1111", "5555 5555 5555 4444", "4111"),
                new IdCase("AddressLine", value -> Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", null, "06236"), "ignored", "ignored", "ignored")
        );

        return cases.stream().map(c -> DynamicTest.dynamicTest(c.name(), () -> {
            if (!c.name.equals("AddressLine")) {
                Object id = c.factory.apply(c.valid);
                // toString/jsonValue/equals/hashCode/optional raw/masked 호출
                assertThat(id.toString()).isNotBlank();
                invokeIfPresent(id, "masked");
                invokeIfPresent(id, "jsonValue");
                invokeIfPresent(id, "raw");
                Object same = c.factory.apply(c.valid);
                assertThat(id).isEqualTo(same);
                assertThat(id).isEqualTo(id);
                assertThat(id).isNotEqualTo(null);
                Object different = c.factory.apply(c.alternate);
                assertThat(id).isNotEqualTo(different);
                assertThrows(IllegalArgumentException.class, () -> c.factory.apply(c.invalid));
            } else {
                Address addr = (Address) c.factory.apply(c.valid);
                assertThat(addr.masked()).contains("ADDR-REDACTED");
                assertThat(addr.toString()).isEqualTo(addr.jsonValue());
                assertThat(addr).isEqualTo(Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", null, "06236"));
                assertThat(addr).isNotEqualTo(Address.of("KR", "Seoul", "Jongno", "Sejong-daero", null, "03000"));
                Address withLine2 = Address.of("KR", "Seoul", "Gangnam", "Teheran-ro 1", "Line2", "06236");
                assertThat(addr).isNotEqualTo(withLine2);
            }
        })).toList();
    }

    private void invokeIfPresent(Object target, String method) {
        try {
            Method m = target.getClass().getMethod(method);
            m.setAccessible(true);
            Object result = m.invoke(target);
            if (result instanceof String str) {
                assertThat(str).isNotNull();
            }
        } catch (NoSuchMethodException ignored) {
            // optional
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @org.junit.jupiter.api.Test
    @DisplayName("주소 필수/옵션 검증 실패 경로")
    void addressValidationFailures() {
        assertThrows(IllegalArgumentException.class, () -> Address.of("KOR", "Seoul", "Gangnam", "Line1", null, "06236"));
        assertThrows(IllegalArgumentException.class, () -> Address.of("KR", "Seoul", "G", "L", null, "06236"));
        assertThrows(IllegalArgumentException.class, () -> Address.of("", "Seoul", "Gangnam", "Teheran-ro", null, "06236"));

        // AbstractIdentifier 길이 초과/패턴 오류 브랜치
        String longId = "X".repeat(70);
        assertThrows(IllegalArgumentException.class, () -> CustomerId.of(longId));
        assertThrows(IllegalArgumentException.class, () -> EmployeeId.of("BAD@ID"));
    }
}
