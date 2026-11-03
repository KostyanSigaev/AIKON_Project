package com.aikon.launcher.applist;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.aikon.core.ui.AikonIconRegistry;
import com.aikon.core.ui.list.AikonListAdapter;
import com.aikon.core.ui.list.AikonListItem;
import com.aikon.core.ui.list.AikonListView;
import com.aikon.core.ui.AikonLoadingOverlay;
import com.aikon.launcher.MainActivity;
import com.aikon.launcher.R;
import com.aikon.launcher.apps.AppIndexRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Список из главного меню: "Все приложения" (с счётчиком) + заданные вручную приложения. */
public class AppsQuickListFragment extends Fragment {

    private static class QuickApp {
        @Nullable final String category;
        final String[] packageCandidates;
        QuickApp(@Nullable String category, String... packageCandidates) {
            this.category = category;
            this.packageCandidates = packageCandidates;
        }
    }

    private static final QuickApp CALCULATOR = new QuickApp(
            Intent.CATEGORY_APP_CALCULATOR,
            "com.google.android.calculator", "com.android.calculator2",
            "com.sec.android.app.popupcalculator", "com.miui.calculator");

    private static final QuickApp CALENDAR = new QuickApp(
            Intent.CATEGORY_APP_CALENDAR,
            "com.google.android.calendar", "com.android.calendar", "com.samsung.android.calendar");

    private static final QuickApp NOTES = new QuickApp(
            null,
            "com.google.android.keep", "com.samsung.android.app.notes",
            "com.miui.notes", "com.color.notes");

    private static final QuickApp[] EXTRA_APPS = {CALCULATOR, CALENDAR, NOTES};

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
        // Счётчик "Все приложения" берётся из уже готового кэша — при первом
        // запуске лаунчера он уже прогрет в Application.onCreate(), поэтому
        // тут почти всегда мгновенный ответ без повторного сканирования.
        AppIndexRepository.getInstance(appContext).ensureLoaded(
                () -> loader.execute(() -> buildItems(appContext)));
    }

    private void buildItems(Context appContext) {
        PackageManager pm = appContext.getPackageManager();
        int launchableAppsCount = AppIndexRepository.getInstance(appContext).getAll().size();

        List<AikonListItem<Runnable>> items = new ArrayList<>();

        Drawable allAppsIcon = ContextCompat.getDrawable(appContext, android.R.drawable.ic_menu_manage);
        items.add(AikonListItem.full(
                getString(R.string.menu_all_apps),
                getString(R.string.all_apps_count_format, launchableAppsCount),
                allAppsIcon,
                (Runnable) this::openAllApps));

        for (QuickApp app : EXTRA_APPS) {
            String resolvedPackage = resolvePackage(pm, app);
            if (resolvedPackage == null) continue;

            Intent launchIntent = pm.getLaunchIntentForPackage(resolvedPackage);
            if (launchIntent == null) continue;

            try {
                ApplicationInfo info = pm.getApplicationInfo(resolvedPackage, 0);
                items.add(AikonListItem.full(
                        pm.getApplicationLabel(info).toString(),
                        resolvedPackage,
                        AikonIconRegistry.resolve(appContext, resolvedPackage, pm, info),
                        (Runnable) () -> startActivity(launchIntent)));
            } catch (PackageManager.NameNotFoundException ignored) {
            }
        }

        mainHandler.post(() -> {
            if (!isAdded() || listView == null) {
                AikonLoadingOverlay.hide();
                return;
            }
            listView.setAdapter(new AikonListAdapter<>(items, (item, position) -> item.getPayload().run()));
            AikonLoadingOverlay.hide();
        });
    }

    @Nullable
    private String resolvePackage(PackageManager pm, QuickApp app) {
        if (app.category != null) {
            Intent categoryIntent = new Intent(Intent.ACTION_MAIN).addCategory(app.category);
            ResolveInfo resolved = pm.resolveActivity(categoryIntent, PackageManager.MATCH_DEFAULT_ONLY);
            if (resolved != null && resolved.activityInfo != null) {
                return resolved.activityInfo.packageName;
            }
        }
        for (String pkg : app.packageCandidates) {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                return pkg;
            }
        }
        return null;
    }

    private void openAllApps() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openScreen(new AppListFragment(), "app_list");
        }
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
