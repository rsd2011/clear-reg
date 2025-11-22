package com.example.draft.application.request;

final class RequestValidationUtil {
    private RequestValidationUtil() {}

    static boolean hasOrganization(String organizationCode) {
        return organizationCode != null && !organizationCode.isBlank();
    }
}
