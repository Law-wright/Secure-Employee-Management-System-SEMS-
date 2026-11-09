package edu.sems.web;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AuditTimestamps {

    private static final ZoneId CENTRAL = ZoneId.of("America/Chicago");
    private static final DateTimeFormatter DISPLAY =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.US);

    private AuditTimestamps() {
    }

    public static String format(String occurredAtUtc) {
        Instant instant = Instant.parse(occurredAtUtc);
        ZonedDateTime central = instant.atZone(CENTRAL);
        return DISPLAY.format(central);
    }
}
