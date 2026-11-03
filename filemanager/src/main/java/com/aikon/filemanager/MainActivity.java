package com.aikon.filemanager;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.aikon.core.ui.AikonBaseActivity;
import com.aikon.core.ui.AikonSoftkeyBar;
import com.aikon.core.ui.AikonLoadingOverlay;

import java.io.File;
import java.io.IOException;

/**
 * Каждая открытая папка — отдельная страница в back stack (как экраны лаунчера):
 * "Назад" возвращает на предыдущую страницу, на первой странице — закрывает приложение.
 * Физические MENU/BACK приходят сюда через AikonBaseActivity и softkey-панель.
 */
public class MainActivity extends AikonBaseActivity {

    private static final int REQUEST_STORAGE = 1001;

    /** Незавершённое копирование/перемещение. originDepth — глубина back stack, с которой оно началось. */
    static final class TransferState {
        final boolean move;
        final File source;
        final int originDepth;

        TransferState(boolean move, File source, int originDepth) {
            this.move = move;
            this.source = source;
            this.originDepth = originDepth;
        }
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextView noAccessText;
    private boolean permissionRequested;
    @Nullable private TransferState transfer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        noAccessText = findViewById(R.id.text_no_access);
        AikonSoftkeyBar softkeyBar = findViewById(R.id.softkey_bar);
        attachSoftkeyBar(softkeyBar);
        softkeyBar.setOnSoftkeyClickListener(new AikonSoftkeyBar.OnSoftkeyClickListener() {
            @Override
            public void onLeftClick(View view) {
                if (!StorageAccess.hasAccess(MainActivity.this)) {
                    StorageAccess.request(MainActivity.this, REQUEST_STORAGE);
                    return;
                }
                Fragment current = currentFragment();
                if (current instanceof DirectoryFragment) {
                    ((DirectoryFragment) current).onMenuRequested();
                }
            }

            @Override
            public void onCenterClick(View view) {
                // В режиме двух кнопок центральной кнопки нет
            }

            @Override
            public void onRightClick(View view) {
                goBack();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (StorageAccess.hasAccess(this)) {
            noAccessText.setVisibility(View.GONE);
            if (currentFragment() == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container,
                                DirectoryFragment.newInstance(Environment.getExternalStorageDirectory()))
                        .commit();
            }
        } else {
            noAccessText.setVisibility(View.VISIBLE);
            if (!permissionRequested) {
                permissionRequested = true;
                StorageAccess.request(this, REQUEST_STORAGE);
            }
        }
    }

    // ---------- Навигация ----------

    void openDirectory(File dir) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, DirectoryFragment.newInstance(dir))
                .addToBackStack(null)
                .commit();
    }

    private void goBack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            finish();
        }
    }

    @Nullable
    private Fragment currentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }

    // ---------- Копирование / перемещение ----------

    boolean isTransferActive() {
        return transfer != null;
    }

    void beginTransfer(boolean move, File source) {
        transfer = new TransferState(move, source,
                getSupportFragmentManager().getBackStackEntryCount());
        reloadCurrent(); // текущий список перерисуется в простом виде
    }

    void cancelTransfer() {
        finishTransfer();
    }

    void completeTransfer(File targetDir) {
        final TransferState state = transfer;
        if (state == null) return;

        if (state.move && targetDir.equals(state.source.getParentFile())) {
            Toast.makeText(this, R.string.fm_toast_already_here, Toast.LENGTH_SHORT).show();
            finishTransfer();
            return;
        }

        final android.content.Context app = getApplicationContext();
        AikonLoadingOverlay.runAsync(() -> {
            String message;
            try {
                if (state.move) FileOperations.move(state.source, targetDir);
                else FileOperations.copy(state.source, targetDir);
                message = app.getString(state.move ? R.string.fm_toast_moved : R.string.fm_toast_copied);
            } catch (FileOperations.SelfCopyException e) {
                message = app.getString(R.string.fm_toast_into_itself);
            } catch (IOException e) {
                message = app.getString(R.string.fm_toast_error, String.valueOf(e.getMessage()));
            }
            final String toastText = message;
            mainHandler.post(() -> {
                Toast.makeText(app, toastText, Toast.LENGTH_SHORT).show();
                finishTransfer();
            });
        });
    }

    /** Выходит из режима выбора папки и возвращает пользователя на страницу, откуда он начал. */
    private void finishTransfer() {
        final TransferState state = transfer;
        transfer = null;
        if (state == null) return;

        FragmentManager fm = getSupportFragmentManager();
        if (isFinishing() || fm.isStateSaved()) return;

        if (fm.getBackStackEntryCount() > state.originDepth) {
            if (state.originDepth == 0) {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            } else {
                fm.popBackStackImmediate(
                        fm.getBackStackEntryAt(state.originDepth - 1).getId(), 0);
            }
            // страница восстановится сама, с пересозданием списка в обычном виде
        } else {
            reloadCurrent(); // остались на той же странице — перечитываем на месте
        }
    }

    private void reloadCurrent() {
        Fragment current = currentFragment();
        if (current instanceof DirectoryFragment) ((DirectoryFragment) current).reload();
    }
}
