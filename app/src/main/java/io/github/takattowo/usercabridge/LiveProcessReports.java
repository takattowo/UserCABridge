package io.github.takattowo.usercabridge;

import java.util.HashMap;
import java.util.Map;

final class LiveProcessReports {
    private static final class TokenEntry {
        final String sessionId;
        final Object token;

        TokenEntry(String sessionId, Object token) {
            this.sessionId = sessionId;
            this.token = token;
        }
    }

    private final Map<String, TokenEntry> tokens = new HashMap<>();

    synchronized void track(String processKey, String sessionId, Object token) {
        tokens.put(processKey, new TokenEntry(sessionId, token));
    }

    synchronized Object tokenFor(String processKey, String sessionId) {
        TokenEntry entry = tokens.get(processKey);
        return entry != null && entry.sessionId.equals(sessionId) ? entry.token : null;
    }

    synchronized boolean contains(String processKey, String sessionId) {
        TokenEntry entry = tokens.get(processKey);
        return entry != null && entry.sessionId.equals(sessionId);
    }

    synchronized void remove(String processKey, String sessionId, Object token) {
        TokenEntry entry = tokens.get(processKey);
        if (entry != null && entry.sessionId.equals(sessionId) && entry.token == token) {
            tokens.remove(processKey);
        }
    }
}
