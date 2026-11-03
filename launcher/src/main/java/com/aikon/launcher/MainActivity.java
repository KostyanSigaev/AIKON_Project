package com.aikon.launcher;

import android.os.Bundle;

import com.aikon.core.ui.AikonBaseActivity;
import com.aikon.core.ui.AikonSoftkeyBar;
import com.aikon.launcher.favorites.FavoritesFragment;
import com.aikon.core.ui.AikonLoadingOverlay;
import com.aikon.launcher.mainmenu.MainMenuFragment;

public class MainActivity extends AikonBaseActivity {

    private AikonSoftkeyBar softkeyBar;

    private final AikonSoftkeyBar.OnSoftkeyClickListener homeListener = new AikonSoftkeyBar.OnSoftkeyClickListener() {
        @Override
        public void onLeftClick(android.view.View view) {
            // Блокировка экрана уже вызывается внутри самой панели (performLockScreenStub)
        }

        @Override
        public void onCenterClick(android.view.View view) {
            openScreen(new MainMenuFragment(), "main_menu");
        }

        @Override
        public void onRightClick(android.view.View view) {
            openScreen(new FavoritesFragment(), "favorites");
        }
    };

    private final AikonSoftkeyBar.OnSoftkeyClickListener backListener = new AikonSoftkeyBar.OnSoftkeyClickListener() {
        @Override
        public void onLeftClick(android.view.View view) {
            // TODO: контекстное меню текущего экрана
        }

        @Override
        public void onCenterClick(android.view.View view) {
            // В режиме двух кнопок центральной кнопки нет — не вызывается
        }

        @Override
        public void onRightClick(android.view.View view) {
            getSupportFragmentManager().popBackStack();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        softkeyBar = findViewById(R.id.softkey_bar);
        attachSoftkeyBar(softkeyBar);
        AikonLoadingOverlay.attach(this);
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateSoftkeyForCurrentScreen);

        if(savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
        updateSoftkeyForCurrentScreen();
    }

    public void openScreen(androidx.fragment.app.Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(tag)
                .commit();
    }

    private void updateSoftkeyForCurrentScreen() {
        boolean atHome = getSupportFragmentManager().getBackStackEntryCount() == 0;
        if(atHome) {
            softkeyBar.setMode(AikonSoftkeyBar.Mode.THREE_BUTTON);
            softkeyBar.setOnSoftkeyClickListener(homeListener);
        } else {
            softkeyBar.setMode(AikonSoftkeyBar.Mode.TWO_BUTTON);
            softkeyBar.setOnSoftkeyClickListener(backListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        com.aikon.launcher.apps.PendingLaunchHolder.consumeIfPresent();
    }
}
