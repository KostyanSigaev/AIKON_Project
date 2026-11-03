package com.aikon.launcher.apps;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class PackageChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri data = intent.getData();
        if (action == null || data == null) return;

        String packageName = data.getSchemeSpecificPart();
        Context appContext = context.getApplicationContext();
        AppIndexRepository repo = AppIndexRepository.getInstance(appContext);

        // goAsync() даёт время фоновой задаче завершиться, даже если система
        // готова прибить процесс сразу после выхода из onReceive.
        PendingResult pendingResult = goAsync();
        Runnable finish = pendingResult::finish;

        switch (action) {
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REPLACED:
                repo.addOrUpdatePackage(packageName, finish);
                break;
            case Intent.ACTION_PACKAGE_REMOVED:
                // EXTRA_REPLACING = true означает, что это часть обновления
                // приложения (следом придёт REPLACED) — тогда не удаляем.
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                    finish.run();
                } else {
                    repo.removePackage(packageName, finish);
                }
                break;
            default:
                finish.run();
        }
    }
}
