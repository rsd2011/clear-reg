package com.example.draft.application.response;

final class ResponseMaskingUtil {
    private ResponseMaskingUtil() {}

    static String maskActor(String actor) {
        if (actor == null || actor.isBlank()) {
            return "UNKNOWN";
        }
        return actor.length() <= 2 ? actor : actor.charAt(0) + "***";
    }
}
