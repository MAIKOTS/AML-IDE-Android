package globalclass.settings;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LogSettings {

    private static final String NOME_PASTA_LOGS = "logs";
    private static final String ARQUIVO_LOG_SISTEMA = "app_system.log";
    private static final String ARQUIVO_LOG_COMPILADOR = "compiler_output.log";

    private static final ExecutorService logExecutor = Executors.newSingleThreadExecutor();

    public enum TipoLog {
        SISTEMA,
        COMPILADOR
    }

    public static void registrar(Context context, TipoLog tipo, String tag, String mensagem) {
        if (context == null || mensagem == null) return;

        Log.d(tag, mensagem);

        // Captura o contexto da aplicação para evitar leak de memória da Activity
        final Context appContext = context.getApplicationContext();

        logExecutor.execute(() -> {
            File pastaLogs = new File(appContext.getExternalFilesDir(null), NOME_PASTA_LOGS);
            if (!pastaLogs.exists() && !pastaLogs.mkdirs()) {
                return;
            }

            String nomeArquivo = (tipo == TipoLog.COMPILADOR) ? ARQUIVO_LOG_COMPILADOR : ARQUIVO_LOG_SISTEMA;
            File arquivoLog = new File(pastaLogs, nomeArquivo);

            String dataHora = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());
            String linhaFormatada = String.format("[%s] [%s] %s: %s\n", dataHora, tipo.name(), tag, mensagem);

            // Uso do try-with-resources com BufferedWriter para maior desempenho
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(arquivoLog, true))) {
                writer.write(linhaFormatada);
            } catch (IOException e) {
                Log.e("LogSettings", "Erro ao escrever no arquivo de log: " + e.getMessage());
            }
        });
    }

    public static void limparLogs(Context context) {
        if (context == null) return;

        final Context appContext = context.getApplicationContext();

        logExecutor.execute(() -> {
            File pastaLogs = new File(appContext.getExternalFilesDir(null), NOME_PASTA_LOGS);
            if (pastaLogs.exists() && pastaLogs.isDirectory()) {
                File[] arquivos = pastaLogs.listFiles();
                if (arquivos != null) {
                    for (File file : arquivos) {
                        file.delete();
                    }
                }
            }
        });
    }
}