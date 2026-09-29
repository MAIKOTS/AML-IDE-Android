package ui.estilos;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.view.View;

public class GerenciadorDeEstilos {

    // Cria fundo arredondado com efeito de toque
    public static Drawable criarFundoBotao(int corNormal, int corPressionado, int raioCanto) {
        // Formato base
        GradientDrawable formatoNormal = new GradientDrawable();
        formatoNormal.setShape(GradientDrawable.RECTANGLE);
        formatoNormal.setColor(corNormal);
        formatoNormal.setCornerRadius(raioCanto);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Efeito Ripple para Android 5.0+
            GradientDrawable mascara = new GradientDrawable();
            mascara.setShape(GradientDrawable.RECTANGLE);
            mascara.setColor(Color.WHITE);
            mascara.setCornerRadius(raioCanto);

            ColorStateList listaCores = ColorStateList.valueOf(corPressionado);
            return new RippleDrawable(listaCores, formatoNormal, mascara);
        } else {
            // Alternativa para versões antigas
            GradientDrawable formatoPressionado = new GradientDrawable();
            formatoPressionado.setShape(GradientDrawable.RECTANGLE);
            formatoPressionado.setColor(corPressionado);
            formatoPressionado.setCornerRadius(raioCanto);

            StateListDrawable estados = new StateListDrawable();
            estados.addState(new int[]{android.R.attr.state_pressed}, formatoPressionado);
            estados.addState(new int[]{}, formatoNormal);
            return estados;
        }
    }

    // Cria fundo de caixas com borda simples
    public static Drawable criarFundoCampoTextual(int corFundo, int corBorda, int larguraBorda, int raioCanto) {
        GradientDrawable formato = new GradientDrawable();
        formato.setShape(GradientDrawable.RECTANGLE);
        formato.setColor(corFundo);
        formato.setCornerRadius(raioCanto);
        if (larguraBorda > 0) {
            formato.setStroke(larguraBorda, corBorda);
        }
        return formato;
    }
}