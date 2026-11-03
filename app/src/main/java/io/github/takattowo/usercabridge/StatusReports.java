package io.github.takattowo.usercabridge;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class StatusReports {
    static final String AUTHORITY = "io.github.takattowo.usercabridge.trustreports";
    static final String METHOD_REPORT = "report";
    static final String METHOD_REPORT_HOOK_SETUP = "reportHookSetup";
    static final String EXTRA_PROCESS_NAME = "processName";
    static final String EXTRA_USER_SOURCE_PRESENT = "userSourcePresent";
    static final String EXTRA_USER_CERTIFICATE_COUNT = "userCertificateCount";
    static final String EXTRA_FRAMEWORK_SUPPORTED = "frameworkSupported";
    static final String EXTRA_SUPPORTED_FACTORY_COUNT = "supportedFactoryCount";
    static final String EXTRA_INSTALLED_FACTORY_COUNT = "installedFactoryCount";
    static final String RESULT_STORED = "stored";

    private static final String PREFERENCES_NAME = "trust-reports";
    private static final String HOOK_SETUP_PREFERENCES_NAME = "hook-setup-reports";

    private StatusReports() { }

    static void report(Context context, String processName, int userCertificateCount) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PROCESS_NAME, processName);
        extras.putBoolean(EXTRA_USER_SOURCE_PRESENT, true);
        extras.putInt(EXTRA_USER_CERTIFICATE_COUNT, userCertificateCount);
        ContentResolver resolver = context.getContentResolver();
        Bundle result = resolver.call(Uri.parse("content://" + AUTHORITY), METHOD_REPORT,
                null, extras);
        if (result == null || !result.getBoolean(RESULT_STORED, false)) {
            throw new IllegalStateException("Trust report was not stored");
        }
    }

    static void reportHookSetup(Context context, String processName, boolean frameworkSupported,
            int supportedFactoryCount, int installedFactoryCount) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PROCESS_NAME, processName);
        extras.putBoolean(EXTRA_FRAMEWORK_SUPPORTED, frameworkSupported);
        extras.putInt(EXTRA_SUPPORTED_FACTORY_COUNT, supportedFactoryCount);
        extras.putInt(EXTRA_INSTALLED_FACTORY_COUNT, installedFactoryCount);
        ContentResolver resolver = context.getContentResolver();
        Bundle result = resolver.call(Uri.parse("content://" + AUTHORITY),
                METHOD_REPORT_HOOK_SETUP, null, extras);
        if (result == null || !result.getBoolean(RESULT_STORED, false)) {
            throw new IllegalStateException("Hook setup report was not stored");
        }
    }

    static void store(Context context, TrustReport report) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(report.storageKey(), report.encode()).apply();
    }

    static void store(Context context, HookSetupReport report) {
        context.getSharedPreferences(HOOK_SETUP_PREFERENCES_NAME, Context.MODE_PRIVATE).edit()
                .putString(report.storageKey(), report.encode()).apply();
    }

    static List<TrustReport> load(Context context) {
        Map<String, ?> values = context.getSharedPreferences(PREFERENCES_NAME,
                Context.MODE_PRIVATE).getAll();
        List<TrustReport> reports = new ArrayList<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (entry.getValue() instanceof String) {
                try {
                    reports.add(TrustReport.decode(entry.getKey(), (String) entry.getValue()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed local entries rather than presenting them as real reports.
                }
            }
        }
        return reports;
    }

    static List<HookSetupReport> loadHookSetup(Context context) {
        Map<String, ?> values = context.getSharedPreferences(HOOK_SETUP_PREFERENCES_NAME,
                Context.MODE_PRIVATE).getAll();
        List<HookSetupReport> reports = new ArrayList<>();
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            if (entry.getValue() instanceof String) {
                try {
                    reports.add(HookSetupReport.decode(entry.getKey(), (String) entry.getValue()));
                } catch (IllegalArgumentException ignored) {
                    // Ignore malformed local entries rather than presenting them as real reports.
                }
            }
        }
        return reports;
    }
}
