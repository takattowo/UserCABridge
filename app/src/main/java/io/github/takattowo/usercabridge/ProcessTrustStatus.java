package io.github.takattowo.usercabridge;

final class ProcessTrustStatus {
    enum State {
        STALE_TARGET,
        WAITING_FOR_REPORT,
        HISTORICAL_REPORT,
        USER_SOURCE_MISSING,
        NO_USER_CAS,
        USER_CA_READY
    }

    private ProcessTrustStatus() { }

    static State evaluate(int uid, int pid, String processName, boolean targetUpToDate,
            boolean reportProcessLive, TrustReport report) {
        if (!targetUpToDate) {
            return State.STALE_TARGET;
        }
        if (report == null || !report.matchesProcess(uid, pid, processName)) {
            return State.WAITING_FOR_REPORT;
        }
        if (!reportProcessLive) {
            return State.HISTORICAL_REPORT;
        }
        if (!report.userSourcePresent) {
            return State.USER_SOURCE_MISSING;
        }
        return report.hasInstalledUserCAs() ? State.USER_CA_READY : State.NO_USER_CAS;
    }
}
