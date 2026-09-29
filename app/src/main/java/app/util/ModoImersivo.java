package app.util;

import android.app.Activity;
import android.os.Build;
import android.view.View;

/**
 * Aplica o modo imersivo (tela cheia sem barras do sistema).
 */
public final class ModoImersivo {

    private ModoImersivo() { }

    public static void aplicar(Activity atividade) {
        if (atividade == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            atividade.getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}