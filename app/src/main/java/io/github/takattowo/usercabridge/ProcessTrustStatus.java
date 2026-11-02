package io.github.takattowo.usercabridge;

final class ProcessTrustStatus {
    enum State {
        STALE_TARGET,
        WAITING_FOR_REPORT,
        USER_SOURCE_MISSING,
        NO_USER_CAS,
        USER_CA_READY
    }

    private ProcessTrustStatus() { }

    static State evaluate(int uid, int pid, String processName, boolean targetUpToDate,
            TrustReport report) {
        if (!targetUpToDate) {
            return State.STALE_TARGET;
        }
        if (report == null || !report.isLiveFor(uid, pid, processName, true)) {
            return State.WAITING_FOR_REPORT;
        }
        if (!report.userSourcePresent) {
            return State.USER_SOURCE_MISSING;
        }
        return report.hasInstalledUserCAs() ? State.USER_CA_READY : State.NO_USER_CAS;
    }
}
