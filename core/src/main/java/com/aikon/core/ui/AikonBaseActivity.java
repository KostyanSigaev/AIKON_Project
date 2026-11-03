package com.aikon.core.ui;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aikon.core.ui.AikonLoadingOverlay;

/**
 * Базовая Activity для всех приложений AIKON UI.
 *
 * Централизует обработку аппаратных клавиш MENU / OK / BACK так, чтобы
 * физическая клавиатура всегда "нажимала" ту же кнопку панели, что и тап
 * пальцем — единая точка входа исключает двойную навигацию.
 *
 * MENU и BACK привязаны ПОЗИЦИОННО (левая/правая кнопка), а не по смыслу:
 * например, на главном экране лаунчера правая кнопка — это "Избранные",
 * и физическая BACK-клавиша там откроет именно избранное, а не "отменит"
 * что-либо — так же вело бы себя реальное устройство на Symbian/S40.
 */
public abstract class AikonBaseActivity extends AppCompatActivity {

    @Nullable
    private AikonSoftkeyBar activeSoftkeyBar;

    private final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            // Единая точка входа для "назад": системный жест, аппаратная
            // кнопка и экранная кнопка навигации — все идут сюда, а не
            // в автоматический откат back stack по умолчанию.
            if (activeSoftkeyBar != null) {
                activeSoftkeyBar.performRightAction();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AikonLoadingOverlay.attach(this);
    }

    @Override
    protected void onPause() {
        // Если экран уходит с переднего плана, а окно загрузки ещё не закрыто
        // конкретным вызовом hide() — скрываем его превентивно, чтобы не
        // держать Dialog поверх уже неактивной Activity (риск утечки окна).
        AikonLoadingOverlay.hide();
        AikonLoadingOverlay.detach(this);
        super.onPause();
    }

    /** Вызывать один раз в onCreate наследника, после того как панель найдена в layout. */
    protected void attachSoftkeyBar(@NonNull AikonSoftkeyBar bar) {
        this.activeSoftkeyBar = bar;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (activeSoftkeyBar != null
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {

            int keyCode = event.getKeyCode();

            if (keyCode == KeyEvent.KEYCODE_MENU) {
                activeSoftkeyBar.performLeftAction();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
                if (activeSoftkeyBar.performCenterAction()) {
                    return true;
                }
                // Центральной кнопки нет в текущем режиме — событие уходит
                // дальше как обычное нажатие OK, например выбор элемента списка.
            }

            // KEYCODE_BACK намеренно НЕ перехватывается здесь — он идёт в
            // OnBackPressedDispatcher выше, чтобы аппаратная кнопка,
            // экранная кнопка навигации и системный жест шли одним путём.
        }
        return super.dispatchKeyEvent(event);
    }
}
