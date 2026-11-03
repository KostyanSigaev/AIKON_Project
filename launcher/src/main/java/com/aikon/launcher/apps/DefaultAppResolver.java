package com.aikon.launcher.apps;

import android.app.role.RoleManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telecom.TelecomManager;

import androidx.annotation.Nullable;

import java.util.List;

/**
 * Резолвинг "приложения по умолчанию" для системных ролей (телефон, SMS)
 * и для действий, где у системы просто есть предпочитаемый обработчик
 * (контакты, браузер). Резолвится заново при каждом клике — так пункт
 * меню всегда ведёт на актуальное приложение по умолчанию, даже если
 * пользователь сменит его в системных настройках.
 */
public final class DefaultAppResolver {
    private DefaultAppResolver() {}

    @Nullable
    public static Intent resolvePhone(Context context) {
        PackageManager pm = context.getPackageManager();
        String defaultDialer = null;

        if (Build.VERSION.SDK_INT >= 23) {
            TelecomManager tm = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
            if (tm != null) defaultDialer = tm.getDefaultDialerPackage();
        }
        if (defaultDialer != null) {
            Intent launch = pm.getLaunchIntentForPackage(defaultDialer);
            if (launch != null) return launch;
        }

        // Запасной путь — обработчик ACTION_DIAL, выбранный системой
        Intent probe = new Intent(Intent.ACTION_DIAL);
        ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null && info.activityInfo != null) {
            Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
            if (launch != null) return launch;
        }
        return info != null ? probe : null;
    }

    @Nullable
    public static Intent resolveContacts(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
        ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) return null;
        Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
        return launch != null ? launch : null;
    }

    @Nullable
    public static Intent resolveSms(Context context) {
        PackageManager pm = context.getPackageManager();
        String defaultSms = Telephony.Sms.getDefaultSmsPackage(context);
        if (defaultSms != null) {
            Intent launch = pm.getLaunchIntentForPackage(defaultSms);
            if (launch != null) return launch;
        }

        if (Build.VERSION.SDK_INT >= 33) {
            Intent probe = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING);
            ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
            if (info != null && info.activityInfo != null) {
                Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
                if (launch != null) return launch;
            }
        }
        return null;
    }

    @Nullable
    public static Intent resolveBrowser(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        ResolveInfo info = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if (info == null || info.activityInfo == null) return null;
        Intent launch = pm.getLaunchIntentForPackage(info.activityInfo.packageName);
        return launch != null ? launch : probe;
    }
}
