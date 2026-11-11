package edu.sems.audit;

import edu.sems.web.AuditTimestamps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditTimestampsTest {

    @Test
    void winterInstant_rendersCentralStandardTime() {
        assertThat(AuditTimestamps.format("2026-01-15T18:00:00Z"))
                .isEqualTo("2026-01-15 12:00:00 CST");
    }

    @Test
    void summerInstant_rendersCentralDaylightTime() {
        assertThat(AuditTimestamps.format("2026-07-15T17:00:00Z"))
                .isEqualTo("2026-07-15 12:00:00 CDT");
    }
}
