package com.aikon.core.ui;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.aikon.core.R;

/**
 * Блокировка экрана через права администратора устройства (force-lock).
 * Ядро не объявляет компонент администратора само — его задаёт приложение
 * (лаунчер) через setAdminComponent().
 */
public final class AikonScreenLock {

    private static final String TAG = "AikonScreenLock";
    /** Временно true: причина каждого сбоя показывается Toast'ом. После отладки поставьте false (в логе останется). */
    private static final boolean DEBUG_TOASTS = true;

    private AikonScreenLock() {}

    @Nullable private static volatile ComponentName adminComponent;

    public static void setAdminComponent(@Nullable ComponentName component) {
        adminComponent = component;
    }

    public static void lock(Context context) {
        ComponentName admin = adminComponent;
        if (admin == null) {
            report(context, "Компонент администратора не задан: setAdminComponent() не вызывался");
            return;
        }

        String manifestProblem = checkReceiver(context, admin);
        if (manifestProblem != null) {
            report(context, manifestProblem);
            return;
        }

        DevicePolicyManager dpm = (DevicePolicyManager)
                context.getApplicationContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm == null) {
            report(context, "DevicePolicyManager недоступен на этом устройстве");
            return;
        }

        if (dpm.isAdminActive(admin)) {
            if (!dpm.hasGrantedPolicy(admin, DeviceAdminInfo.USES_POLICY_FORCE_LOCK)) {
                report(context, "Администратор включён без политики force-lock: отключите его в настройках и включите заново");
                return;
            }
            try {
                dpm.lockNow();
                Log.d(TAG, "lockNow() выполнен");
            } catch (SecurityException e) {
                report(context, "lockNow() отклонён: " + e);
            }
            return;
        }

        requestAdmin(context, admin);
    }

    /** Проверяет, что приёмник правильно объявлен в манифесте приложения. @return описание проблемы или null. */
    @Nullable
    private static String checkReceiver(Context context, ComponentName admin) {
        try {
            ActivityInfo info = context.getPackageManager()
                    .getReceiverInfo(admin, PackageManager.GET_META_DATA);
            if (!Manifest.permission.BIND_DEVICE_ADMIN.equals(info.permission)) {
                return "У приёмника нет android:permission=BIND_DEVICE_ADMIN";
            }
            if (!info.exported) {
                return "Приёмник должен быть android:exported=\"true\"";
            }
            if (info.metaData == null
                    || !info.metaData.containsKey(DeviceAdminReceiver.DEVICE_ADMIN_META_DATA)) {
                return "У приёмника нет meta-data android.app.device_admin";
            }
            return null;
        } catch (PackageManager.NameNotFoundException e) {
            return "Приёмник не найден в манифесте: " + admin.flattenToShortString();
        }
    }

    private static void requestAdmin(Context context, ComponentName admin) {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.aikon_lock_admin_explanation));
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        try {
            context.startActivity(intent);
            Log.d(TAG, "Запрос прав администратора отправлен для " + admin.flattenToShortString());
        } catch (ActivityNotFoundException | SecurityException e) {
            report(context, "Не удалось открыть запрос прав администратора: " + e);
        }
    }

    private static void report(Context context, String message) {
        Log.w(TAG, message);
        if (DEBUG_TOASTS) {
            Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }
    }
}
