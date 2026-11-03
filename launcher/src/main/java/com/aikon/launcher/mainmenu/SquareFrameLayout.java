package com.aikon.launcher.mainmenu;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Форсирует квадратную форму ячейки одной строкой в onMeasure —
 * без ViewTreeObserver и без пересчётов после первого layout-прохода,
 * это самый дешёвый способ получить квадрат в сетке по CPU.
 */
public class SquareFrameLayout extends FrameLayout {

    public SquareFrameLayout(Context context) { super(context); }
    public SquareFrameLayout(Context context, AttributeSet attrs) { super(context, attrs); }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
