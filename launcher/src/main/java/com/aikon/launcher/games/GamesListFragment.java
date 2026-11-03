package com.aikon.launcher.games;

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

import com.aikon.core.ui.AikonIconRegistry;
import com.aikon.core.ui.list.AikonListAdapter;
import com.aikon.core.ui.list.AikonListItem;
import com.aikon.core.ui.list.AikonListView;
import com.aikon.core.ui.AikonLoadingOverlay;
import com.aikon.launcher.R;
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

/** Список "Игры": установленные приложения с категорией "игра" (флаг GAME из кэша). */
public class GamesListFragment extends Fragment {

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
        AikonLoadingOverlay.show();
        AppIndexRepository repo = AppIndexRepository.getInstance(appContext);
        repo.ensureLoaded(() -> loader.execute(() -> buildItems(appContext, repo)));
    }

    private void buildItems(Context appContext, AppIndexRepository repo) {
        PackageManager pm = appContext.getPackageManager();
        List<AikonListItem<String>> items = new ArrayList<>();

        for (CachedApp app : repo.getByCategory(AppCategoryFlags.GAME)) {
            try {
                ApplicationInfo info = pm.getApplicationInfo(app.getPackageName(), 0);
                items.add(AikonListItem.full(
                        pm.getApplicationLabel(info).toString(),
                        app.getPackageName(),
                        AikonIconRegistry.resolve(appContext, app.getPackageName(), pm, info),
                        app.getPackageName()));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        Collections.sort(items, Comparator.comparing(AikonListItem::getTitle, String.CASE_INSENSITIVE_ORDER));

        mainHandler.post(() -> {
            if (isAdded() && listView != null) {
                listView.setAdapter(new AikonListAdapter<>(items, this::onItemClick, this::onItemLongClick));
            }
            AikonLoadingOverlay.hide();
        });
    }

    private void onItemClick(AikonListItem<String> item, int position) {
        Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(item.getPayload());
        if (intent != null) {
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void onItemLongClick(AikonListItem<String> item, int position) {
        AppContextMenuBuilder.showAppManagementMenu(this, listView, item.getPayload(),
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
