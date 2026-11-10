package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class StatusReportRetentionTest {
    @Test
    public void evictsOldestReportOnlyWhenAddingBeyondLimit() {
        Map<String, Long> timestamps = new HashMap<>();
        timestamps.put("older", 1L);
        timestamps.put("newer", 2L);

        assertNull(StatusReportRetention.evictionCandidate(timestamps, "incoming", 3));
        assertNull(StatusReportRetention.evictionCandidate(timestamps, "older", 2));
        assertEquals("older", StatusReportRetention.evictionCandidate(timestamps,
                "incoming", 2));
    }
}
