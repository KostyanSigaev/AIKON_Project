package com.aikon.launcher.apps;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.aikon.core.ui.AikonContextMenu;
import com.aikon.launcher.R;
import com.aikon.launcher.favorites.FavoritesRepository;

/**
 * Общее меню управления приложением — переиспользуется в списке всех
 * приложений, мультимедиа и где угодно ещё, где элемент списка — это
 * реальное установленное приложение. Список избранных использует свой
 * вариант (showFavoritesMenu), т.к. там вместо "добавить" нужно "убрать".
 */
public final class AppContextMenuBuilder {

    private AppContextMenuBuilder() {}

    public static void showAppManagementMenu(Fragment fragment, View anchor, String packageName, Runnable onOpen) {
        Context context = fragment.requireContext();
        FavoritesRepository favorites = FavoritesRepository.getInstance(context);
        boolean isFavorite = favorites.isFavorite(packageName);

        AikonContextMenu menu = new AikonContextMenu(context);
        menu.addItem(context.getString(R.string.context_menu_open), onOpen);
        menu.addItem(
                context.getString(isFavorite
                        ? R.string.context_menu_remove_favorite
                        : R.string.context_menu_add_favorite),
                () -> {
                    if (isFavorite) favorites.remove(packageName);
                    else favorites.add(packageName);
                });
        menu.addItem(context.getString(R.string.context_menu_about), () -> openAppInfo(fragment, packageName));
        menu.addItem(context.getString(R.string.context_menu_clear_cache_open),
                () -> clearCacheThenOpen(fragment, packageName, onOpen));
        menu.addItem(context.getString(R.string.context_menu_uninstall),
                () -> requestUninstall(fragment, packageName));

        menu.show(anchor);
    }

    /**
     * Программной очистки кэша чужого пакета у обычных приложений нет (требует
     * системных прав уровня signature|privileged) — поэтому открываем системный
     * экран "О приложении", где пользователь сам нажимает "Очистить кэш", а при
     * возврате в AIKON приложение доткрывается автоматически через PendingLaunchHolder.
     */
    private static void clearCacheThenOpen(Fragment fragment, String packageName, Runnable onOpen) {
        PendingLaunchHolder.setPending(onOpen);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        fragment.startActivity(intent);
    }

    /** Запрос системного удаления — показывает стандартный диалог подтверждения Android. */
    private static void requestUninstall(Fragment fragment, String packageName) {
        Intent intent = new Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null));
        fragment.startActivity(intent);
    }

    public static void showFavoritesMenu(Fragment fragment, View anchor, String packageName,
                                         Runnable onOpen, Runnable onRemoved) {
        Context context = fragment.requireContext();
        FavoritesRepository favorites = FavoritesRepository.getInstance(context);

        AikonContextMenu menu = new AikonContextMenu(context);
        menu.addItem(context.getString(R.string.context_menu_open), onOpen);
        menu.addItem(context.getString(R.string.context_menu_remove_favorite), () -> {
            favorites.remove(packageName);
            onRemoved.run();
        });
        menu.addItem(context.getString(R.string.context_menu_about), () -> openAppInfo(fragment, packageName));

        menu.show(anchor);
    }

    private static void openAppInfo(Fragment fragment, String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        fragment.startActivity(intent);
    }
}
