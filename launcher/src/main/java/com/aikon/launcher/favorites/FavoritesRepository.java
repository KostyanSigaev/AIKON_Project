package com.aikon.launcher.favorites;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Хранилище избранных приложений. Порядок добавления сохраняется
 * (LinkedHashSet), данные персистентны через SharedPreferences в виде JSON —
 * тот же лёгкий подход, что и в AppIndexStore, без сторонних библиотек.
 *
 * При самом первом запуске (когда пользователь ещё ничего не выбрал) список
 * заполняется тестовым набором — Настройки/Камера/Калькулятор — как и было
 * раньше; дальше пользователь управляет списком сам через контекстное меню.
 */
public class FavoritesRepository {

    private static volatile FavoritesRepository instance;

    public static FavoritesRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (FavoritesRepository.class) {
                if (instance == null) {
                    instance = new FavoritesRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    private static final String PREFS_NAME = "aikon_favorites";
    private static final String KEY_PACKAGES = "packages";
    private static final String KEY_SEEDED = "seeded";

    private final SharedPreferences prefs;
    private final LinkedHashSet<String> cache;

    private FavoritesRepository(Context appContext) {
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        cache = new LinkedHashSet<>(load());
        seedDefaultsIfNeeded(appContext);
    }

    public synchronized List<String> getAll() {
        return new ArrayList<>(cache);
    }

    public synchronized boolean isFavorite(String packageName) {
        return cache.contains(packageName);
    }

    public synchronized void add(String packageName) {
        if (cache.add(packageName)) persist();
    }

    public synchronized void remove(String packageName) {
        if (cache.remove(packageName)) persist();
    }

    private void seedDefaultsIfNeeded(Context appContext) {
        if (prefs.getBoolean(KEY_SEEDED, false)) return;

        PackageManager pm = appContext.getPackageManager();
        addIfResolvable(pm, new Intent(Settings.ACTION_SETTINGS));
        addIfResolvable(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        addIfResolvable(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR));

        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
        persist();
    }

    private void addIfResolvable(PackageManager pm, Intent intent) {
        ResolveInfo info = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null && info.activityInfo != null) {
            cache.add(info.activityInfo.packageName);
        }
    }

    private List<String> load() {
        String raw = prefs.getString(KEY_PACKAGES, null);
        List<String> result = new ArrayList<>();
        if (raw == null) return result;
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                result.add(array.getString(i));
            }
        } catch (JSONException ignored) {
        }
        return result;
    }

    private void persist() {
        JSONArray array = new JSONArray();
        for (String pkg : cache) array.put(pkg);
        prefs.edit().putString(KEY_PACKAGES, array.toString()).apply();
    }
}
