package com.aikon.launcher.apps;

import androidx.annotation.Nullable;

/**
 * Хранит одно отложенное действие, которое нужно выполнить, когда
 * MainActivity снова окажется на переднем плане (onResume) — используется,
 * чтобы после возврата из системного экрана "О приложении" (где пользователь
 * вручную очистил кэш) автоматически доткрыть исходное приложение.
 */
public final class PendingLaunchHolder {
    private PendingLaunchHolder() {}

    @Nullable private static Runnable pendingAction;

    public static void setPending(Runnable action) {
        pendingAction = action;
    }

    public static void consumeIfPresent() {
        Runnable action = pendingAction;
        pendingAction = null;
        if (action != null) action.run();
    }
}
