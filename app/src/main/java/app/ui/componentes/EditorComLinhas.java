package ui.componentes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.EditText;

/**
 * EditText com configuração para código-fonte.
 * NÃO desenha numeração — isso é trabalho do GutterView.
 */
public class EditorComLinhas extends EditText {

    public EditorComLinhas(Context context) {
        super(context);
        init(context);
    }

    public EditorComLinhas(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EditorComLinhas(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context ctx) {
        setTypeface(Typeface.MONOSPACE);
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        setIncludeFontPadding(false);
        setLineSpacing(0f, 1.0f);
        setBackgroundColor(Color.TRANSPARENT);
        setTextColor(0xFFE4E4E7);
        setHorizontallyScrolling(false);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);

        int pad = (int) dp(ctx, 12);
        int padV = (int) dp(ctx, 6);
        setPadding(pad, padV, pad, padV);
    }

    /**
     * Mede o EditText com altura IRRESTRITA — assim ele sempre tem
     * o Layout completo, com TODAS as linhas do texto.
     */
    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        int heightUnspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        super.onMeasure(widthSpec, heightUnspecified);
    }

    private float dp(Context ctx, int v) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                ctx.getResources().getDisplayMetrics());
    }
}