package com.aikon.launcher.mainmenu;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aikon.launcher.MainActivity;
import com.aikon.launcher.R;
import com.aikon.launcher.applist.AppsQuickListFragment;
import com.aikon.launcher.MultimediaListFragment;
import com.aikon.launcher.apps.DefaultAppResolver;
import com.aikon.launcher.games.GamesListFragment;

public class MainMenuFragment extends Fragment {
    private static final int SPAN_COUNT = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView grid = view.findViewById(R.id.grid_main_menu);
        grid.setLayoutManager(new GridLayoutManager(requireContext(), SPAN_COUNT));
        grid.setHasFixedSize(true);
        grid.setAdapter(new MainMenuAdapter(MainMenuData.items(), this::handleClick));

        grid.post(() -> {
            RecyclerView.ViewHolder holder = grid.findViewHolderForAdapterPosition(INITIAL_FOCUS_POSITION);
            if(holder != null) holder.itemView.requestFocus();
        });
    }

    private void handleClick(MainMenuItem item) {
        switch (item.getType()) {
            case APP:
                launchApp(item.getPackageName());
                break;
            case LIST:
                openList(item.getListTarget());
                break;
            case DEFAULT_APP:
                launchDefaultApp(item.getDefaultAppKind());
                break;
            case LINK:
                openLink(item.getLinkTarget());
                break;
        }
    }

    private void launchDefaultApp(MainMenuItem.DefaultAppKind kind) {
        Intent intent;
        switch (kind) {
            case PHONE: intent = DefaultAppResolver.resolvePhone(requireContext()); break;
            case CONTACTS: intent = DefaultAppResolver.resolveContacts(requireContext()); break;
            case SMS: intent = DefaultAppResolver.resolveSms(requireContext()); break;
            case BROWSER: intent = DefaultAppResolver.resolveBrowser(requireContext()); break;
            default: intent = null;
        }
        if (intent == null) {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void openList(MainMenuItem.ListTarget target) {
        Fragment fragment;
        String tag;
        switch (target) {
            case MULTIMEDIA:
                fragment = new MultimediaListFragment();
                tag = "multimedia";
                break;
            case GAMES:
                fragment = new GamesListFragment();
                tag = "games";
                break;
            default:
                fragment = new AppsQuickListFragment();
                tag = "apps_quick";
        }
        ((MainActivity) requireActivity()).openScreen(fragment, tag);
    }

    private void openLink(MainMenuItem.LinkTarget target) {
        if (target == MainMenuItem.LinkTarget.FDROID) {
            openFDroid();
        }
    }

    private void openFDroid() {
        PackageManager pm = requireContext().getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage("org.fdroid.fdroid");
        if (launch != null) {
            startActivity(launch);
            return;
        }
        // F-Droid не установлен — вместо простого отказа открываем его страницу в браузере
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://f-droid.org"));
        if (browserIntent.resolveActivity(pm) != null) {
            startActivity(browserIntent);
        } else {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchApp(String packageName) {
        Intent intent = requireContext().getPackageManager().getLaunchIntentForPackage(packageName);
        if(intent == null) {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        startActivity(intent);
    }

    private void openDefaultBrowser() {
        PackageManager pm = requireContext().getPackageManager();
        Intent probe = new Intent(Intent.ACTION_VIEW, Uri.parse("http://"));
        ResolveInfo resolveInfo = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY);
        if(resolveInfo == null) {
            Toast.makeText(requireContext(), R.string.app_not_found, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent launchIntent = pm.getLaunchIntentForPackage(resolveInfo.activityInfo.packageName);
        startActivity(launchIntent != null ? launchIntent : probe);
    }

    private static final int INITIAL_FOCUS_POSITION = 4; // 0-based — это "Приложения"
}
