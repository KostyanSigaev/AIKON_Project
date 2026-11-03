package com.aikon.core.ui;

import android.app.Activity;
import android.app.Dialog;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import com.aikon.core.R;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Единое окно "Пожалуйста, подождите..." для всего проекта AIKON.
 * Привязывается к текущей видимой Activity автоматически (через
 * AikonBaseActivity), поэтому show()/hide() можно звать из любого
 * приложения проекта без передачи Context.
 *
 * show()/hide() безопасны для вызова с ЛЮБОГО потока: если вызов пришёл
 * не с главного потока, он сам перекидывается на него через Handler.
 */
public final class AikonLoadingOverlay {

    private AikonLoadingOverlay() {}

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final ExecutorService BACKGROUND = Executors.newCachedThreadPool();

    @Nullable private static volatile Activity currentActivity;
    @Nullable private static Dialog dialog;
    private static int showCount = 0; // защита от преждевременного скрытия при вложенных show()

    /** Вызывается ядром (AikonBaseActivity) — не нужно дёргать вручную из приложений. */
    public static void attach(Activity activity) {
        currentActivity = activity;
    }

    public static void detach(Activity activity) {
        if (currentActivity == activity) {
            currentActivity = null;
            dialog = null; // диалог привязан к окну активности, которая уходит с экрана
            showCount = 0;
        }
    }

    /** Показать окно загрузки поверх текущего экрана. */
    public static void show() {
        runOnMain(() -> {
            showCount++;
            if (dialog != null && dialog.isShowing()) return;
            Activity activity = currentActivity;
            if (activity == null || activity.isFinishing()) return;
            dialog = buildDialog(activity);
            dialog.show();
        });
    }

    /** Скрыть окно загрузки. */
    public static void hide() {
        runOnMain(() -> {
            if (showCount > 0) showCount--;
            if (showCount > 0) return; // есть ещё незавершённые вложенные show()
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
        });
    }

    /**
     * Удобный способ выполнить тяжёлую задачу с автоматическим показом/скрытием
     * окна загрузки — показывает окно, выполняет задачу в фоновом потоке,
     * гарантированно скрывает окно по завершении (даже при исключении).
     */
    public static void runAsync(Runnable backgroundTask) {
        show();
        BACKGROUND.execute(() -> {
            try {
                backgroundTask.run();
            } finally {
                hide();
            }
        });
    }

    private static Dialog buildDialog(Activity activity) {
        Dialog dialog = new Dialog(activity, R.style.Theme_Aikon_LoadingOverlay);
        dialog.setContentView(R.layout.aikon_loading_overlay);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            // Сдвиг вниз, чтобы окно оказалось в нижней половине экрана
            params.y = activity.getResources().getDisplayMetrics().heightPixels / 4;
            window.setAttributes(params);
        }
        return dialog;
    }

    private static void runOnMain(Runnable action) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action.run();
        } else {
            MAIN_HANDLER.post(action);
        }
    }
}
