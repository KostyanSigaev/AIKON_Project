package com.aikon.launcher.apps;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Персистентное хранилище кэша приложений. Использует встроенный в Android
 * org.json — без сторонних библиотек. Формат компактный: пакет, имя
 * activity и битовая маска категорий на каждое приложение.
 */
public class AppIndexStore {

    private static final String PREFS_NAME = "aikon_app_index";
    private static final String KEY_APPS = "apps";

    private final SharedPreferences prefs;

    public AppIndexStore(Context appContext) {
        prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public List<CachedApp> load() {
        String raw = prefs.getString(KEY_APPS, null);
        if (raw == null) return null;
        try {
            JSONArray array = new JSONArray(raw);
            List<CachedApp> result = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                result.add(new CachedApp(
                        obj.getString("p"),
                        obj.getString("c"),
                        obj.optInt("f", 0)));
            }
            return result;
        } catch (JSONException e) {
            return null; // повреждённый кэш — вызовет полное пересканирование
        }
    }

    public void save(List<CachedApp> apps) {
        JSONArray array = new JSONArray();
        try {
            for (CachedApp app : apps) {
                JSONObject obj = new JSONObject();
                obj.put("p", app.getPackageName());
                obj.put("c", app.getActivityName());
                obj.put("f", app.getCategoryFlags());
                array.put(obj);
            }
            prefs.edit().putString(KEY_APPS, array.toString()).apply();
        } catch (JSONException ignored) {
        }
    }
}
