package com.aikon.launcher.apps;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единая точка доступа к кэшу приложений.
 * - Полное сканирование — только если кэша ещё нет.
 * - Пока процесс жив: точечные обновления по PACKAGE_ADDED/REMOVED/REPLACED.
 * - При каждом старте: дешёвая сверка кэша с системой (один запрос), потому что
 *   пока процесс не работал, события установки/удаления могли быть пропущены.
 * Все операции идут на одном потоке; чтение кэша — без блокировок.
 */
public class AppIndexRepository {

    private static volatile AppIndexRepository instance;

    public static AppIndexRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (AppIndexRepository.class) {
                if (instance == null) {
                    instance = new AppIndexRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    /** Если расхождений больше — дешевле просканировать всё заново, чем править по одному. */
    private static final int RECONCILE_INCREMENTAL_LIMIT = 8;

    private static final Set<String> RECORDER_FALLBACK_PACKAGES = new HashSet<>(Arrays.asList(
            "com.google.android.apps.recorder",
            "com.android.soundrecorder",
            "com.sec.android.app.voicenote"
    ));

    private final Context appContext;
    private final AppIndexStore store;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private volatile List<CachedApp> cache = Collections.emptyList();
    private volatile boolean loaded = false;

    private AppIndexRepository(Context appContext) {
        this.appContext = appContext;
        this.store = new AppIndexStore(appContext);
    }

    /** Загружает кэш (или строит с нуля). onReady вызывается на главном потоке. */
    public void ensureLoaded(@Nullable Runnable onReady) {
        if (loaded) {
            if (onReady != null) mainHandler.post(onReady);
            return;
        }
        executor.execute(() -> {
            if (loaded) { // параллельный вызов уже всё загрузил
                if (onReady != null) mainHandler.post(onReady);
                return;
            }
            List<CachedApp> stored = store.load();
            boolean fromStore = stored != null && !stored.isEmpty();
            if (fromStore) {
                cache = withoutSelf(stored);
            } else {
                cache = scanFull();
                store.save(cache);
            }
            loaded = true;
            if (onReady != null) mainHandler.post(onReady);

            // Список уже можно показывать; сверка идёт следом и исправляет пропущенное
            if (fromStore) reconcile();
        });
    }

    public List<CachedApp> getAll() {
        return cache;
    }

    public List<CachedApp> getByCategory(int flagMask) {
        List<CachedApp> result = new ArrayList<>();
        for (CachedApp app : cache) {
            if (app.hasCategory(flagMask)) result.add(app);
        }
        return result;
    }

    /** Установка или обновление одного пакета (вызывает PackageChangeReceiver). */
    public void addOrUpdatePackage(String packageName, @Nullable Runnable onDone) {
        executor.execute(() -> {
            applyAddOrUpdate(packageName);
            store.save(cache);
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    /** Удаление одного пакета (вызывает PackageChangeReceiver). */
    public void removePackage(String packageName, @Nullable Runnable onDone) {
        executor.execute(() -> {
            applyRemove(packageName);
            store.save(cache);
            if (onDone != null) mainHandler.post(onDone);
        });
    }

    // ---------- Сверка с системой ----------

    private void reconcile() {
        PackageManager pm = appContext.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        Set<String> actual = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(launcherIntent, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (!isSelf(pkg)) actual.add(pkg);
        }
        Set<String> cached = new HashSet<>();
        for (CachedApp app : cache) cached.add(app.getPackageName());

        Set<String> added = new HashSet<>(actual);
        added.removeAll(cached);
        Set<String> removed = new HashSet<>(cached);
        removed.removeAll(actual);

        if (added.isEmpty() && removed.isEmpty()) return;

        if (added.size() + removed.size() > RECONCILE_INCREMENTAL_LIMIT) {
            cache = scanFull();
        } else {
            for (String pkg : removed) applyRemove(pkg);
            for (String pkg : added) applyAddOrUpdate(pkg);
        }
        store.save(cache);
    }

    // ---------- Изменение кэша (вызывать только с потока executor) ----------

    private void applyAddOrUpdate(String packageName) {
        if (isSelf(packageName)) return;

        PackageManager pm = appContext.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(packageName);
        List<ResolveInfo> infos = pm.queryIntentActivities(launcherIntent, 0);

        List<CachedApp> updated = new ArrayList<>();
        for (CachedApp app : cache) {
            if (!app.getPackageName().equals(packageName)) updated.add(app);
        }
        if (!infos.isEmpty()) {
            int flags = resolveFlagsForPackage(pm, packageName);
            for (ResolveInfo ri : infos) {
                updated.add(new CachedApp(packageName, ri.activityInfo.name, flags));
            }
        }
        cache = updated;
    }

    private void applyRemove(String packageName) {
        List<CachedApp> updated = new ArrayList<>();
        for (CachedApp app : cache) {
            if (!app.getPackageName().equals(packageName)) updated.add(app);
        }
        cache = updated;
    }

    // ---------- Сканирование и определение категорий ----------

    private List<CachedApp> scanFull() {
        PackageManager pm = appContext.getPackageManager();
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launcherApps = pm.queryIntentActivities(launcherIntent, 0);

        Set<String> cameraPkgs = resolvePackages(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
        Set<String> galleryPkgs = resolvePackages(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY));
        Set<String> musicPkgs = resolvePackages(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC));

        List<CachedApp> result = new ArrayList<>(launcherApps.size());
        for (ResolveInfo ri : launcherApps) {
            String pkg = ri.activityInfo.packageName;
            if (isSelf(pkg)) continue;

            int flags = AppCategoryFlags.NONE;
            if (cameraPkgs.contains(pkg)) flags |= AppCategoryFlags.CAMERA;
            if (galleryPkgs.contains(pkg)) flags |= AppCategoryFlags.GALLERY;
            if (musicPkgs.contains(pkg)) flags |= AppCategoryFlags.MUSIC;
            if (RECORDER_FALLBACK_PACKAGES.contains(pkg)) flags |= AppCategoryFlags.RECORDER;
            if (isGame(ri.activityInfo.applicationInfo)) flags |= AppCategoryFlags.GAME;

            result.add(new CachedApp(pkg, ri.activityInfo.name, flags));
        }
        return result;
    }

    private int resolveFlagsForPackage(PackageManager pm, String packageName) {
        int flags = AppCategoryFlags.NONE;
        if (resolvePackages(pm, new Intent(MediaStore.ACTION_IMAGE_CAPTURE)).contains(packageName)) {
            flags |= AppCategoryFlags.CAMERA;
        }
        if (resolvePackages(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY)).contains(packageName)) {
            flags |= AppCategoryFlags.GALLERY;
        }
        if (resolvePackages(pm, new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)).contains(packageName)) {
            flags |= AppCategoryFlags.MUSIC;
        }
        if (RECORDER_FALLBACK_PACKAGES.contains(packageName)) {
            flags |= AppCategoryFlags.RECORDER;
        }
        try {
            if (isGame(pm.getApplicationInfo(packageName, 0))) flags |= AppCategoryFlags.GAME;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return flags;
    }

    @SuppressWarnings("deprecation")
    private boolean isGame(ApplicationInfo appInfo) {
        if (Build.VERSION.SDK_INT >= 26 && appInfo.category == ApplicationInfo.CATEGORY_GAME) {
            return true;
        }
        return (appInfo.flags & ApplicationInfo.FLAG_IS_GAME) != 0;
    }

    private static Set<String> resolvePackages(PackageManager pm, Intent intent) {
        Set<String> set = new HashSet<>();
        for (ResolveInfo ri : pm.queryIntentActivities(intent, 0)) set.add(ri.activityInfo.packageName);
        return set;
    }

    private boolean isSelf(String packageName) {
        return packageName.equals(appContext.getPackageName());
    }

    private List<CachedApp> withoutSelf(List<CachedApp> apps) {
        List<CachedApp> result = new ArrayList<>(apps.size());
        for (CachedApp app : apps) {
            if (!isSelf(app.getPackageName())) result.add(app);
        }
        return result;
    }
}
