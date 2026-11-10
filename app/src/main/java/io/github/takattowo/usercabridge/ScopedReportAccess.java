package io.github.takattowo.usercabridge;

import java.util.HashSet;
import java.util.Set;

final class ScopedReportAccess {
    private final Set<String> scopedPackages = new HashSet<>();
    private final Set<String> uriGrantedPackages = new HashSet<>();

    synchronized Set<String> updateScope(Set<String> packages) {
        Set<String> removedPackages = new HashSet<>(uriGrantedPackages);
        removedPackages.removeAll(packages);
        scopedPackages.clear();
        scopedPackages.addAll(packages);
        uriGrantedPackages.retainAll(packages);
        return removedPackages;
    }

    synchronized Set<String> packagesNeedingUriGrant() {
        Set<String> packages = new HashSet<>(scopedPackages);
        packages.removeAll(uriGrantedPackages);
        return packages;
    }

    synchronized void recordUriGrant(String packageName) {
        if (isScopedPackage(packageName, scopedPackages)) {
            uriGrantedPackages.add(packageName);
        }
    }

    synchronized boolean hasReportAccess(String packageName) {
        return isScopedPackage(packageName, scopedPackages)
                && uriGrantedPackages.contains(packageName);
    }

    synchronized void clear() {
        scopedPackages.clear();
        uriGrantedPackages.clear();
    }

    static boolean isScopedPackage(String packageName, Set<String> scopedPackages) {
        return packageName != null && scopedPackages != null && scopedPackages.contains(packageName);
    }
}
