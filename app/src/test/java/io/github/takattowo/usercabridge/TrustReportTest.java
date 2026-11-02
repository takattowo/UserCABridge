package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrustReportTest {
    private TrustReport report() {
        return new TrustReport(10123, 421, "org.example.app:network", true, 1,
                1_700_000_000_000L);
    }

    @Test
    public void exactCurrentTargetMatchesReport() {
        assertTrue(report().isLiveFor(10123, 421, "org.example.app:network", true));
    }

    @Test
    public void differentUidDoesNotMatchReport() {
        assertFalse(report().isLiveFor(10124, 421, "org.example.app:network", true));
    }

    @Test
    public void differentPidDoesNotMatchReport() {
        assertFalse(report().isLiveFor(10123, 422, "org.example.app:network", true));
    }

    @Test
    public void differentProcessNameDoesNotMatchReport() {
        assertFalse(report().isLiveFor(10123, 421, "org.example.app", true));
    }

    @Test
    public void staleModuleTargetDoesNotMatchAsLive() {
        assertFalse(report().isLiveFor(10123, 421, "org.example.app:network", false));
    }

    @Test
    public void emptyUserStoreDoesNotMeanAnAdGuardCaIsInstalled() {
        assertFalse(new TrustReport(10123, 421, "org.example.app", true, 0,
                1_700_000_000_000L).hasInstalledUserCAs());
    }

    @Test
    public void positiveCertificateCountReportsInstalledUserCas() {
        assertTrue(report().hasInstalledUserCAs());
    }

    @Test
    public void negativeUidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(-1, 421,
                "org.example.app", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void nonpositivePidIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 0,
                "org.example.app", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void emptyProcessNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "", true, 1, 1_700_000_000_000L));
    }

    @Test
    public void negativeUserCertificateCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", true, -1, 1_700_000_000_000L));
    }

    @Test
    public void certificatesCannotBeReportedWhenUserSourceIsAbsent() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", false, 1, 1_700_000_000_000L));
    }

    @Test
    public void nonpositiveReportTimestampIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new TrustReport(10123, 421,
                "org.example.app", true, 1, 0));
    }
}
