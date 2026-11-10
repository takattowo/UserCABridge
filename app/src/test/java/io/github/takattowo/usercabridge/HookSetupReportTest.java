package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookSetupReportTest {
    private HookSetupReport report() {
        return new HookSetupReport(10123, 421, "org.example.app", "session-a", true, 2, 1,
                1_700_000_000_000L);
    }

    @Test
    public void storedReportRoundTripsIdentityAndHookCounts() {
        HookSetupReport original = report();

        assertEquals("10123|org.example.app", original.storageKey());
        assertEquals("421|true|2|1|1700000000000|session-a", original.encode());

        HookSetupReport restored = HookSetupReport.decode(original.storageKey(),
                original.encode());
        assertTrue(restored.matchesTarget(10123, "org.example.app"));
        assertEquals("session-a", restored.processSessionId);
        assertEquals(2, restored.supportedFactoryCount);
        assertEquals(1, restored.installedFactoryCount);
        assertEquals(1_700_000_000_000L, restored.timestampMillis);
    }

    @Test
    public void reportMatchesTargetByUidAndProcessName() {
        HookSetupReport report = report();

        assertTrue(report.matchesTarget(10123, "org.example.app"));
        assertFalse(report.matchesTarget(10124, "org.example.app"));
        assertFalse(report.matchesTarget(10123, "org.example.app:network"));
        assertEquals(421, report.pid);
    }

    @Test
    public void unsupportedFrameworkIsRepresentedWithoutInventedCounts() {
        HookSetupReport report = new HookSetupReport(10123, 421, "org.example.app", "session-a", false,
                0, 0, 1_700_000_000_000L);

        assertFalse(report.frameworkSupported);
        assertEquals(0, report.supportedFactoryCount);
        assertEquals(0, report.installedFactoryCount);
    }

    @Test
    public void inconsistentHookCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "session-a", true, -1, 0, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "session-a", true, 1, -1, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "session-a", true, 1, 2, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "session-a", false, 1, 0, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "session-a", true, 0, 0, 1_700_000_000_000L));
    }

    @Test
    public void excessiveProcessNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "x".repeat(257), "session-a", true, 1, 1, 1_700_000_000_000L));
    }

    @Test
    public void invalidSessionIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "bad|session", true, 1, 1,
                1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", "x".repeat(65), true, 1, 1,
                1_700_000_000_000L));
    }
}
