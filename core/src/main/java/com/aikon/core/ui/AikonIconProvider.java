package com.aikon.core.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

/**
 * Источник кастомных иконок (например, будущий набор в стиле Symbian).
 * Возвращает null, если для пакета своей иконки нет — тогда используется
 * системная иконка приложения как запасной вариант.
 */
public interface AikonIconProvider {
    @Nullable
    Drawable getIcon(Context context, String packageName);
}
