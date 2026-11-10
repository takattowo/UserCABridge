package io.github.takattowo.usercabridge;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.libxposed.service.XposedService;
import io.github.libxposed.service.XposedServiceHelper;

public final class UserCABridgeApp extends Application
        implements XposedServiceHelper.OnServiceListener {
    private final List<XposedService> services = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        XposedServiceHelper.registerListener(this);
    }

    @Override
    public void onServiceBind(XposedService service) {
        synchronized (services) {
            if (!services.contains(service)) {
                services.add(service);
            }
        }
    }

    @Override
    public void onServiceDied(XposedService service) {
        synchronized (services) {
            services.remove(service);
        }
    }

    List<XposedService> getServices() {
        synchronized (services) {
            return new ArrayList<>(services);
        }
    }

    synchronized void refreshReportProviderGrants() {
        Set<String> scopedPackages = new HashSet<>();
        for (XposedService service : getServices()) {
            try {
                if (service.getApiVersion() >= XposedService.API_101) {
                    for (String packageName : service.getScope()) {
                        if (packageName != null && !packageName.trim().isEmpty()) {
                            scopedPackages.add(packageName);
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // Ignore scopes from a service that cannot be verified.
            }
        }

        Uri reportUri = StatusReports.reportUri();
        int grantFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
        Set<String> packagesToRevoke = StatusReports.updateScopedPackages(scopedPackages);
        for (String packageName : packagesToRevoke) {
            try {
                revokeUriPermission(packageName, reportUri, grantFlags);
            } catch (RuntimeException ignored) {
                // Removed packages stay denied by the in-memory scope check.
            }
        }
        for (String packageName : StatusReports.packagesNeedingUriGrant()) {
            try {
                grantUriPermission(packageName, reportUri, grantFlags);
                StatusReports.recordUriGrant(packageName);
            } catch (RuntimeException ignored) {
                // A failed grant stays unrecorded and cannot authorize reports.
            }
        }
    }
}
