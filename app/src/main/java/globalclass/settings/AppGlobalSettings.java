package globalclass.settings;

import android.app.Activity;
import android.content.Context;
import android.view.View;

public class AppGlobalSettings {

    /**
     * 🚀 INICIALIZAÇÃO GLOBAL DO APLICATIVO
     */
    public static void inicializar(Activity activity) {
        if (activity == null) return;

        // 1. Log de Inicialização do App
        logSistema(activity, "AppGlobalSettings", "Iniciando subsistemas do MKIDE...");

        // 2. Configurações de UI e Hardware
        UISettings.aplicarModoFullScreen(activity);
        UISettings.manterTelaLigada(activity, true);
        UISettings.otimizarRenderizacaoHardware(activity);

        logSistema(activity, "AppGlobalSettings", "Todos os subsistemas foram carregados com sucesso.");
    }

    // =======================================================
    // 🪵 MÉTODOS PÚBLICOS DE LOG
    // =======================================================

    public static void logSistema(Context context, String tag, String mensagem) {
        LogSettings.registrar(context, LogSettings.TipoLog.SISTEMA, tag, mensagem);
    }

    public static void logCompilador(Context context, String tag, String mensagem) {
        LogSettings.registrar(context, LogSettings.TipoLog.COMPILADOR, tag, mensagem);
    }

    public static void limparLogs(Context context) {
        LogSettings.limparLogs(context);
    }

    // =======================================================
    // ⚙️ MÉTODOS UTILITÁRIOS REPASSADOS
    // =======================================================

    public static void aplicarModoFullScreen(Activity activity) {
        UISettings.aplicarModoFullScreen(activity);
    }
}
