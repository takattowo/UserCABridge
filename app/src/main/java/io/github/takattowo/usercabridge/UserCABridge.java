package io.github.takattowo.usercabridge;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public final class UserCABridge extends XposedModule {
    private static final String TAG = "UserCA Bridge";
    private static final String PREFIX = "android.security.net.config.";
    private volatile Context processContext;
    private String processName;

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        processName = param.getProcessName();
        log(Log.INFO, TAG, "Process=" + processName + " Android API="
                + Build.VERSION.SDK_INT + " Xposed API=" + getApiVersion());
        try {
            hookApplicationAttach();
            FrameworkTrust trust = FrameworkTrust.resolve(
                    frameworkClass("NetworkSecurityConfig"),
                    frameworkClass("NetworkSecurityConfig$Builder"),
                    frameworkClass("UserCertificateSource"),
                    frameworkClass("CertificateSource"),
                    frameworkClass("CertificatesEntryRef"), Build.VERSION.SDK_INT);
            log(Log.INFO, TAG, "Runtime reference=" + trust.getReferenceConstructor());
            int installed = 0;
            for (Method factory : trust.getFactoryMethods()) {
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
            log(installed == 0 ? Log.WARN : Log.INFO, TAG, "Framework hooks installed=" + installed);
            if (installed > 0) {
                deoptimizeCallers();
            }
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Unsupported framework; trust unchanged", error);
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
                }
                return result;
            });
        } catch (Throwable error) {
            log(Log.WARN, TAG, "Cannot hook Application.attach; trust reports unavailable", error);
        }
    }

    private void reportTrustStatus(int userCertificateCount) {
        Context context = processContext;
        if (context == null) {
            log(Log.WARN, TAG, "Application context unavailable; trust report not sent");
            return;
        }
        try {
            StatusReports.report(context, processName, userCertificateCount);
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
