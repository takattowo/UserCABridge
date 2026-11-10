package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrustReportTest {
    private TrustReport report() {
        return new TrustReport(10123, 421, "org.example.app:network", "session-a", true, 1,
                1_700_000_000_000L);
    }

    @Test
    public void reportMatchesTargetByUidAndProcessName() {
        assertTrue(report().matchesTarget(10123, "org.example.app:network"));
    }

    @Test
    public void differentUidDoesNotMatchTarget() {
        assertFalse(report().matchesTarget(10124, "org.example.app:network"));
    }

    @Test
    public void differentProcessNameDoesNotMatchTarget() {
        assertFalse(report().matchesTarget(10123, "org.example.app"));
    }

    @Test
    public void processIdRemainsAvailableAsDiagnosticMetadata() {
        assertEquals(421, report().pid);
    }

    @Test
    public void emptyUserStoreDoesNotMeanAnAdGuardCaIsInstalled() {
        assertFalse(new TrustReport(10123, 421, "org.example.app", "session-a", true, 0,
                1_700_000_000_000L).hasInstalledUserCAs());
    }

    @Test
    public void positiveCertificateCountReportsInstalledUserCas() {
        assertTrue(report().hasInstalledUserCAs());
    }

    @Test
    public void reportHasStableStorageEncoding() {
        assertEquals("10123|org.example.app:network", report().storageKey());
        assertEquals("421|true|1|1700000000000|session-a", report().encode());
    }

    @Test
    public void storedReportRoundTripsIdentityAndTrustResult() {
        TrustReport restored = TrustReport.decode(
                "10123|org.example.app:network", "421|true|1|1700000000000|session-a");

        assertTrue(restored.matchesTarget(10123, "org.example.app:network"));
        assertEquals(421, restored.pid);
        assertEquals("session-a", restored.processSessionId);
        assertTrue(restored.hasInstalledUserCAs());
        assertTrue(restored.timestampMillis == 1_700_000_000_000L);
    }

    @Test
    public void malformedStoredReportIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> TrustReport.decode("not-a-uid|org.example.app", "bad"));
    }

    @Test
    public void negativeUidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(-1, 421,
                "org.example.app", "session-a", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void nonpositivePidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 0,
                "org.example.app", "session-a", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void emptyProcessNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "", "session-a", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void negativeUserCertificateCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", "session-a", true, -1, 1_700_000_000_000L));
    }

    @Test
    public void certificatesCannotBeReportedWhenUserSourceIsAbsent() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", "session-a", false, 1, 1_700_000_000_000L));
    }

    @Test
    public void nonpositiveReportTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", "session-a", true, 1, 0));
    }

    @Test
    public void excessiveProcessNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "x".repeat(257), "session-a", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void sessionIdCannotBreakReportEncoding() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", "bad|session", true, 1, 1_700_000_000_000L));
    }
}
