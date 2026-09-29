package ui.componentes;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.OverScroller;

/**
 * ScrollView que rola em QUALQUER direção (2D), com fling.
 *
 * - Toque curto → passa pro filho (ex: posicionar cursor)
 * - Arrastar → rola 2D
 * - Soltar rápido → fling 2D
 */
public class ScrollView2D extends ViewGroup {

    private OverScroller scroller;
    private GestureDetector gestureDetector;

    private int lastX, lastY;
    private int touchSlop;
    private boolean isBeingDragged = false;

    private int maxScrollX, maxScrollY;

    public ScrollView2D(Context context) {
        this(context, null);
    }

    public ScrollView2D(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context ctx) {
        scroller = new OverScroller(ctx);
        touchSlop = ViewConfiguration.get(ctx).getScaledTouchSlop();

        gestureDetector = new GestureDetector(ctx,
                new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                return true;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2,
                                   float velocityX, float velocityY) {
                if (maxScrollX > 0 || maxScrollY > 0) {
                    scroller.fling(
                            getScrollX(), getScrollY(),
                            (int) -velocityX, (int) -velocityY,
                            0, maxScrollX,
                            0, maxScrollY);
                    postInvalidateOnAnimation();
                }
                return true;
            }
        });
    }

    // ==========================================================
    //  Medição / layout
    // ==========================================================

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec);
        int height = MeasureSpec.getSize(heightSpec);

        // Mede o filho sem restrição de tamanho (ele cresce o quanto quiser)
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int childW = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            int childH = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            child.measure(childW, childH);
        }

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int childW = child.getMeasuredWidth();
            int childH = child.getMeasuredHeight();

            // Limites de scroll
            maxScrollX = Math.max(0, childW - getWidth());
            maxScrollY = Math.max(0, childH - getHeight());

            child.layout(0, 0, childW, childH);
        }
    }

    // ==========================================================
    //  Touch
    // ==========================================================

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) ev.getX();
                lastY = (int) ev.getY();
                isBeingDragged = !scroller.isFinished();
                if (isBeingDragged) {
                    scroller.abortAnimation();
                    postInvalidateOnAnimation();
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (isBeingDragged) return true;

                int x = (int) ev.getX();
                int y = (int) ev.getY();
                int dx = Math.abs(x - lastX);
                int dy = Math.abs(y - lastY);

                if (dx > touchSlop || dy > touchSlop) {
                    isBeingDragged = true;
                    lastX = x;
                    lastY = y;
                    return true;
                }
                return false;
        }

        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                lastX = (int) ev.getX();
                lastY = (int) ev.getY();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isBeingDragged) return true;

                int x = (int) ev.getX();
                int y = (int) ev.getY();

                int dx = x - lastX;
                int dy = y - lastY;

                int newScrollX = clamp(getScrollX() - dx, 0, maxScrollX);
                int newScrollY = clamp(getScrollY() - dy, 0, maxScrollY);

                scrollTo(newScrollX, newScrollY);

                lastX = x;
                lastY = y;
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isBeingDragged = false;
                return true;
        }

        return false;
    }

    private int clamp(int valor, int min, int max) {
        return Math.max(min, Math.min(valor, max));
    }

    // ==========================================================
    //  Fling
    // ==========================================================

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidateOnAnimation();
        }
    }
}