package io.github.takattowo.usercabridge;

import android.os.Build;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;

import io.github.libxposed.api.XposedModule;

public final class UserCABridge extends XposedModule {
    private static final String TAG = "UserCA Bridge";
    private static final String PREFIX = "android.security.net.config.";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        log(Log.INFO, TAG, "Process=" + param.getProcessName() + " Android API="
                + Build.VERSION.SDK_INT + " Xposed API=" + getApiVersion());
        try {
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
                                log(Log.INFO, TAG, "User certificates loaded="
                                        + trust.getUserCertificateCount());
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
