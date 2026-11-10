package io.github.takattowo.usercabridge;

final class TrustReport {
    final int uid;
    final int pid;
    final String processName;
    final String processSessionId;
    final boolean userSourcePresent;
    final int userCertificateCount;
    final long timestampMillis;

    TrustReport(int uid, int pid, String processName, String processSessionId,
            boolean userSourcePresent, int userCertificateCount, long timestampMillis) {
        if (uid < 0 || pid <= 0 || processName == null || processName.trim().isEmpty()
                || processName.contains("|") || processName.length() > 256
                || processSessionId == null || processSessionId.trim().isEmpty()
                || processSessionId.contains("|") || processSessionId.length() > 64
                || userCertificateCount < 0
                || (!userSourcePresent && userCertificateCount != 0) || timestampMillis <= 0) {
            throw new IllegalArgumentException("Invalid trust report");
        }
        this.uid = uid;
        this.pid = pid;
        this.processName = processName;
        this.processSessionId = processSessionId;
        this.userSourcePresent = userSourcePresent;
        this.userCertificateCount = userCertificateCount;
        this.timestampMillis = timestampMillis;
    }

    boolean matchesTarget(int targetUid, String targetProcessName) {
        return uid == targetUid && processName.equals(targetProcessName);
    }

    boolean hasInstalledUserCAs() {
        return userSourcePresent && userCertificateCount > 0;
    }

    String storageKey() {
        return uid + "|" + processName;
    }

    String encode() {
        return pid + "|" + userSourcePresent + "|" + userCertificateCount + "|"
                + timestampMillis + "|" + processSessionId;
    }

    static TrustReport decode(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Missing trust report data");
        }
        String[] identity = key.split("\\|", 2);
        String[] fields = value.split("\\|", -1);
        if (identity.length != 2 || fields.length != 5
                || !("true".equals(fields[1]) || "false".equals(fields[1]))) {
            throw new IllegalArgumentException("Malformed trust report");
        }
        try {
            return new TrustReport(Integer.parseInt(identity[0]), Integer.parseInt(fields[0]),
                    identity[1], fields[4], Boolean.parseBoolean(fields[1]),
                    Integer.parseInt(fields[2]), Long.parseLong(fields[3]));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Malformed trust report", error);
        }
    }
}
