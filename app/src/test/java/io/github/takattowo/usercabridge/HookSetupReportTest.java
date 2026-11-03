package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HookSetupReportTest {
    private HookSetupReport report() {
        return new HookSetupReport(10123, 421, "org.example.app", true, 2, 1,
                1_700_000_000_000L);
    }

    @Test
    public void storedReportRoundTripsIdentityAndHookCounts() {
        HookSetupReport original = report();

        assertEquals("10123|org.example.app", original.storageKey());
        assertEquals("421|true|2|1|1700000000000", original.encode());

        HookSetupReport restored = HookSetupReport.decode(original.storageKey(),
                original.encode());
        assertTrue(restored.matchesProcess(10123, 421, "org.example.app"));
        assertEquals(2, restored.supportedFactoryCount);
        assertEquals(1, restored.installedFactoryCount);
        assertEquals(1_700_000_000_000L, restored.timestampMillis);
    }

    @Test
    public void reportMatchesOnlyExactProcessIdentity() {
        HookSetupReport report = report();

        assertTrue(report.matchesProcess(10123, 421, "org.example.app"));
        assertFalse(report.matchesProcess(10124, 421, "org.example.app"));
        assertFalse(report.matchesProcess(10123, 422, "org.example.app"));
        assertFalse(report.matchesProcess(10123, 421, "org.example.app:network"));
    }

    @Test
    public void unsupportedFrameworkIsRepresentedWithoutInventedCounts() {
        HookSetupReport report = new HookSetupReport(10123, 421, "org.example.app", false,
                0, 0, 1_700_000_000_000L);

        assertFalse(report.frameworkSupported);
        assertEquals(0, report.supportedFactoryCount);
        assertEquals(0, report.installedFactoryCount);
    }

    @Test
    public void inconsistentHookCountsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", true, -1, 0, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", true, 1, -1, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", true, 1, 2, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", false, 1, 0, 1_700_000_000_000L));
        assertThrows(IllegalArgumentException.class, () -> new HookSetupReport(10123, 421,
                "org.example.app", true, 0, 0, 1_700_000_000_000L));
    }
}
