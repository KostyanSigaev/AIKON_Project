package com.aikon.filemanager;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.Locale;

/** Открытие и отправка файлов через FileProvider (file:// URI на современном Android запрещены). */
final class FileIntents {
    private FileIntents() {}

    static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1)
                ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    static String mimeOf(File file) {
        String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension(file.getName()));
        return mime != null ? mime : "*/*";
    }

    /** forceChooser = true — всегда показывать системное меню выбора приложения. */
    static void view(Activity activity, File file, boolean forceChooser) {
        Uri uri = uriFor(activity, file);
        if (uri == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeOf(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        start(activity, forceChooser
                ? Intent.createChooser(intent, activity.getString(R.string.fm_chooser_open))
                : intent);
    }

    static void send(Activity activity, File file) {
        Uri uri = uriFor(activity, file);
        if (uri == null) return;

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeOf(file));
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        start(activity, Intent.createChooser(intent, activity.getString(R.string.fm_chooser_send)));
    }

    private static Uri uriFor(Activity activity, File file) {
        try {
            return FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", file);
        } catch (IllegalArgumentException e) {
            Toast.makeText(activity, R.string.fm_toast_no_app, Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private static void start(Activity activity, Intent intent) {
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.fm_toast_no_app, Toast.LENGTH_SHORT).show();
        }
    }
}
