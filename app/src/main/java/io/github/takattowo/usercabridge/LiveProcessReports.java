package io.github.takattowo.usercabridge;

import java.util.HashMap;
import java.util.Map;

final class LiveProcessReports {
    private final Map<String, Object> tokens = new HashMap<>();

    synchronized void track(String processKey, Object token) {
        tokens.put(processKey, token);
    }

    synchronized Object tokenFor(String processKey) {
        return tokens.get(processKey);
    }

    synchronized boolean contains(String processKey) {
        return tokens.containsKey(processKey);
    }

    synchronized void remove(String processKey, Object token) {
        if (tokens.get(processKey) == token) {
            tokens.remove(processKey);
        }
    }
}
