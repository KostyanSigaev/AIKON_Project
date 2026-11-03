package com.aikon.core.ui.list;

import android.graphics.drawable.Drawable;

/**
 * Единая модель элемента списка для всего проекта.
 * SIMPLE — одна строка текста (используется почти везде: пункты меню, избранное).
 * FULL — заголовок + описание + квадратная иконка (списки приложений, контакты и т.п.).
 *
 * payload — произвольные данные, которые понадобятся при клике
 * (например, Intent для запуска приложения) — их тип задаёт вызывающий код.
 */
public class AikonListItem<T> {

    public enum Type { SIMPLE, FULL }

    private final Type type;
    private final String title;
    private final String description;
    private final Drawable icon;
    private final T payload;

    private AikonListItem(Type type, String title, String description, Drawable icon, T payload) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.payload = payload;
    }

    public static <T> AikonListItem<T> simple(String title, T payload) {
        return new AikonListItem<>(Type.SIMPLE, title, null, null, payload);
    }

    public static <T> AikonListItem<T> full(String title, String description, Drawable icon, T payload) {
        return new AikonListItem<>(Type.FULL, title, description, icon, payload);
    }

    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Drawable getIcon() { return icon; }
    public T getPayload() { return payload; }
}
