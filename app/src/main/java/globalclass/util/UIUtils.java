package globalclass.util;

import android.content.res.Resources;
import android.util.TypedValue;

public final class UIUtils {

    // Construtor privado para impedir instanciação da classe utilitária
    private UIUtils() {}

    /**
     * Converte DP (Density-independent Pixels) para Pixels (px).
     */
    public static int dpToPx(float dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Converte Pixels (px) para DP.
     */
    public static float pxToDp(float px) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return px / density;
    }

    /**
     * Converte SP (Scale-independent Pixels) para Pixels (px). Ideal para textos.
     */
    public static int spToPx(float sp) {
        float scaledDensity = Resources.getSystem().getDisplayMetrics().scaledDensity;
        return Math.round(sp * scaledDensity);
    }

    /**
     * Converte dimensões utilizando o TypedValue nativo do Android.
     * Exemplo de uso: UIUtils.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16);
     */
    public static int applyDimension(int unit, float value) {
        return Math.round(TypedValue.applyDimension(
                unit, 
                value, 
                Resources.getSystem().getDisplayMetrics()
        ));
    }
}
