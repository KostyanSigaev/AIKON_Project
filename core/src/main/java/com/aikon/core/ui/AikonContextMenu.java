package com.aikon.core.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.aikon.core.R;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Кастомное контекстное меню AIKON. Появляется в левом нижнем углу экрана,
 * над панелью кнопок. Поддерживает вложенные подпункты (со стрелкой справа).
 *
 * Пример использования:
 *   AikonContextMenu menu = new AikonContextMenu(context);
 *   menu.addItem("Открыть", this::open);
 *   AikonContextMenu.Item more = menu.addSubmenu("Ещё");
 *   more.addItem("Подпункт", this::doSomething);
 *   menu.show(anchorView);
 */
public class AikonContextMenu {

    /** Пункт меню. Если у него есть дочерние пункты — при клике открывается подменю. */
    public static class Item {
        private final String title;
        private Runnable action;
        private List<Item> children;

        private Item(String title) {
            this.title = title;
        }

        public Item addItem(String title, Runnable action) {
            Item item = new Item(title);
            item.action = action;
            children().add(item);
            return item;
        }

        public Item addSubmenu(String title) {
            Item item = new Item(title);
            item.children = new ArrayList<>();
            children().add(item);
            return item;
        }

        private List<Item> children() {
            if (children == null) children = new ArrayList<>();
            return children;
        }

        public boolean hasChildren() {
            return children != null && !children.isEmpty();
        }
    }

    private final Context context;
    private final Item root = new Item(null);
    private final Deque<List<Item>> navigationStack = new ArrayDeque<>();

    private PopupWindow popupWindow;
    private LinearLayout container;
    private List<Item> currentLevel;

    public AikonContextMenu(Context context) {
        this.context = context;
        this.root.children = new ArrayList<>();
    }

    public Item addItem(String title, Runnable action) {
        return root.addItem(title, action);
    }

    public Item addSubmenu(String title) {
        return root.addSubmenu(title);
    }

    /** Показать меню, "прицепив" его к левому нижнему углу экрана anchor'а. */
    public void show(View anchor) {
        if (root.children == null || root.children.isEmpty()) return;

        View content = LayoutInflater.from(context).inflate(R.layout.aikon_context_menu, null);
        container = content.findViewById(R.id.aikon_context_menu_container);

        navigationStack.clear();
        currentLevel = root.children;
        render();

        popupWindow = new PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        // Прозрачный фон самого окна PopupWindow — форма и тень задаются content'ом
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        popupWindow.setOutsideTouchable(true);

        int marginStart = context.getResources().getDimensionPixelSize(R.dimen.aikon_context_menu_margin);
        int marginBottom = context.getResources().getDimensionPixelSize(R.dimen.aikon_bar_height)
                + marginStart;

        popupWindow.showAtLocation(anchor.getRootView(), Gravity.BOTTOM | Gravity.START,
                marginStart, marginBottom);
    }

    public void dismiss() {
        if (popupWindow != null) {
            popupWindow.dismiss();
            popupWindow = null;
        }
    }

    private void render() {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);

        if (!navigationStack.isEmpty()) {
            View backRow = inflater.inflate(R.layout.aikon_context_menu_item, container, false);
            ((TextView) backRow.findViewById(R.id.aikon_context_item_title))
                    .setText(R.string.aikon_context_menu_back);
            backRow.findViewById(R.id.aikon_context_item_arrow).setVisibility(View.GONE);
            backRow.setOnClickListener(v -> {
                currentLevel = navigationStack.pop();
                render();
            });
            container.addView(backRow);
        }

        for (Item item : currentLevel) {
            View row = inflater.inflate(R.layout.aikon_context_menu_item, container, false);
            TextView title = row.findViewById(R.id.aikon_context_item_title);
            ImageView arrow = row.findViewById(R.id.aikon_context_item_arrow);
            title.setText(item.title);
            arrow.setVisibility(item.hasChildren() ? View.VISIBLE : View.GONE);

            row.setOnClickListener(v -> {
                if (item.hasChildren()) {
                    navigationStack.push(currentLevel);
                    currentLevel = item.children;
                    render();
                } else {
                    dismiss();
                    if (item.action != null) item.action.run();
                }
            });
            container.addView(row);
        }
    }
}
