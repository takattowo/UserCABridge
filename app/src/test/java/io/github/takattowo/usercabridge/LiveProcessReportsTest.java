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

        reports.track("10123|421|org.example.app", token);
        assertSame(token, reports.tokenFor("10123|421|org.example.app"));

        reports.remove("10123|421|org.example.app", token);
        assertFalse(reports.contains("10123|421|org.example.app"));
    }

    @Test
    public void oldDeathEventCannotRemoveNewProcessToken() {
        LiveProcessReports reports = new LiveProcessReports();
        Object oldToken = new Object();
        Object newToken = new Object();
        String key = "10123|421|org.example.app";

        reports.track(key, oldToken);
        reports.track(key, newToken);
        reports.remove(key, oldToken);

        assertTrue(reports.contains(key));
        assertSame(newToken, reports.tokenFor(key));
    }
}
