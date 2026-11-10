package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProcessHookSetupStatusTest {
    private HookSetupReport report(boolean supported, int supportedFactories,
            int installedFactories) {
        return new HookSetupReport(10123, 421, "org.example.app", supported,
                supportedFactories, installedFactories, 1_700_000_000_000L);
    }

    @Test
    public void missingOrMismatchedSetupReportWaitsForSetupEvidence() {
        assertEquals(ProcessHookSetupStatus.State.WAITING_FOR_SETUP_REPORT,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, false, null));
        assertEquals(ProcessHookSetupStatus.State.WAITING_FOR_SETUP_REPORT,
                ProcessHookSetupStatus.evaluate(10124, 421, "org.example.app", true, true,
                        report(true, 1, 1)));
        assertEquals(ProcessHookSetupStatus.State.WAITING_FOR_SETUP_REPORT,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.other", true, true,
                        report(true, 1, 1)));
    }

    @Test
    public void diagnosticPidMismatchDoesNotRejectLiveSetupReport() {
        assertEquals(ProcessHookSetupStatus.State.HOOKS_INSTALLED,
                ProcessHookSetupStatus.evaluate(10123, 422, "org.example.app", true, true,
                        report(true, 1, 1)));
    }

    @Test
    public void staleTargetDoesNotShowHookSetupAsCurrent() {
        assertEquals(ProcessHookSetupStatus.State.STALE_TARGET,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", false, false,
                        report(true, 1, 1)));
    }

    @Test
    public void unsupportedFrameworkDoesNotClaimHooksInstalled() {
        assertEquals(ProcessHookSetupStatus.State.FRAMEWORK_UNSUPPORTED,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, true,
                        report(false, 0, 0)));
    }

    @Test
    public void zeroInstalledHooksReportsSetupFailure() {
        assertEquals(ProcessHookSetupStatus.State.NO_HOOKS_INSTALLED,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, true,
                        report(true, 2, 0)));
    }

    @Test
    public void partialFactoryInstallationDoesNotReportFullSetup() {
        assertEquals(ProcessHookSetupStatus.State.PARTIAL_HOOKS_INSTALLED,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, true,
                        report(true, 2, 1)));
    }

    @Test
    public void allSupportedFactoryHooksInstalledReportsSetupReady() {
        assertEquals(ProcessHookSetupStatus.State.HOOKS_INSTALLED,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, true,
                        report(true, 2, 2)));
    }

    @Test
    public void reportFromDeadProcessIsHistorical() {
        assertEquals(ProcessHookSetupStatus.State.HISTORICAL_REPORT,
                ProcessHookSetupStatus.evaluate(10123, 421, "org.example.app", true, false,
                        report(true, 1, 1)));
    }
}
