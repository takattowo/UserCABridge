package io.github.takattowo.usercabridge;

import java.util.Set;

final class ScopedReportAccess {
    private ScopedReportAccess() { }

    static boolean isScopedPackage(String packageName, Set<String> scopedPackages) {
        return packageName != null && scopedPackages != null && scopedPackages.contains(packageName);
    }
}
