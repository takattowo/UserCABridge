package io.github.takattowo.usercabridge;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;

public final class TrustReportProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return getContext() != null;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (extras == null) {
            return null;
        }
        Context context = getContext();
        if (context == null) {
            return null;
        }
        try {
            if (StatusReports.METHOD_REPORT.equals(method)) {
                if (!extras.containsKey(StatusReports.EXTRA_PROCESS_NAME)
                        || !extras.containsKey(StatusReports.EXTRA_USER_SOURCE_PRESENT)
                        || !extras.containsKey(StatusReports.EXTRA_USER_CERTIFICATE_COUNT)) {
                    return null;
                }
                TrustReport report = new TrustReport(Binder.getCallingUid(), Binder.getCallingPid(),
                        extras.getString(StatusReports.EXTRA_PROCESS_NAME),
                        extras.getBoolean(StatusReports.EXTRA_USER_SOURCE_PRESENT),
                        extras.getInt(StatusReports.EXTRA_USER_CERTIFICATE_COUNT),
                        System.currentTimeMillis());
                StatusReports.store(context, report);
            } else if (StatusReports.METHOD_REPORT_HOOK_SETUP.equals(method)) {
                if (!extras.containsKey(StatusReports.EXTRA_PROCESS_NAME)
                        || !extras.containsKey(StatusReports.EXTRA_FRAMEWORK_SUPPORTED)
                        || !extras.containsKey(StatusReports.EXTRA_SUPPORTED_FACTORY_COUNT)
                        || !extras.containsKey(StatusReports.EXTRA_INSTALLED_FACTORY_COUNT)) {
                    return null;
                }
                HookSetupReport report = new HookSetupReport(Binder.getCallingUid(),
                        Binder.getCallingPid(), extras.getString(StatusReports.EXTRA_PROCESS_NAME),
                        extras.getBoolean(StatusReports.EXTRA_FRAMEWORK_SUPPORTED),
                        extras.getInt(StatusReports.EXTRA_SUPPORTED_FACTORY_COUNT),
                        extras.getInt(StatusReports.EXTRA_INSTALLED_FACTORY_COUNT),
                        System.currentTimeMillis());
                StatusReports.store(context, report);
            } else {
                return null;
            }
            Bundle result = new Bundle();
            result.putBoolean(StatusReports.RESULT_STORED, true);
            return result;
        } catch (IllegalArgumentException | ClassCastException error) {
            return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
