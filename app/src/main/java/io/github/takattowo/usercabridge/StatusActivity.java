package io.github.takattowo.usercabridge;

import android.app.Activity;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.libxposed.service.HookedTarget;
import io.github.libxposed.service.XposedService;

public final class StatusActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView statusText;
    private Button refreshButton;
    private boolean refreshing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, padding, padding, padding);

        TextView title = new TextView(this);
        title.setText(R.string.status_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        content.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        statusText = new TextView(this);
        statusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusParams.topMargin = padding;
        content.addView(statusText, statusParams);

        refreshButton = new Button(this);
        refreshButton.setText(R.string.status_refresh);
        refreshButton.setOnClickListener(view -> refresh());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = padding / 2;
        content.addView(refreshButton, buttonParams);

        TextView note = new TextView(this);
        note.setText(R.string.status_scope_note);
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        noteParams.topMargin = padding;
        content.addView(note, noteParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        int paddingLeft = scroll.getPaddingLeft();
        int paddingTop = scroll.getPaddingTop();
        int paddingRight = scroll.getPaddingRight();
        int paddingBottom = scroll.getPaddingBottom();
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            int insetLeft;
            int insetTop;
            int insetRight;
            int insetBottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                insetLeft = bars.left;
                insetTop = bars.top;
                insetRight = bars.right;
                insetBottom = bars.bottom;
            } else {
                insetLeft = insets.getSystemWindowInsetLeft();
                insetTop = insets.getSystemWindowInsetTop();
                insetRight = insets.getSystemWindowInsetRight();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            StatusContentInsets.Padding contentPadding = StatusContentInsets.add(paddingLeft, paddingTop,
                    paddingRight, paddingBottom, insetLeft, insetTop, insetRight, insetBottom);
            view.setPadding(contentPadding.left, contentPadding.top, contentPadding.right,
                    contentPadding.bottom);
            return insets;
        });
        scroll.addView(content);
        setContentView(scroll);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }

    private void refresh() {
        if (refreshing) {
            return;
        }
        refreshing = true;
        refreshButton.setEnabled(false);
        statusText.setText(R.string.status_refreshing);
        worker.execute(() -> {
            String status = collectStatus();
            mainHandler.post(() -> {
                if (!isFinishing() && !isDestroyed()) {
                    statusText.setText(status);
                    refreshButton.setEnabled(true);
                    refreshing = false;
                }
            });
        });
    }

    private String collectStatus() {
        StringBuilder text = new StringBuilder();
        try {
            text.append(getString(R.string.status_user_ca_count, countUserCAs())).append('\n');
        } catch (Exception error) {
            text.append(getString(R.string.status_user_ca_count_unavailable)).append('\n');
        }

        List<XposedService> services = ((UserCABridgeApp) getApplication()).getServices();
        List<HookedTarget> targets = new ArrayList<>();
        boolean api102Available = false;
        boolean targetListAvailable = false;
        if (services.isEmpty()) {
            text.append("Framework service: unavailable\n");
        }
        for (XposedService service : services) {
            try {
                int api = service.getApiVersion();
                text.append("Framework: ").append(service.getFrameworkName()).append(' ')
                        .append(service.getFrameworkVersion()).append(" (API ").append(api)
                        .append(")\n");
                if (api >= XposedService.API_102) {
                    api102Available = true;
                    try {
                        targets.addAll(service.getRunningTargets());
                        targetListAvailable = true;
                    } catch (RuntimeException error) {
                        text.append("Running target query unavailable.\n");
                    }
                }
            } catch (RuntimeException error) {
                text.append("Framework service unavailable during refresh.\n");
            }
        }

        List<TrustReport> reports;
        boolean reportsAvailable = true;
        try {
            reports = StatusReports.load(this);
        } catch (RuntimeException error) {
            text.append("Stored trust reports unavailable; no success inferred.\n");
            reports = new ArrayList<>();
            reportsAvailable = false;
        }
        Map<String, TrustReport> reportsByProcess = new HashMap<>();
        for (TrustReport report : reports) {
            reportsByProcess.put(report.storageKey(), report);
        }
        List<HookSetupReport> setupReports;
        try {
            setupReports = StatusReports.loadHookSetup(this);
        } catch (RuntimeException error) {
            text.append("Stored hook setup reports unavailable; setup is unconfirmed.\n");
            setupReports = new ArrayList<>();
        }
        Map<String, HookSetupReport> setupReportsByProcess = new HashMap<>();
        for (HookSetupReport report : setupReports) {
            setupReportsByProcess.put(report.storageKey(), report);
        }
        if (!targetListAvailable) {
            if (!services.isEmpty()) {
                text.append(api102Available
                        ? "Running targets could not be verified; reports are unconfirmed.\n"
                        : "Service API 102 is required to verify running targets.\n");
            }
            for (HookSetupReport report : setupReports) {
                appendHookSetupReport(text, "Unverified hook setup report", report);
            }
            for (TrustReport report : reports) {
                appendReport(text, "Unverified trust report", report);
            }
            if (reports.isEmpty() && reportsAvailable) {
                text.append("No default-trust factory observation has been reported.\n");
            }
            return text.toString();
        }

        Set<String> currentProcesses = new HashSet<>();
        if (targets.isEmpty()) {
            text.append("No module-loaded target processes currently running.\n");
        }
        for (HookedTarget target : targets) {
            String identity = processIdentity(target.getUid(), target.getPid(),
                    target.getProcessName());
            if (!currentProcesses.add(identity)) {
                continue;
            }
            text.append('\n').append(target.getProcessName()).append(" (UID ")
                    .append(target.getUid()).append(", PID ").append(target.getPid())
                    .append(")\nModule state: ").append(target.getState()).append('\n');

            HookSetupReport setupReport = setupReportsByProcess.get(
                    target.getUid() + "|" + target.getProcessName());
            if (setupReport != null && !setupReport.matchesProcess(target.getUid(),
                    target.getPid(), target.getProcessName())) {
                setupReport = null;
            }
            ProcessHookSetupStatus.State setupState = ProcessHookSetupStatus.evaluate(
                    target.getUid(), target.getPid(), target.getProcessName(),
                    target.getState() == HookedTarget.State.UP_TO_DATE, setupReport);
            text.append(describeHookSetup(setupState, setupReport)).append('\n');
            if (setupReport != null) {
                appendHookSetupReport(text, "Last hook setup report", setupReport);
            }

            TrustReport report = reportsByProcess.get(
                    target.getUid() + "|" + target.getProcessName());
            if (report != null && !report.matchesProcess(target.getUid(), target.getPid(),
                    target.getProcessName())) {
                report = null;
            }
            ProcessTrustStatus.State state = ProcessTrustStatus.evaluate(target.getUid(),
                    target.getPid(), target.getProcessName(),
                    target.getState() == HookedTarget.State.UP_TO_DATE, report);
            text.append("Trust observation: ").append(describe(state, report)).append('\n');
            if (report != null) {
                appendReport(text, "Last report", report);
            }
        }

        for (HookSetupReport report : setupReports) {
            if (!currentProcesses.contains(processIdentity(report.uid, report.pid,
                    report.processName))) {
                appendHookSetupReport(text,
                        "Historical hook setup report (no matching hooked process)", report);
            }
        }
        for (TrustReport report : reports) {
            if (!currentProcesses.contains(processIdentity(report.uid, report.pid,
                    report.processName))) {
                appendReport(text, "Historical report (no matching hooked process)", report);
            }
        }
        if (targets.isEmpty() && reports.isEmpty() && reportsAvailable) {
            text.append("No default-trust factory observation has been reported.\n");
        }
        return text.toString();
    }

    private int countUserCAs() throws Exception {
        java.security.KeyStore store = java.security.KeyStore.getInstance("AndroidCAStore");
        store.load(null);
        int count = 0;
        Enumeration<String> aliases = store.aliases();
        while (aliases.hasMoreElements()) {
            if (aliases.nextElement().startsWith("user:")) {
                count++;
            }
        }
        return count;
    }

    private static String describe(ProcessTrustStatus.State state, TrustReport report) {
        switch (state) {
            case STALE_TARGET:
                return "Target is not UP_TO_DATE; current trust status is unconfirmed.";
            case WAITING_FOR_REPORT:
                return "Waiting for a matching trust-hook report; no success inferred.";
            case USER_SOURCE_MISSING:
                return "Hook report says user CA source is missing; inclusion is unconfirmed.";
            case NO_USER_CAS:
                return "Hook ran; default trust config includes user source, but no user CAs are installed.";
            case USER_CA_READY:
                return "Hook ran; default trust config includes user source; "
                        + report.userCertificateCount + " user CAs installed.";
            default:
                throw new AssertionError(state);
        }
    }

    private static String describeHookSetup(ProcessHookSetupStatus.State state,
            HookSetupReport report) {
        switch (state) {
            case STALE_TARGET:
                return "Hook setup: target is not UP_TO_DATE; report is unconfirmed.";
            case WAITING_FOR_SETUP_REPORT:
                return "Hook setup: waiting for per-process report; installation unconfirmed.";
            case FRAMEWORK_UNSUPPORTED:
                return "Hook setup: could not resolve supported default-trust factories.";
            case NO_HOOKS_INSTALLED:
            case PARTIAL_HOOKS_INSTALLED:
            case HOOKS_INSTALLED:
                return "Hook setup: " + report.installedFactoryCount + " of "
                        + report.supportedFactoryCount
                        + " supported default-trust factory hooks installed.";
            default:
                throw new AssertionError(state);
        }
    }

    private static void appendHookSetupReport(StringBuilder text, String label,
            HookSetupReport report) {
        text.append(label).append(": ").append(report.processName).append(" (UID ")
                .append(report.uid).append(", PID ").append(report.pid).append(") — ");
        if (report.frameworkSupported) {
            text.append(report.installedFactoryCount).append('/')
                    .append(report.supportedFactoryCount).append(" hooks installed");
        } else {
            text.append("factory resolution unavailable");
        }
        text.append(", ")
                .append(DateFormat.getDateTimeInstance().format(new Date(report.timestampMillis)))
                .append('\n');
    }

    private static void appendReport(StringBuilder text, String label, TrustReport report) {
        text.append(label).append(": ").append(report.processName).append(" (UID ")
                .append(report.uid).append(", PID ").append(report.pid).append(") — ")
                .append(report.userSourcePresent ? "user source present" : "user source missing")
                .append(", ").append(report.userCertificateCount).append(" user CAs, ")
                .append(DateFormat.getDateTimeInstance().format(new Date(report.timestampMillis)))
                .append('\n');
    }

    private static String processIdentity(int uid, int pid, String processName) {
        return uid + "|" + pid + "|" + processName;
    }
}
