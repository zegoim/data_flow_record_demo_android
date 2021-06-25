package im.zego.recordplayer.manager.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import im.zego.recordplayer.R;

/**
 * 根据view的zOrder属性改变viewGroup的绘制顺序
 */
public class ZOrderLayout extends FrameLayout {
    private final List<Pair<View, Integer>> list = new ArrayList<>();

    public ZOrderLayout(@NonNull Context context) {
        this(context, null);
    }

    public ZOrderLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ZOrderLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setChildrenDrawingOrderEnabled(true);
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int drawingPosition) {
        if (list.size() > 0) {
            return indexOfChild(list.get(drawingPosition).first);
        }
        return drawingPosition;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateZOrder();
    }

    void updateZOrder() {
        list.clear();
        final int childCount = getChildCount();
        View view;
        ZOrderLayout.LayoutParams params;
        for (int i = 0; i < childCount; i++) {
            view = getChildAt(i);
            params = (LayoutParams) view.getLayoutParams();

            Pair<View, Integer> pair = new Pair<>(view, params.zOrder);
            list.add(pair);
        }

        Collections.sort(list, (o1, o2) -> o1.second - o2.second);
    }

    /**
     * 在解析xml时,会解析每个跟布局的LayoutParams
     */
    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends FrameLayout.LayoutParams {

        public final static int DEFAULT_ZORDER = 0;

        public int zOrder;

        public LayoutParams(@NonNull Context c, @Nullable AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ZOrderLayout);
            zOrder = a.getInt(R.styleable.ZOrderLayout_zorder, DEFAULT_ZORDER);
            a.recycle();
        }

        public LayoutParams(int width, int height, int zOrder) {
            super(width, height);
            this.zOrder = zOrder;
        }
    }
}