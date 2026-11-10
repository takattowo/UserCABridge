package io.github.takattowo.usercabridge;

final class HookSetupReport {
    final int uid;
    final int pid;
    final String processName;
    final boolean frameworkSupported;
    final int supportedFactoryCount;
    final int installedFactoryCount;
    final long timestampMillis;

    HookSetupReport(int uid, int pid, String processName, boolean frameworkSupported,
            int supportedFactoryCount, int installedFactoryCount, long timestampMillis) {
        if (uid < 0 || pid <= 0 || processName == null || processName.trim().isEmpty()
                || processName.contains("|") || processName.length() > 256
                || supportedFactoryCount < 0
                || installedFactoryCount < 0 || installedFactoryCount > supportedFactoryCount
                || (frameworkSupported && supportedFactoryCount == 0)
                || (!frameworkSupported && supportedFactoryCount != 0)
                || timestampMillis <= 0) {
            throw new IllegalArgumentException("Invalid hook setup report");
        }
        this.uid = uid;
        this.pid = pid;
        this.processName = processName;
        this.frameworkSupported = frameworkSupported;
        this.supportedFactoryCount = supportedFactoryCount;
        this.installedFactoryCount = installedFactoryCount;
        this.timestampMillis = timestampMillis;
    }

    boolean isLiveFor(int targetUid, int targetPid, String targetProcessName,
            boolean targetUpToDate) {
        return targetUpToDate && matchesProcess(targetUid, targetPid, targetProcessName);
    }

    boolean matchesProcess(int targetUid, int targetPid, String targetProcessName) {
        return uid == targetUid && pid == targetPid && processName.equals(targetProcessName);
    }

    String storageKey() {
        return uid + "|" + processName;
    }

    String encode() {
        return pid + "|" + frameworkSupported + "|" + supportedFactoryCount + "|"
                + installedFactoryCount + "|" + timestampMillis;
    }

    static HookSetupReport decode(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Missing hook setup report data");
        }
        String[] identity = key.split("\\|", 2);
        String[] fields = value.split("\\|", -1);
        if (identity.length != 2 || fields.length != 5
                || !("true".equals(fields[1]) || "false".equals(fields[1]))) {
            throw new IllegalArgumentException("Malformed hook setup report");
        }
        try {
            return new HookSetupReport(Integer.parseInt(identity[0]), Integer.parseInt(fields[0]),
                    identity[1], Boolean.parseBoolean(fields[1]), Integer.parseInt(fields[2]),
                    Integer.parseInt(fields[3]), Long.parseLong(fields[4]));
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Malformed hook setup report", error);
        }
    }
}
