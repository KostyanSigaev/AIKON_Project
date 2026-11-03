package com.aikon.core.ui.list;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Готовый список AIKON: LinearLayoutManager и настройки производительности
 * заданы один раз здесь, чтобы каждое приложение проекта их не дублировало.
 *
 * Также автоматически ставит клавиатурный фокус на первый элемент списка
 * сразу после его заполнения — на кнопочных устройствах это избавляет от
 * лишнего "холостого" нажатия стрелкой только для того, чтобы проявилось
 * выделение, прежде чем начать реальную навигацию по списку.
 *
 * В режиме сенсорного управления (touch mode) requestFocus() у обычных
 * View тихо не срабатывает — поэтому при обычном тапе по экрану никакой
 * рамки фокуса не появится, автофокус проявляется только при реальной
 * навигации физическими клавишами/D-pad'ом.
 */
public class AikonListView extends RecyclerView {

    public AikonListView(Context context) {
        this(context, null);
    }

    public AikonListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AikonListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setLayoutManager(new LinearLayoutManager(context));
        setHasFixedSize(true);
        setItemViewCacheSize(20);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        super.setAdapter(adapter);
        requestInitialFocus();
    }

    private void requestInitialFocus() {
        Adapter<?> adapter = getAdapter();
        if (adapter == null || adapter.getItemCount() == 0) return;
        if (hasFocus()) return; // фокус уже стоит внутри списка — не перехватываем

        addOnChildAttachStateChangeListener(new OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(@NonNull View view) {
                if (getChildAdapterPosition(view) == 0) {
                    view.requestFocus();
                    removeOnChildAttachStateChangeListener(this);
                }
            }

            @Override
            public void onChildViewDetachedFromWindow(@NonNull View view) {
            }
        });
    }
}
