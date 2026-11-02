package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessTrustStatusTest {
    private TrustReport report(boolean sourcePresent, int certificateCount) {
        return new TrustReport(10123, 421, "org.example.app", sourcePresent,
                certificateCount, 1_700_000_000_000L);
    }

    @Test
    public void missingReportWaitsInsteadOfShowingSuccess() {
        assertEquals(ProcessTrustStatus.State.WAITING_FOR_REPORT,
                ProcessTrustStatus.evaluate(10123, 421, "org.example.app", true, null));
    }

    @Test
    public void reportFromAnotherProcessWaitsInsteadOfShowingSuccess() {
        assertEquals(ProcessTrustStatus.State.WAITING_FOR_REPORT,
                ProcessTrustStatus.evaluate(10123, 422, "org.example.app", true,
                        report(true, 1)));
    }

    @Test
    public void staleTargetNeverShowsLiveTrust() {
        assertEquals(ProcessTrustStatus.State.STALE_TARGET,
                ProcessTrustStatus.evaluate(10123, 421, "org.example.app", false,
                        report(true, 1)));
    }

    @Test
    public void matchingSourceAndUserCaReportsReady() {
        assertEquals(ProcessTrustStatus.State.USER_CA_READY,
                ProcessTrustStatus.evaluate(10123, 421, "org.example.app", true,
                        report(true, 1)));
    }

    @Test
    public void matchingSourceWithEmptyStoreReportsNoUserCas() {
        assertEquals(ProcessTrustStatus.State.NO_USER_CAS,
                ProcessTrustStatus.evaluate(10123, 421, "org.example.app", true,
                        report(true, 0)));
    }

    @Test
    public void missingUserSourceNeverShowsReady() {
        assertEquals(ProcessTrustStatus.State.USER_SOURCE_MISSING,
                ProcessTrustStatus.evaluate(10123, 421, "org.example.app", true,
                        report(false, 0)));
    }
}
