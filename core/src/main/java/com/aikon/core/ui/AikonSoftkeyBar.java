package com.aikon.core.ui;

import static androidx.core.app.ActivityCompat.startActivityForResult;
import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.PowerManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.aikon.core.R;

public class AikonSoftkeyBar extends LinearLayout {

    public enum Mode {
        TWO_BUTTON,
        THREE_BUTTON
    }

    public interface OnSoftkeyClickListener {
        void onLeftClick(View view);
        void onCenterClick(View view);
        void onRightClick(View view);
    }

    private Mode mode = Mode.TWO_BUTTON;
    private TextView leftButton;
    private TextView centerButton;
    private TextView rightButton;
    private OnSoftkeyClickListener listener;
    private final int fixedHeightPx;

    public AikonSoftkeyBar(Context context) {
        this(context, null);
    }

    public AikonSoftkeyBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AikonSoftkeyBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setOrientation(HORIZONTAL);
        setBackgroundColor(resolveThemeColor(R.attr.aikonBarBackground, 0xFF1B1B1B));
        fixedHeightPx = getResources().getDimensionPixelSize(R.dimen.aikon_bar_height);

        String leftText = null, centerText = null, rightText = null;

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AikonSoftkeyBar);
            int modeValue = a.getInt(R.styleable.AikonSoftkeyBar_aikonBarMode, 0);
            mode = (modeValue == 1) ? Mode.THREE_BUTTON : Mode.TWO_BUTTON;
            leftText = a.getString(R.styleable.AikonSoftkeyBar_aikonLeftText);
            centerText = a.getString(R.styleable.AikonSoftkeyBar_aikonCenterText);
            rightText = a.getString(R.styleable.AikonSoftkeyBar_aikonRightText);
            a.recycle();
        }

        inflateMode();
        bindViews();
        applyCustomTexts(leftText, centerText, rightText);
        setClickForwarding();
    }

    /**
     * Принудительно фиксируем высоту панели независимо от того, что указано
     * снаружи (match_parent, wrap_content и т.п.) — так гарантируется
     * одинаковое поведение во всех приложениях проекта без исключений,
     * и это дешевле по CPU, чем сложный measure-пасс LinearLayout по умолчанию.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int exactHeightSpec = MeasureSpec.makeMeasureSpec(fixedHeightPx, MeasureSpec.EXACTLY);
        super.onMeasure(widthMeasureSpec, exactHeightSpec);
    }

    private void inflateMode() {
        removeAllViews();
        int layoutRes = (mode == Mode.THREE_BUTTON)
                ? R.layout.aikon_softkey_bar_three
                : R.layout.aikon_softkey_bar_two;
        LayoutInflater.from(getContext()).inflate(layoutRes, this, true);
    }

    private void bindViews() {
        leftButton = findViewById(R.id.aikon_btn_left);
        rightButton = findViewById(R.id.aikon_btn_right);
        centerButton = (mode == Mode.THREE_BUTTON) ? findViewById(R.id.aikon_btn_center) : null;
    }

    private void applyCustomTexts(String left, String center, String right) {
        if (left != null) leftButton.setText(left);
        if (right != null) rightButton.setText(right);
        if (center != null && centerButton != null) centerButton.setText(center);
    }

    private void setClickForwarding() {
        leftButton.setOnClickListener(v -> {
            if (mode == Mode.THREE_BUTTON) {
                performLockScreen();
            }
            if (listener != null) listener.onLeftClick(v);
        });
        rightButton.setOnClickListener(v -> {
            if (listener != null) listener.onRightClick(v);
        });
        if (centerButton != null) {
            centerButton.setOnClickListener(v -> {
                if (listener != null) listener.onCenterClick(v);
            });
        }
    }

    /** Вызывается автоматически при нажатии левой кнопки в режиме THREE_BUTTON. */
    protected void performLockScreen() {
        AikonScreenLock.lock(getContext());
    }

    private int resolveThemeColor(int attrRes, int fallback) {
        TypedArray a = getContext().obtainStyledAttributes(new int[]{attrRes});
        int color = a.getColor(0, fallback);
        a.recycle();
        return color;
    }

    // ---------- Публичный API ----------

    public void setMode(Mode mode) {
        if (this.mode == mode) return;
        this.mode = mode;
        inflateMode();
        bindViews();
        setClickForwarding();
        requestLayout();
    }

    public Mode getMode() {
        return mode;
    }

    public void setOnSoftkeyClickListener(OnSoftkeyClickListener listener) {
        this.listener = listener;
    }

    /** Аппаратная клавиша MENU всегда "нажимает" левую кнопку — какая бы функция у неё сейчас ни была. */
    public void performLeftAction() {
        leftButton.performClick();
    }

    /**
     * Аппаратная клавиша OK (DPAD_CENTER/ENTER) "нажимает" центральную кнопку,
     * если она есть в текущем режиме (THREE_BUTTON).
     * @return true, если центральная кнопка существует и была нажата;
     *         false в режиме TWO_BUTTON — тогда клавиша OK должна уйти дальше
     *         как обычное выделение элемента в списке.
     */
    public boolean performCenterAction() {
        if (centerButton != null) {
            centerButton.performClick();
            return true;
        }
        return false;
    }

    /** Аппаратная клавиша BACK всегда "нажимает" правую кнопку — какая бы функция у неё сейчас ни была. */
    public void performRightAction() {
        rightButton.performClick();
    }

    public void setLeftText(CharSequence text) { leftButton.setText(text); }
    public void setCenterText(CharSequence text) { if (centerButton != null) centerButton.setText(text); }
    public void setRightText(CharSequence text) { rightButton.setText(text); }

    public TextView getLeftButton() { return leftButton; }
    public TextView getCenterButton() { return centerButton; }
    public TextView getRightButton() { return rightButton; }
}
