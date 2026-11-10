package io.github.takattowo.usercabridge;

final class ProcessHookSetupStatus {
    enum State {
        STALE_TARGET,
        WAITING_FOR_SETUP_REPORT,
        HISTORICAL_REPORT,
        FRAMEWORK_UNSUPPORTED,
        NO_HOOKS_INSTALLED,
        PARTIAL_HOOKS_INSTALLED,
        HOOKS_INSTALLED
    }

    private ProcessHookSetupStatus() { }

    static State evaluate(int uid, String processName, boolean targetUpToDate,
            boolean reportProcessLive, HookSetupReport report) {
        if (!targetUpToDate) {
            return State.STALE_TARGET;
        }
        if (report == null || !report.matchesTarget(uid, processName)) {
            return State.WAITING_FOR_SETUP_REPORT;
        }
        if (!reportProcessLive) {
            return State.HISTORICAL_REPORT;
        }
        if (!report.frameworkSupported) {
            return State.FRAMEWORK_UNSUPPORTED;
        }
        if (report.installedFactoryCount == 0) {
            return State.NO_HOOKS_INSTALLED;
        }
        return report.installedFactoryCount == report.supportedFactoryCount
                ? State.HOOKS_INSTALLED : State.PARTIAL_HOOKS_INSTALLED;
    }
}
