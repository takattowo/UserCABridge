package io.github.takattowo.usercabridge;

import java.util.Map;

final class StatusReportRetention {
    static final int MAX_REPORTS = 128;

    private StatusReportRetention() { }

    static String evictionCandidate(Map<String, Long> reportTimestamps, String incomingKey,
            int maximum) {
        if (reportTimestamps.containsKey(incomingKey) || reportTimestamps.size() < maximum) {
            return null;
        }
        String oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;
        for (Map.Entry<String, Long> entry : reportTimestamps.entrySet()) {
            if (entry.getValue() < oldestTimestamp) {
                oldestKey = entry.getKey();
                oldestTimestamp = entry.getValue();
            }
        }
        return oldestKey;
    }
}
