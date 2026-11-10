package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ScopedReportGrantRegistryTest {
    @Test
    public void scopedPackageNeedsRecordedUriGrant() {
        ScopedReportAccess access = new ScopedReportAccess();
        access.updateScope(Collections.singleton("org.example.scoped"));

        assertFalse(access.hasReportAccess("org.example.scoped"));
        access.recordUriGrant("org.example.scoped");
        assertTrue(access.hasReportAccess("org.example.scoped"));
        assertFalse(access.hasReportAccess("org.example.unscoped"));
    }

    @Test
    public void scopeRefreshKeepsRetainedGrantAndReturnsOnlyRemovedPackages() {
        ScopedReportAccess access = new ScopedReportAccess();
        Set<String> initialScope = new HashSet<>(Arrays.asList(
                "org.example.kept", "org.example.removed"));
        access.updateScope(initialScope);
        access.recordUriGrant("org.example.kept");
        access.recordUriGrant("org.example.removed");

        Set<String> removed = access.updateScope(new HashSet<>(Arrays.asList(
                "org.example.kept", "org.example.new")));

        assertEquals(Collections.singleton("org.example.removed"), removed);
        assertTrue(access.hasReportAccess("org.example.kept"));
        assertFalse(access.hasReportAccess("org.example.removed"));
        assertFalse(access.hasReportAccess("org.example.new"));
        assertEquals(Collections.singleton("org.example.new"), access.packagesNeedingUriGrant());
    }

    @Test
    public void providerRestartClearsScopeAndRecordedGrants() {
        ScopedReportAccess access = new ScopedReportAccess();
        Set<String> scope = Collections.singleton("org.example.scoped");
        access.updateScope(scope);
        access.recordUriGrant("org.example.scoped");

        access.clear();
        access.updateScope(scope);

        assertFalse(access.hasReportAccess("org.example.scoped"));
        assertEquals(scope, access.packagesNeedingUriGrant());
    }
}
