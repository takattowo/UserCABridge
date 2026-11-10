package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LiveProcessReportsTest {
    @Test
    public void reportIsLiveOnlyWhileItsProcessTokenIsRegistered() {
        LiveProcessReports reports = new LiveProcessReports();
        Object token = new Object();

        reports.track("10123|org.example.app", "session-a", token);
        assertSame(token, reports.tokenFor("10123|org.example.app", "session-a"));

        reports.remove("10123|org.example.app", "session-a", token);
        assertFalse(reports.contains("10123|org.example.app", "session-a"));
    }

    @Test
    public void oldDeathEventCannotRemoveNewProcessToken() {
        LiveProcessReports reports = new LiveProcessReports();
        Object oldToken = new Object();
        Object newToken = new Object();
        String key = "10123|org.example.app";

        reports.track(key, "session-a", oldToken);
        reports.track(key, "session-b", newToken);
        reports.remove(key, "session-a", oldToken);

        assertFalse(reports.contains(key, "session-a"));
        assertTrue(reports.contains(key, "session-b"));
        assertSame(newToken, reports.tokenFor(key, "session-b"));
    }
}
