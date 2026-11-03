package com.aikon.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aikon.launcher.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Главный экран лаунчера: время, дата и (в Activity) панель кнопок снизу.
 * Обновление часов идёт через системный ACTION_TIME_TICK (раз в минуту),
 * а не через собственный Handler/поток — так экономим CPU и батарею.
 */
public class HomeFragment extends Fragment {

    private TextView clockView;
    private TextView dateView;
    private SimpleDateFormat dateFormat;

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateClock();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        clockView = view.findViewById(R.id.text_clock);
        dateView = view.findViewById(R.id.text_date);
        dateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        requireContext().registerReceiver(timeReceiver, filter);
        updateClock();
    }

    @Override
    public void onPause() {
        super.onPause();
        requireContext().unregisterReceiver(timeReceiver);
    }

    private void updateClock() {
        Calendar now = Calendar.getInstance();
        // Формат времени берём из системных настроек (12ч/24ч) — так пользователь
        // видит время так же, как во всей системе, без ручной логики.
        java.text.DateFormat systemTimeFormat = DateFormat.getTimeFormat(requireContext());
        clockView.setText(systemTimeFormat.format(now.getTime()));
        dateView.setText(capitalize(dateFormat.format(now.getTime())));
    }

    private String capitalize(String text) {
        if (text.isEmpty()) return text;
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }
}