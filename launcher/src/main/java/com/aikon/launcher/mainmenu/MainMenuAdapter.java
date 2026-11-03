package com.aikon.launcher.mainmenu;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aikon.launcher.R;

import java.util.List;

public class MainMenuAdapter extends RecyclerView.Adapter<MainMenuAdapter.ViewHolder> {

    public interface OnMenuItemClickListener {
        void onMenuItemClick(MainMenuItem item);
    }

    private final List<MainMenuItem> items;
    private final OnMenuItemClickListener listener;

    public MainMenuAdapter(List<MainMenuItem> items, OnMenuItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_main_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MainMenuItem item = items.get(position);
        holder.icon.setImageResource(item.getIconRes());
        holder.title.setText(item.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onMenuItemClick(item);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;
        ViewHolder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.menu_icon);
            title = itemView.findViewById(R.id.menu_title);
        }
    }
}
