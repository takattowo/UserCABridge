package io.github.takattowo.usercabridge;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    static final String EXTRA_PROCESS_SESSION_ID = "processSessionId";
    static final String EXTRA_PROCESS_TOKEN = "processToken";
    static final String RESULT_STORED = "stored";

    private static final String PREFERENCES_NAME = "trust-reports";
    private static final String HOOK_SETUP_PREFERENCES_NAME = "hook-setup-reports";
    private static final LiveProcessReports LIVE_PROCESSES = new LiveProcessReports();
    private static final ScopedReportAccess REPORT_ACCESS = new ScopedReportAccess();

    private StatusReports() { }

    static Uri reportUri() {
        return Uri.parse("content://" + AUTHORITY + "/report");
    }

    static Set<String> updateScopedPackages(Set<String> packages) {
        return REPORT_ACCESS.updateScope(packages);
    }

    static Set<String> packagesNeedingUriGrant() {
        return REPORT_ACCESS.packagesNeedingUriGrant();
    }

    static void recordUriGrant(String packageName) {
        REPORT_ACCESS.recordUriGrant(packageName);
    }

    static boolean hasReportAccess(String packageName) {
        return REPORT_ACCESS.hasReportAccess(packageName);
    }

    static void clearReportProviderAccess() {
        REPORT_ACCESS.clear();
    }

    static boolean isProcessLive(int uid, String processName, String processSessionId) {
        Object token = LIVE_PROCESSES.tokenFor(processKey(uid, processName), processSessionId);
        return token instanceof IBinder && ((IBinder) token).isBinderAlive();
    }

    static synchronized boolean trackProcess(int uid, String processName,
            String processSessionId, IBinder token) {
        String key = processKey(uid, processName);
        if (LIVE_PROCESSES.tokenFor(key, processSessionId) == token) {
            return token.isBinderAlive();
        }
        LIVE_PROCESSES.track(key, processSessionId, token);
        try {
            token.linkToDeath(() -> LIVE_PROCESSES.remove(key, processSessionId, token), 0);
        } catch (RemoteException error) {
            LIVE_PROCESSES.remove(key, processSessionId, token);
            return false;
        }
        if (!token.isBinderAlive()) {
            LIVE_PROCESSES.remove(key, processSessionId, token);
            return false;
        }
        return true;
    }

    private static String processKey(int uid, String processName) {
        return uid + "|" + processName;
    }

    static void report(Context context, String processName, int userCertificateCount,
            String processSessionId, IBinder processToken) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PROCESS_NAME, processName);
        extras.putString(EXTRA_PROCESS_SESSION_ID, processSessionId);
        extras.putBoolean(EXTRA_USER_SOURCE_PRESENT, true);
        extras.putInt(EXTRA_USER_CERTIFICATE_COUNT, userCertificateCount);
        extras.putBinder(EXTRA_PROCESS_TOKEN, processToken);
        ContentResolver resolver = context.getContentResolver();
        Bundle result = resolver.call(reportUri(), METHOD_REPORT, null, extras);
        if (result == null || !result.getBoolean(RESULT_STORED, false)) {
            throw new IllegalStateException("Trust report was not stored");
        }
    }

    static void reportHookSetup(Context context, String processName, boolean frameworkSupported,
            int supportedFactoryCount, int installedFactoryCount, String processSessionId,
            IBinder processToken) {
        Bundle extras = new Bundle();
        extras.putString(EXTRA_PROCESS_NAME, processName);
        extras.putString(EXTRA_PROCESS_SESSION_ID, processSessionId);
        extras.putBoolean(EXTRA_FRAMEWORK_SUPPORTED, frameworkSupported);
        extras.putInt(EXTRA_SUPPORTED_FACTORY_COUNT, supportedFactoryCount);
        extras.putInt(EXTRA_INSTALLED_FACTORY_COUNT, installedFactoryCount);
        extras.putBinder(EXTRA_PROCESS_TOKEN, processToken);
        ContentResolver resolver = context.getContentResolver();
        Bundle result = resolver.call(reportUri(), METHOD_REPORT_HOOK_SETUP, null, extras);
        if (result == null || !result.getBoolean(RESULT_STORED, false)) {
            throw new IllegalStateException("Hook setup report was not stored");
        }
    }

    static synchronized void store(Context context, TrustReport report) {
        storeBounded(context, PREFERENCES_NAME, report.storageKey(), report.encode(), false);
    }

    static synchronized void store(Context context, HookSetupReport report) {
        storeBounded(context, HOOK_SETUP_PREFERENCES_NAME, report.storageKey(),
                report.encode(), true);
    }

    private static void storeBounded(Context context, String preferencesName, String key,
            String value, boolean hookSetup) {
        SharedPreferences preferences = context.getSharedPreferences(preferencesName,
                Context.MODE_PRIVATE);
        Map<String, Long> timestamps = new HashMap<>();
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            long timestamp = Long.MIN_VALUE;
            if (entry.getValue() instanceof String) {
                try {
                    timestamp = hookSetup
                            ? HookSetupReport.decode(entry.getKey(), (String) entry.getValue())
                                    .timestampMillis
                            : TrustReport.decode(entry.getKey(), (String) entry.getValue())
                                    .timestampMillis;
                } catch (IllegalArgumentException ignored) {
                    // Malformed entries are first in line for eviction.
                }
            }
            timestamps.put(entry.getKey(), timestamp);
        }

        SharedPreferences.Editor editor = preferences.edit();
        String evictionKey;
        while ((evictionKey = StatusReportRetention.evictionCandidate(timestamps, key,
                StatusReportRetention.MAX_REPORTS)) != null) {
            editor.remove(evictionKey);
            timestamps.remove(evictionKey);
        }
        editor.putString(key, value).apply();
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
