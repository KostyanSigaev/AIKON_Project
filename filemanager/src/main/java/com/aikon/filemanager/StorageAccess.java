package com.aikon.filemanager;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/** Доступ к файловой системе: Android 11+ — "все файлы", Android 10 и ниже — обычные разрешения. */
final class StorageAccess {
    private StorageAccess() {}

    static boolean hasAccess(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            return Environment.isExternalStorageManager();
        }
        return granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                && granted(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    static void request(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                activity.startActivity(new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (ActivityNotFoundException e) {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
            }
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
        }
    }

    private static boolean granted(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
}
