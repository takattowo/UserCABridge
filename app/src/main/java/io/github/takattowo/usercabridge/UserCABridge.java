package io.github.takattowo.usercabridge;

import android.app.Application;
import android.content.Context;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import io.github.libxposed.api.XposedModule;

public final class UserCABridge extends XposedModule {
    private static final String TAG = "UserCA Bridge";
    private static final String PREFIX = "android.security.net.config.";
    private static final String PROCESS_SESSION_ID = UUID.randomUUID().toString();
    private volatile Context processContext;
    private volatile String processName;
    private final IBinder processToken = new Binder();
    private volatile boolean hookSetupComplete;
    private boolean frameworkSupported;
    private int supportedFactoryCount;
    private int installedFactoryCount;
    private boolean hookSetupReportSent;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        log(Log.INFO, TAG, "Process=" + processName + " Android API="
                + Build.VERSION.SDK_INT + " Xposed API=" + getApiVersion());
        boolean setupResolved = false;
        int supported = 0;
        int installed = 0;
        try {
            hookApplicationAttach();
            FrameworkTrust trust = FrameworkTrust.resolve(
                    frameworkClass("NetworkSecurityConfig"),
                    frameworkClass("NetworkSecurityConfig$Builder"),
                    frameworkClass("UserCertificateSource"),
                    frameworkClass("CertificateSource"),
                    frameworkClass("CertificatesEntryRef"), Build.VERSION.SDK_INT);
            List<Method> factories = trust.getFactoryMethods();
            supported = factories.size();
            setupResolved = true;
            log(Log.INFO, TAG, "Runtime reference=" + trust.getReferenceConstructor());
            for (Method factory : factories) {
                log(Log.INFO, TAG, "Runtime factory=" + factory);
                try {
                    hook(factory).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                        // Never catch or convert failures from Android's original factory.
                        Object builder = chain.proceed();
                        try {
                            boolean added = trust.addUserSource(builder);
                            log(Log.INFO, TAG, "Default trust executed; user source="
                                    + (added ? "appended" : "already present"));
                            try {
                                int count = trust.getUserCertificateCount();
                                log(Log.INFO, TAG, "User certificates loaded=" + count);
                                reportTrustStatus(count);
                            } catch (Throwable error) {
                                log(Log.WARN, TAG, "Cannot enumerate user certificates", error);
                            }
                        } catch (Throwable error) {
                            log(Log.WARN, TAG,
                                    "User CA extension failed; retaining original factory result", error);
                        }
                        return builder;
                    });
                    installed++;
                } catch (Throwable error) {
                    log(Log.WARN, TAG, "Cannot hook " + factory, error);
                }
            }
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Framework hook setup could not be resolved", error);
        }
        frameworkSupported = setupResolved;
        supportedFactoryCount = setupResolved ? supported : 0;
        installedFactoryCount = setupResolved ? installed : 0;
        hookSetupComplete = true;
        reportHookSetupIfReady();
        log(installed == 0 ? Log.WARN : Log.INFO, TAG, "Framework hooks installed=" + installed);
        if (installed > 0) {
            deoptimizeCallers();
        }
    }

    private void hookApplicationAttach() {
        try {
            Method attach = Application.class.getDeclaredMethod("attach", Context.class);
            hook(attach).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(chain -> {
                Object baseContext = chain.getArg(0);
                Object result = chain.proceed();
                if (baseContext instanceof Context) {
                    processContext = (Context) baseContext;
                    reportHookSetupIfReady();
                }
                return result;
            });
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Cannot hook Application.attach; trust reports unavailable", error);
        }
    }

    private synchronized void reportHookSetupIfReady() {
        Context context = processContext;
        if (!hookSetupComplete || context == null || hookSetupReportSent) {
            return;
        }
        try {
            StatusReports.reportHookSetup(context, processName, frameworkSupported,
                    supportedFactoryCount, installedFactoryCount, PROCESS_SESSION_ID,
                    processToken);
            hookSetupReportSent = true;
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Cannot send hook setup report", error);
        }
    }

    private void reportTrustStatus(int userCertificateCount) {
        Context context = processContext;
        if (context == null) {
            log(Log.WARN, TAG, "Application context unavailable; trust report not sent");
            return;
        }
        reportHookSetupIfReady();
        try {
            StatusReports.report(context, processName, userCertificateCount, PROCESS_SESSION_ID,
                    processToken);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Cannot send trust report", error);
        }
    }

    private static Class<?> frameworkClass(String name) throws ClassNotFoundException {
        return Class.forName(PREFIX + name, false, null);
    }

    private void deoptimizeCallers() {
        // Source-verified callers only; never deoptimize the entire process.
        for (String name : new String[] {"ManifestConfigSource$DefaultConfigSource",
                "ManifestConfigSource", "XmlConfigSource"}) {
            try {
                Class<?> caller = frameworkClass(name);
                if (name.endsWith("$DefaultConfigSource")) {
                    for (Constructor<?> constructor : caller.getDeclaredConstructors()) {
                        tryDeoptimize(constructor);
                    }
                } else {
                    String methodName = name.equals("XmlConfigSource")
                            ? "parseNetworkSecurityConfig" : "getConfigSource";
                    for (Method method : caller.getDeclaredMethods()) {
                        if (method.getName().equals(methodName)) {
                            tryDeoptimize(method);
                        }
                    }
                }
            } catch (Throwable error) {
                log(Log.WARN, TAG, "Cannot inspect caller " + name, error);
            }
        }
    }

    private void tryDeoptimize(Executable caller) {
        try {
            boolean success = deoptimize(caller);
            log(success ? Log.INFO : Log.WARN, TAG,
                    (success ? "Deoptimized " : "Could not deoptimize ") + caller);
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Cannot deoptimize " + caller, error);
        }
    }
}
