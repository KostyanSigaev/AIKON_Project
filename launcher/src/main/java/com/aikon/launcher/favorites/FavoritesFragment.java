package com.aikon.launcher.favorites;

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
import com.aikon.launcher.apps.AppContextMenuBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Список избранных приложений: полные элементы, добавление/удаление через контекстное меню. */
public class FavoritesFragment extends Fragment {

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
        refresh(true);
    }

    /** showOverlay=false — после удаления из избранного прямо на этом экране операция мгновенная. */
    private void refresh(boolean showOverlay) {
        if (showOverlay) AikonLoadingOverlay.show();
        Context appContext = requireContext().getApplicationContext();
        loader.execute(() -> buildItems(appContext, showOverlay));
    }

    private void buildItems(Context appContext, boolean hideOverlayAfter) {
        PackageManager pm = appContext.getPackageManager();
        FavoritesRepository repo = FavoritesRepository.getInstance(appContext);

        List<AikonListItem<String>> items = new ArrayList<>();
        for (String packageName : repo.getAll()) {
            if (pm.getLaunchIntentForPackage(packageName) == null) continue; // удалено или не приложение — пропускаем

            try {
                ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
                items.add(AikonListItem.full(
                        pm.getApplicationLabel(info).toString(),
                        packageName,
                        AikonIconRegistry.resolve(appContext, packageName, pm, info),
                        packageName));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        mainHandler.post(() -> {
            if (isAdded() && listView != null) {
                listView.setAdapter(new AikonListAdapter<>(items, this::onItemClick, this::onItemLongClick));
            }
            if (hideOverlayAfter) AikonLoadingOverlay.hide();
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
        AppContextMenuBuilder.showFavoritesMenu(this, listView, item.getPayload(),
                () -> onItemClick(item, position),
                () -> refresh(false));
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
