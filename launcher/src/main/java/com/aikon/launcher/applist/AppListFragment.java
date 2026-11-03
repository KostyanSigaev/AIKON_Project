package com.aikon.launcher.applist;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aikon.core.ui.list.AikonListAdapter;
import com.aikon.core.ui.list.AikonListItem;
import com.aikon.core.ui.list.AikonListView;
import com.aikon.core.ui.AikonLoadingOverlay;
import com.aikon.launcher.R;
import com.aikon.launcher.apps.AppIndexRepository;
import com.aikon.launcher.apps.CachedApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.aikon.core.ui.AikonIconRegistry;
import com.aikon.launcher.apps.AppContextMenuBuilder;

/** Полный список всех приложений. Долгое нажатие открывает контекстное меню. */
public class AppListFragment extends Fragment {

    /** Payload элемента списка: и Intent запуска, и имя пакета для контекстного меню. */
    private static class AppEntry {
        final Intent launchIntent;
        final String packageName;
        AppEntry(Intent launchIntent, String packageName) {
            this.launchIntent = launchIntent;
            this.packageName = packageName;
        }
    }

    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private AikonListView listView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listView = view.findViewById(R.id.list_generic);

        Context appContext = requireContext().getApplicationContext();
        AikonLoadingOverlay.show();
        AppIndexRepository repo = AppIndexRepository.getInstance(appContext);
        repo.ensureLoaded(() -> loadIcons(appContext, repo.getAll()));
    }

    /*private void loadIcons(Context appContext, List<CachedApp> apps) {
        loader.execute(() -> {
            PackageManager pm = appContext.getPackageManager();
            List<AikonListItem<AppEntry>> items = new ArrayList<>(apps.size());

            for (CachedApp app : apps) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(app.getPackageName(), 0);
                    CharSequence label = pm.getApplicationLabel(info);
                    String versionName = pm.getPackageInfo(app.getPackageName(), 0).versionName;
                    AppEntry entry = new AppEntry(app.createLaunchIntent(), app.getPackageName());
                    items.add(AikonListItem.full(
                            label.toString(),
                            app.getPackageName() + " · v" + versionName,
                            pm.getApplicationIcon(info),
                            entry));
                } catch (PackageManager.NameNotFoundException e) {
                    // приложение удалили между кэшированием и открытием списка — пропускаем
                }
            }

            Collections.sort(items, Comparator.comparing(AikonListItem::getTitle, String.CASE_INSENSITIVE_ORDER));

            mainHandler.post(() -> {
                if (!isAdded() || listView == null) {
                    AikonLoadingOverlay.hide();
                    return;
                }
                listView.setAdapter(new AikonListAdapter<>(items, this::onItemClick, this::onItemLongClick));
                AikonLoadingOverlay.hide();
            });
        });
    }*/
    private void loadIcons(Context appContext, List<CachedApp> apps) {
        loader.execute(() -> {
            PackageManager pm = appContext.getPackageManager();
            List<AikonListItem<AppEntry>> items = new ArrayList<>(apps.size());

            for (CachedApp app : apps) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(app.getPackageName(), 0);
                    CharSequence label = pm.getApplicationLabel(info);
                    String versionName = pm.getPackageInfo(app.getPackageName(), 0).versionName;
                    AppEntry entry = new AppEntry(app.createLaunchIntent(), app.getPackageName());
                    items.add(AikonListItem.full(
                            label.toString(),
                            app.getPackageName() + " · v" + versionName,
                            AikonIconRegistry.resolve(appContext, app.getPackageName(), pm, info),
                            entry));
                } catch (PackageManager.NameNotFoundException e) {
                    // приложение удалили между кэшированием и открытием списка — пропускаем
                }
            }

            Collections.sort(items, Comparator.comparing(AikonListItem::getTitle, String.CASE_INSENSITIVE_ORDER));

            mainHandler.post(() -> {
                if (!isAdded() || listView == null) {
                    AikonLoadingOverlay.hide();
                    return;
                }
                listView.setAdapter(new AikonListAdapter<>(items, this::onItemClick, this::onItemLongClick));
                AikonLoadingOverlay.hide();
            });
        });
    }

    private void onItemLongClick(AikonListItem<AppEntry> item, int position) {
        AppContextMenuBuilder.showAppManagementMenu(this, listView, item.getPayload().packageName,
                () -> onItemClick(item, position));
    }

    private void onItemClick(AikonListItem<AppEntry> item, int position) {
        try {
            startActivity(item.getPayload().launchIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    /*private void onItemLongClick(AikonListItem<AppEntry> item, int position) {
        String packageName = item.getPayload().packageName;

        AikonContextMenu menu = new AikonContextMenu(requireContext());
        menu.addItem(getString(R.string.context_menu_open), () -> onItemClick(item, position));
        menu.addItem(getString(R.string.context_menu_add_favorite), null); // без логики пока
        menu.addItem(getString(R.string.context_menu_about), () -> openAppInfo(packageName));
        menu.addItem(getString(R.string.context_menu_clear_cache_open), null); // без логики пока
        menu.addItem(getString(R.string.context_menu_uninstall), null); // без логики пока

        menu.show(listView);
    }*/

    /*private void openAppInfo(String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null));
        startActivity(intent);
    }*/

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        listView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        loader.shutdownNow();
    }
}
