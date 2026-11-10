package io.github.takattowo.usercabridge;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.junit.Test;

public class ScopedReportAccessTest {
    @Test
    public void scopedPackageCanReport() {
        Set<String> scope = Collections.singleton("org.example.scoped");

        assertTrue(ScopedReportAccess.isScopedPackage("org.example.scoped", scope));
    }

    @Test
    public void systemOrUnknownPackageCannotReport() {
        Set<String> scope = Collections.singleton("org.example.scoped");

        assertFalse(ScopedReportAccess.isScopedPackage("android", scope));
        assertFalse(ScopedReportAccess.isScopedPackage("org.example.unscoped", scope));
        assertFalse(ScopedReportAccess.isScopedPackage(null, scope));
    }
}
