package app;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;

import app.controladores.ControladorApp;
import app.util.LibsNativas;
import app.util.ModoImersivo;
import app.util.Permissoes;
import globalclass.settings.AppGlobalSettings;
import globalclass.settings.LogApp;

/**
 * Activity principal.
 *
 * Responsabilidade: apenas o ciclo de vida do Android.
 * Toda a orquestração de UI fica no ControladorApp.
 */
public class MainActivity extends Activity {

    static {
        // Força o Android a extrair as libs para nativeLibraryDir/
        // ANTES de qualquer execução nativa.
        LibsNativas.carregar();
    }

    private ControladorApp controlador;

    @Override
    protected void onCreate(Bundle estadoInstancia) {
        super.onCreate(estadoInstancia);

        // ---- Bootstrap ----
        AppGlobalSettings.aplicarModoFullScreen(this);
        AppGlobalSettings.inicializar(this);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ModoImersivo.aplicar(this);

        LogApp.inicializar(this);
        LogApp.i("─────────────────────────────────");
        LogApp.i("AML-IDE inicializado com sucesso!");
        LogApp.i("─────────────────────────────────");

        Permissoes.solicitarArmazenamento(this);

        // ---- Cria e monta a UI ----
        controlador = new ControladorApp(this);
        setContentView(controlador.construirViewPrincipal());
    }

    @Override
    public void onBackPressed() {
        if (!controlador.aoVoltar()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean temFoco) {
        super.onWindowFocusChanged(temFoco);
        if (temFoco) {
            controlador.aoGanharFoco();
        }
    }
}