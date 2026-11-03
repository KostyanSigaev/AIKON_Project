package com.aikon.filemanager;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Единая точка выбора иконки файла. Когда в core появятся готовые иконки —
 * достаточно заменить ресурсы в методах ниже, остальной код трогать не нужно.
 * Вызывать из фонового потока (для .apk читается манифест файла).
 */
final class FileIconResolver {

    private static final Set<String> ARCHIVES = new HashSet<>(Arrays.asList(
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "jar"));

    private FileIconResolver() {}

    static Drawable parentIcon(Context context) {
        return load(context, R.drawable.fm_ic_up);
    }

    static Drawable resolve(Context context, File file) {
        if (file.isDirectory()) return load(context, R.drawable.fm_ic_folder);

        String ext = FileIntents.extension(file.getName());
        if ("apk".equals(ext)) {
            Drawable icon = loadApkIcon(context, file);
            return icon != null ? icon : load(context, R.drawable.fm_ic_apk);
        }
        if (ARCHIVES.contains(ext)) return load(context, R.drawable.fm_ic_archive);

        String mime = FileIntents.mimeOf(file);
        if (mime.startsWith("image/")) return load(context, R.drawable.fm_ic_image);
        if (mime.startsWith("audio/")) return load(context, R.drawable.fm_ic_audio);
        if (mime.startsWith("video/")) return load(context, R.drawable.fm_ic_video);
        return load(context, R.drawable.fm_ic_file);
    }

    @Nullable
    private static Drawable loadApkIcon(Context context, File apk) {
        PackageManager pm = context.getPackageManager();
        String path = apk.getAbsolutePath();
        PackageInfo info = pm.getPackageArchiveInfo(path, 0);
        if (info == null || info.applicationInfo == null) return null;

        ApplicationInfo appInfo = info.applicationInfo;
        appInfo.sourceDir = path;
        appInfo.publicSourceDir = path;
        return pm.getApplicationIcon(appInfo);
    }

    private static Drawable load(Context context, @DrawableRes int res) {
        return ContextCompat.getDrawable(context, res);
    }
}
