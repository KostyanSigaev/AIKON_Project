package com.aikon.core.ui;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Единая точка получения иконки для ЛЮБОГО списка во ВСЕХ приложениях проекта.
 * Порядок разрешения: 1) явно зарегистрированная иконка пакета,
 * 2) внешний AikonIconProvider (например, целый кастомный набор),
 * 3) системная иконка приложения как запасной вариант.
 *
 * Когда появится готовый набор иконок AIKON, достаточно один раз вызвать
 * registerIcon(...) для нужных пакетов (или setProvider на целый набор) —
 * списки во всех приложениях подхватят изменения сами, без правок кода.
 */
public final class AikonIconRegistry {

    private AikonIconRegistry() {}

    private static final Map<String, Integer> customIcons = new ConcurrentHashMap<>();
    @Nullable private static volatile AikonIconProvider provider;

    public static void registerIcon(String packageName, @DrawableRes int iconRes) {
        customIcons.put(packageName, iconRes);
    }

    public static void unregisterIcon(String packageName) {
        customIcons.remove(packageName);
    }

    public static void setProvider(@Nullable AikonIconProvider newProvider) {
        provider = newProvider;
    }

    public static Drawable resolve(Context context, String packageName,
                                   PackageManager pm, ApplicationInfo appInfo) {
        Integer customRes = customIcons.get(packageName);
        if (customRes != null) {
            Drawable d = ContextCompat.getDrawable(context, customRes);
            if (d != null) return d;
        }

        AikonIconProvider currentProvider = provider;
        if (currentProvider != null) {
            Drawable d = currentProvider.getIcon(context, packageName);
            if (d != null) return d;
        }

        return pm.getApplicationIcon(appInfo);
    }
}
