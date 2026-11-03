package com.aikon.core.ui.list;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aikon.core.R;

import java.util.List;

/**
 * Единый адаптер для SIMPLE и FULL элементов. Тип view определяется
 * автоматически по AikonListItem.Type — так один RecyclerView может
 * содержать вперемешку оба вида (пригодится, например, для настроек
 * с секциями-разделителями в будущем).
 */
public class AikonListAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int VIEW_TYPE_SIMPLE = 0;
    private static final int VIEW_TYPE_FULL = 1;

    public interface OnItemClickListener<T> {
        void onItemClick(AikonListItem<T> item, int position);
    }

    private final List<AikonListItem<T>> items;
    private final OnItemClickListener<T> listener;

    /*public AikonListAdapter(List<AikonListItem<T>> items, OnItemClickListener<T> listener) {
        this.items = items;
        this.listener = listener;
    }*/

    @Override
    public int getItemViewType(int position) {
        return items.get(position).getType() == AikonListItem.Type.FULL ? VIEW_TYPE_FULL : VIEW_TYPE_SIMPLE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == VIEW_TYPE_FULL) {
            return new FullViewHolder(inflater.inflate(R.layout.aikon_list_item_full, parent, false));
        }
        return new SimpleViewHolder(inflater.inflate(R.layout.aikon_list_item_simple, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AikonListItem<T> item = items.get(position);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, holder.getBindingAdapterPosition());
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(item, holder.getBindingAdapterPosition());
                return true;
            }
            return false;
        });

        if (holder instanceof SimpleViewHolder) {
            ((SimpleViewHolder) holder).title.setText(item.getTitle());
        } else if (holder instanceof FullViewHolder) {
            FullViewHolder fh = (FullViewHolder) holder;
            fh.title.setText(item.getTitle());
            fh.description.setText(item.getDescription());
            fh.icon.setImageDrawable(item.getIcon());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class SimpleViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        SimpleViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.aikon_list_title);
        }
    }

    static class FullViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView description;
        final ImageView icon;
        FullViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.aikon_list_title);
            description = itemView.findViewById(R.id.aikon_list_description);
            icon = itemView.findViewById(R.id.aikon_list_icon);
        }
    }

    public interface OnItemLongClickListener<T> {
        void onItemLongClick(AikonListItem<T> item, int position);
    }

    private final OnItemLongClickListener<T> longClickListener;

    public AikonListAdapter(List<AikonListItem<T>> items, OnItemClickListener<T> listener) {
        this(items, listener, null);
    }

    public AikonListAdapter(List<AikonListItem<T>> items, OnItemClickListener<T> listener,
                            OnItemLongClickListener<T> longClickListener) {
        this.items = items;
        this.listener = listener;
        this.longClickListener = longClickListener;
    }
}
