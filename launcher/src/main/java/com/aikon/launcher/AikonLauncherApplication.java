package com.aikon.launcher;

import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.core.content.ContextCompat;

import com.aikon.core.ui.AikonScreenLock;
import com.aikon.launcher.apps.AppIndexRepository;
import com.aikon.launcher.apps.PackageChangeReceiver;

public class AikonLauncherApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AikonScreenLock.setAdminComponent(new ComponentName(this, LauncherDeviceAdminReceiver.class));
        registerPackageReceiver();
        AppIndexRepository.getInstance(this).ensureLoaded(null);
    }

    /** Приёмник живёт столько же, сколько процесс, поэтому отписка не нужна. */
    private void registerPackageReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        filter.addDataScheme("package");
        ContextCompat.registerReceiver(this, new PackageChangeReceiver(), filter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }
}
