package io.github.takattowo.usercabridge;

import android.app.Application;

import java.util.ArrayList;
import java.util.List;

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
}
