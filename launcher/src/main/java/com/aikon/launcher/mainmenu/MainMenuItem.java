package com.aikon.launcher.mainmenu;

import androidx.annotation.DrawableRes;

public class MainMenuItem {

    public enum Type { APP, LIST, DEFAULT_APP, LINK }
    public enum ListTarget { APPS, MULTIMEDIA, GAMES }
    public enum DefaultAppKind { PHONE, CONTACTS, SMS, BROWSER }
    public enum LinkTarget { FDROID }

    private final String title;
    @DrawableRes private final int iconRes;
    private final Type type;
    private final String packageName;
    private final ListTarget listTarget;
    private final DefaultAppKind defaultAppKind;
    private final LinkTarget linkTarget;

    private MainMenuItem(String title, int iconRes, Type type, String packageName,
                         ListTarget listTarget, DefaultAppKind defaultAppKind, LinkTarget linkTarget) {
        this.title = title;
        this.iconRes = iconRes;
        this.type = type;
        this.packageName = packageName;
        this.listTarget = listTarget;
        this.defaultAppKind = defaultAppKind;
        this.linkTarget = linkTarget;
    }

    public static MainMenuItem app(String title, int iconRes, String packageName) {
        return new MainMenuItem(title, iconRes, Type.APP, packageName, null, null, null);
    }

    public static MainMenuItem list(String title, int iconRes, ListTarget target) {
        return new MainMenuItem(title, iconRes, Type.LIST, null, target, null, null);
    }

    public static MainMenuItem defaultApp(String title, int iconRes, DefaultAppKind kind) {
        return new MainMenuItem(title, iconRes, Type.DEFAULT_APP, null, null, kind, null);
    }

    public static MainMenuItem link(String title, int iconRes, LinkTarget target) {
        return new MainMenuItem(title, iconRes, Type.LINK, null, null, null, target);
    }

    public String getTitle() { return title; }
    public int getIconRes() { return iconRes; }
    public Type getType() { return type; }
    public String getPackageName() { return packageName; }
    public ListTarget getListTarget() { return listTarget; }
    public DefaultAppKind getDefaultAppKind() { return defaultAppKind; }
    public LinkTarget getLinkTarget() { return linkTarget; }
}
