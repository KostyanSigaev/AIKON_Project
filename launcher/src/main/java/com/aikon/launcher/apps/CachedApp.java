package com.aikon.launcher.apps;

import android.content.Intent;

/**
 * Кэшированная запись приложения. Намеренно НЕ хранит название —
 * оно всегда актуально в самой системе и берётся через PackageManager
 * в момент отображения, а не дублируется здесь.
 */
public class CachedApp {

    private final String packageName;
    private final String activityName;
    private final int categoryFlags;

    public CachedApp(String packageName, String activityName, int categoryFlags) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.categoryFlags = categoryFlags;
    }

    public String getPackageName() { return packageName; }
    public String getActivityName() { return activityName; }
    public int getCategoryFlags() { return categoryFlags; }

    public boolean hasCategory(int flag) {
        return (categoryFlags & flag) != 0;
    }

    public Intent createLaunchIntent() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(packageName, activityName);
        return intent;
    }
}
