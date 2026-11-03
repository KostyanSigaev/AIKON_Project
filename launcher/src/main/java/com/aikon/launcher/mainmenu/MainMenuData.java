package com.aikon.launcher.mainmenu;

import java.util.Arrays;
import java.util.List;

public final class MainMenuData {
    private MainMenuData() {}

    public static List<MainMenuItem> items() {
        return Arrays.asList(
                MainMenuItem.defaultApp("Телефон", android.R.drawable.ic_menu_call, MainMenuItem.DefaultAppKind.PHONE),
                MainMenuItem.defaultApp("Контакты", android.R.drawable.ic_menu_myplaces, MainMenuItem.DefaultAppKind.CONTACTS),
                MainMenuItem.defaultApp("SMS", android.R.drawable.ic_dialog_email, MainMenuItem.DefaultAppKind.SMS),
                MainMenuItem.list("Игры", android.R.drawable.ic_menu_slideshow, MainMenuItem.ListTarget.GAMES),
                MainMenuItem.list("Приложения", android.R.drawable.ic_menu_manage, MainMenuItem.ListTarget.APPS),
                MainMenuItem.list("Мультимедиа", android.R.drawable.ic_menu_gallery, MainMenuItem.ListTarget.MULTIMEDIA),
                MainMenuItem.link("Магазин", android.R.drawable.ic_menu_upload, MainMenuItem.LinkTarget.FDROID),
                MainMenuItem.app("Настройки", android.R.drawable.ic_menu_preferences, "com.android.settings"),
                MainMenuItem.defaultApp("Браузер", android.R.drawable.ic_menu_compass, MainMenuItem.DefaultAppKind.BROWSER)
        );
    }
}
