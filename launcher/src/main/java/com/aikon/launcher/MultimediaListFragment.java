package com.aikon.launcher;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aikon.core.ui.AikonIconRegistry;
import com.aikon.core.ui.list.AikonListAdapter;
import com.aikon.core.ui.list.AikonListItem;
import com.aikon.core.ui.list.AikonListView;
import com.aikon.launcher.apps.AppCategoryFlags;
import com.aikon.launcher.apps.AppContextMenuBuilder;
import com.aikon.launcher.apps.AppIndexRepository;
import com.aikon.launcher.apps.CachedApp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Список мультимедиа-приложений — читает готовые флаги категорий из кэша,
 * без единого вызова queryIntentActivities на этом экране: вся тяжёлая
 * работа уже сделана заранее при сканировании.
 */
public class MultimediaListFragment extends Fragment {

    private static final int CATEGORY_MASK = AppCategoryFlags.CAMERA | AppCategoryFlags.GALLERY
            | AppCategoryFlags.MUSIC | AppCategoryFlags.RECORDER;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
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
        AppIndexRepository repo = AppIndexRepository.getInstance(appContext);
        repo.ensureLoaded(() -> loadItems(appContext, repo));
    }

    /*private void loadItems(Context appContext, AppIndexRepository repo) {
        AikonLoadingOverlay.show();
        loader.execute(() -> {
            // Каждый CachedApp уникален по пакету, поэтому дублей быть
            // не может, даже если пакет подходит сразу под несколько категорий.
            List<CachedApp> matched = repo.getByCategory(CATEGORY_MASK);

            PackageManager pm = appContext.getPackageManager();
            List<AikonListItem<Intent>> items = new ArrayList<>(matched.size());
            for (CachedApp app : matched) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(app.getPackageName(), 0);
                    items.add(AikonListItem.full(
                            pm.getApplicationLabel(info).toString(),
                            app.getPackageName(),
                            pm.getApplicationIcon(info),
                            app.createLaunchIntent()));
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            Collections.sort(items, Comparator.comparing(AikonListItem::getTitle, String.CASE_INSENSITIVE_ORDER));

            mainHandler.post(() -> {
                if (!isAdded() || listView == null) return;
                listView.setAdapter(new AikonListAdapter<>(items, (item, position) -> {
                    try {
                        startActivity(item.getPayload());
                    } catch (Exception e) {
                        // TODO: показать сообщение об ошибке запуска
                    }
                }));
                AikonLoadingOverlay.hide();
            });
        });
    }*/
    private void loadItems(Context appContext, AppIndexRepository repo) {
        loader.execute(() -> {
            List<CachedApp> matched = repo.getByCategory(CATEGORY_MASK);

            PackageManager pm = appContext.getPackageManager();
            List<AikonListItem<Intent>> items = new ArrayList<>(matched.size());
            for (CachedApp app : matched) {
                try {
                    ApplicationInfo info = pm.getApplicationInfo(app.getPackageName(), 0);
                    items.add(AikonListItem.full(
                            pm.getApplicationLabel(info).toString(),
                            app.getPackageName(),
                            AikonIconRegistry.resolve(appContext, app.getPackageName(), pm, info),
                            app.createLaunchIntent()));
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
            Collections.sort(items, Comparator.comparing(AikonListItem::getTitle, String.CASE_INSENSITIVE_ORDER));

            mainHandler.post(() -> {
                if (!isAdded() || listView == null) return;
                listView.setAdapter(new AikonListAdapter<>(items, this::onItemClick, this::onItemLongClick));
            });
        });
    }

    private void onItemClick(AikonListItem<Intent> item, int position) {
        try {
            startActivity(item.getPayload());
        } catch (Exception e) {
            // TODO: показать сообщение об ошибке запуска
        }
    }

    private void onItemLongClick(AikonListItem<Intent> item, int position) {
        // Для контекстного меню нужно имя пакета, а не Intent — берём его из компонента
        String packageName = item.getPayload().getComponent().getPackageName();
        AppContextMenuBuilder.showAppManagementMenu(this, listView, packageName,
                () -> onItemClick(item, position));
    }




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
