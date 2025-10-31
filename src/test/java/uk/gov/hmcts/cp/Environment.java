package uk.gov.hmcts.cp;

import java.util.Optional;

public final class Environment {

    private Environment() {}

    public static String require(String key) {
        return Optional
                .ofNullable(System.getenv(key))
                .filter(s -> !s.isBlank())
                .orElseThrow(() -> new IllegalStateException("Required environment variable " + key + " is missing or blank"));
    }
}
