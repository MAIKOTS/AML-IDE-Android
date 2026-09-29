package globalclass.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Sistema de log do app.
 *
 * Uso:
 *   LogApp.inicializar(this);              // 1x no onCreate
 *   LogApp.escrever("mensagem");           // ativo por padrão
 *   LogApp.escrever("msg", false);         // silencioso (ignora)
 *   LogApp.e("algo deu errado");           // atalho de nível
 *
 * Arquivo: <externalFilesDir>/logs/logs_Aml-Ide.log
 */
public final class LogApp {

    // ==========================================================
    //  Configuração (constantes)
    // ==========================================================
    private static final String TAG             = "LogApp";
    private static final String NOME_PASTA_LOGS = "logs";
    private static final String NOME_ARQUIVO    = "logs_Aml-Ide.log";
    private static final String AUTOR           = "MaikoTS";

    private static final int LARGURA_SEPARADOR = 72;

    private static final SimpleDateFormat FMT_DATA =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
    private static final SimpleDateFormat FMT_CRIACAO =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    // ==========================================================
    //  Estado interno
    // ==========================================================
    private static Context appContext;
    private static File arquivoLog;
    private static int contador = 0;
    private static boolean inicializado = false;

    /** Informações do ambiente, preenchidas no `inicializar()`. */
    private static InfoAmbiente info;

    private static final Object LOCK = new Object();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor();

    // ==========================================================
    //  Níveis de log
    // ==========================================================
    public enum Nivel {
        INFO("INFO"),
        AVISO("AVISO"),
        ERRO("ERRO"),
        DEBUG("DEBUG"),
        COMPILADOR("COMPILADOR");

        private final String rotulo;
        Nivel(String rotulo) { this.rotulo = rotulo; }
        public String getRotulo() { return rotulo; }
    }

    // ==========================================================
    //  Estrutura com as informações extraídas do sistema
    // ==========================================================
    private static class InfoAmbiente {
        String nomeApp       = "Desconhecido";
        String pacote        = "Desconhecido";
        String versaoNome    = "?";
        long   versaoCodigo  = 0;
        boolean ehDebug      = false;
        String fabricante    = "Desconhecido";
        String modelo        = "Desconhecido";
        String marca         = "Desconhecido";
        String hardware      = "Desconhecido";
        String androidVer    = "Desconhecido";
        int    androidApi    = 0;
        String abi           = "Desconhecido";
        String[] abisSuportadas = new String[0];
        String idioma        = "Desconhecido";
        String densidade     = "Desconhecida";
        int    larguraPx     = 0;
        int    alturaPx      = 0;
        int    ramTotalMb    = 0;
        String fingerprint   = "Desconhecido";
    }

    private LogApp() { }

    // ==========================================================
    //  Inicialização
    // ==========================================================

    /**
     * Chame UMA vez no onCreate da MainActivity.
     * Depois disso, pode usar LogApp.escrever(...) em qualquer lugar.
     */
    public static void inicializar(Context context) {
        if (context == null || inicializado) return;
        appContext = context.getApplicationContext();
        inicializado = true;

        // Extrai todas as informações na thread principal (PackageManager
        // não é thread-safe em background em algumas versões)
        info = extrairInfo(appContext);

        EXEC.execute(() -> {
            try {
                File pastaLogs = new File(appContext.getExternalFilesDir(null), NOME_PASTA_LOGS);
                if (!pastaLogs.exists() && !pastaLogs.mkdirs()) {
                    Log.e(TAG, "Não foi possível criar pasta de logs.");
                    return;
                }

                arquivoLog = new File(pastaLogs, NOME_ARQUIVO);

                if (!arquivoLog.exists()) {
                    criarCabecalho(arquivoLog);
                } else {
                    contador = contarLinhasDeLog(arquivoLog);
                }
            } catch (Exception e) {
                Log.e(TAG, "Erro na inicialização do log.", e);
            }
        });
    }

    // ==========================================================
    //  Extração de informações do ambiente
    // ==========================================================

    private static InfoAmbiente extrairInfo(Context ctx) {
        InfoAmbiente i = new InfoAmbiente();

        // -------- App --------
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo appInfo = ctx.getApplicationInfo();

            // Nome (do AndroidManifest/android:label)
            i.nomeApp = pm.getApplicationLabel(appInfo).toString();
            i.pacote  = ctx.getPackageName();

            PackageInfo pkg = pm.getPackageInfo(ctx.getPackageName(), 0);
            i.versaoNome   = pkg.versionName != null ? pkg.versionName : "?";
            i.versaoCodigo = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    ? pkg.getLongVersionCode()
                    : pkg.versionCode;

            // App debugável ou release?
            i.ehDebug = (appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;

        } catch (Exception e) {
            Log.w(TAG, "Falha ao obter info do app: " + e.getMessage());
        }

        // -------- Dispositivo --------
        i.fabricante = safe(Build.MANUFACTURER);
        i.modelo     = safe(Build.MODEL);
        i.marca      = safe(Build.BRAND);
        i.hardware   = safe(Build.HARDWARE);
        i.fingerprint = safe(Build.FINGERPRINT);

        // -------- Sistema --------
        i.androidVer = safe(Build.VERSION.RELEASE);
        i.androidApi = Build.VERSION.SDK_INT;

        // -------- ABI --------
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                i.abisSuportadas = Build.SUPPORTED_ABIS;
                if (i.abisSuportadas.length > 0) i.abi = i.abisSuportadas[0];
            } else {
                i.abi = safe(Build.CPU_ABI);
                i.abisSuportadas = new String[] { i.abi };
            }
        } catch (Exception e) {
            i.abi = "?";
        }

        // -------- Idioma --------
        try {
            Locale loc = Locale.getDefault();
            i.idioma = loc.getLanguage() + "-" + loc.getCountry();
        } catch (Exception e) {
            i.idioma = "?";
        }

        // -------- Tela --------
        try {
            DisplayMetrics dm = ctx.getResources().getDisplayMetrics();
            i.larguraPx = dm.widthPixels;
            i.alturaPx  = dm.heightPixels;
            i.densidade = dm.densityDpi + " dpi (x" + dm.density + ")";
        } catch (Exception e) {
            i.densidade = "?";
        }

        // -------- RAM --------
        try {
            ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(mi);
                i.ramTotalMb = (int) (mi.totalMem / (1024 * 1024));
            }
        } catch (Exception e) {
            i.ramTotalMb = 0;
        }

        return i;
    }

    private static String safe(String s) {
        return (s == null || s.isEmpty()) ? "Desconhecido" : s;
    }

    // ==========================================================
    //  API principal — UMA LINHA
    // ==========================================================

    public static void escrever(String mensagem) {
        escrever(mensagem, Nivel.INFO, true);
    }

    public static void escrever(String mensagem, boolean ativar) {
        escrever(mensagem, Nivel.INFO, ativar);
    }

    public static void escrever(String mensagem, Nivel nivel) {
        escrever(mensagem, nivel, true);
    }

    public static void escrever(String mensagem, Nivel nivel, boolean ativar) {
        if (!ativar) return;
        if (mensagem == null || !inicializado) return;
        if (nivel == null) nivel = Nivel.INFO;

        switch (nivel) {
            case ERRO:  Log.e(TAG, mensagem); break;
            case AVISO: Log.w(TAG, mensagem); break;
            case DEBUG: Log.d(TAG, mensagem); break;
            default:    Log.i(TAG, mensagem); break;
        }

        final String msg = mensagem;
        final Nivel nvl = nivel;

        EXEC.execute(() -> {
            synchronized (LOCK) {
                try {
                    if (arquivoLog == null) {
                        File pastaLogs = new File(appContext.getExternalFilesDir(null), NOME_PASTA_LOGS);
                        pastaLogs.mkdirs();
                        arquivoLog = new File(pastaLogs, NOME_ARQUIVO);
                    }

                    contador++;

                    String linha = String.format(
                            "[%04d] [%s] [%-10s] %s%n",
                            contador,
                            FMT_DATA.format(new Date()),
                            nvl.getRotulo(),
                            msg);

                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivoLog, true))) {
                        bw.write(linha);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Erro ao escrever no arquivo de log.", e);
                }
            }
        });
    }

    // ==========================================================
    //  Atalhos
    // ==========================================================
    public static void i(String msg) { escrever(msg, Nivel.INFO); }
    public static void w(String msg) { escrever(msg, Nivel.AVISO); }
    public static void e(String msg) { escrever(msg, Nivel.ERRO); }
    public static void d(String msg) { escrever(msg, Nivel.DEBUG); }
    public static void c(String msg) { escrever(msg, Nivel.COMPILADOR); }

    // ==========================================================
    //  Cabeçalho
    // ==========================================================

    private static void criarCabecalho(File arquivo) {
        String sep = repetir('=', LARGURA_SEPARADOR);
        String agora = FMT_CRIACAO.format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append(sep).append("\n");
        sb.append("  LOG - ").append(info.nomeApp).append("\n");
        sb.append(sep).append("\n");
        sb.append("  Arquivo        : ").append(NOME_ARQUIVO).append("\n");
        sb.append("  Criado em      : ").append(agora).append("\n");
        sb.append("  Autor          : ").append(AUTOR).append("\n");
        sb.append("\n");
        sb.append("  ─── APLICATIVO ─────────────────────────────────\n");
        sb.append("  Nome           : ").append(info.nomeApp).append("\n");
        sb.append("  Pacote         : ").append(info.pacote).append("\n");
        sb.append("  Versão         : ").append(info.versaoNome)
          .append(" (código ").append(info.versaoCodigo).append(")\n");
        sb.append("  Build          : ").append(info.ehDebug ? "DEBUG" : "RELEASE").append("\n");
        sb.append("\n");
        sb.append("  ─── DISPOSITIVO ────────────────────────────────\n");
        sb.append("  Fabricante     : ").append(info.fabricante).append("\n");
        sb.append("  Marca          : ").append(info.marca).append("\n");
        sb.append("  Modelo         : ").append(info.modelo).append("\n");
        sb.append("  Hardware       : ").append(info.hardware).append("\n");
        sb.append("  ABI principal  : ").append(info.abi).append("\n");
        sb.append("  ABIs suportadas: ").append(String.join(", ", info.abisSuportadas)).append("\n");
        sb.append("\n");
        sb.append("  ─── SISTEMA ────────────────────────────────────\n");
        sb.append("  Android        : ").append(info.androidVer)
          .append(" (API ").append(info.androidApi).append(")\n");
        sb.append("  Idioma         : ").append(info.idioma).append("\n");
        sb.append("  Tela           : ").append(info.larguraPx)
          .append(" x ").append(info.alturaPx).append(" px\n");
        sb.append("  Densidade      : ").append(info.densidade).append("\n");
        if (info.ramTotalMb > 0) {
            sb.append("  RAM total      : ").append(info.ramTotalMb).append(" MB\n");
        }
        sb.append("  Fingerprint    : ").append(info.fingerprint).append("\n");
        sb.append(sep).append("\n\n");

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(arquivo, false))) {
            bw.write(sb.toString());
        } catch (IOException e) {
            Log.e(TAG, "Erro ao criar cabeçalho.", e);
        }

        contador = 0;
    }

    // ==========================================================
    //  Contagem de linhas existentes
    // ==========================================================

    private static int contarLinhasDeLog(File arquivo) {
        int total = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(arquivo))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                if (linha.length() > 5
                        && linha.charAt(0) == '['
                        && linha.charAt(5) == ']') {
                    total++;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Erro ao contar linhas.", e);
        }
        return total;
    }

    // ==========================================================
    //  Utilitários públicos
    // ==========================================================

    public static File obterArquivo(Context context) {
        if (context == null) return null;
        File pastaLogs = new File(context.getExternalFilesDir(null), NOME_PASTA_LOGS);
        return new File(pastaLogs, NOME_ARQUIVO);
    }

    public static void limpar(Context context) {
        if (context == null || !inicializado) return;
        EXEC.execute(() -> {
            synchronized (LOCK) {
                if (arquivoLog != null && arquivoLog.exists()) {
                    arquivoLog.delete();
                }
                criarCabecalho(arquivoLog);
            }
        });
    }

    // ==========================================================
    //  Helper privado
    // ==========================================================

    private static String repetir(char c, int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }
}