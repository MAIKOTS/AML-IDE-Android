package ui.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ui.componentes.ConsoleTerminal;

/**
 * Gerenciador centralizado de logs da IDE.
 *
 * Responsabilidades:
 *  - Formatar mensagens com nível + tag + timestamp
 *  - Enviar pro ConsoleTerminal (visual)
 *  - Salvar TUDO em arquivo (files/logs/ide.log)
 *
 * Níveis:
 *   DEBUG — detalhes técnicos (paths, args, env)
 *   INFO  — informação normal
 *   OK    — operação bem-sucedida
 *   AVISO — atenção
 *   ERRO  — falha
 *
 * Uso:
 *   GerenciadorLogsConsole log = new GerenciadorLogsConsole(ctx, console);
 *   log.secao("COMPILAÇÃO");
 *   log.info("Clang", "Caminho: " + caminho);
 *   log.comando("Exec", args);
 *   log.bloco("Saída", saidaCompleta);
 *   log.ok("Compilador", "Tudo pronto");
 */
public final class GerenciadorLogsConsole {

    private static final String TAG = "GerenciadorLogs";
    private static final String ARQUIVO_LOG = "logs/ide.log";
    private static final long TAMANHO_MAX_ARQUIVO = 4L * 1024 * 1024; // 4 MB

    private final ConsoleTerminal console;
    private final File arquivoLog;

    private final SimpleDateFormat fmtHora = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private final SimpleDateFormat fmtData = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    private boolean salvarEmArquivo = true;
    private boolean nivelDebug = true;   // pode desligar pra menos ruído

    public GerenciadorLogsConsole(Context ctx, ConsoleTerminal console) {
        this.console = console;
        this.arquivoLog = new File(ctx.getFilesDir(), ARQUIVO_LOG);
        File pasta = arquivoLog.getParentFile();
        if (pasta != null && !pasta.exists()) pasta.mkdirs();
    }

    // ==========================================================
    //  Níveis básicos
    // ==========================================================

    public void debug(String tag, String msg) {
        if (!nivelDebug) return;
        linha("DEBUG", tag, msg);
    }

    public void info(String tag, String msg) {
        linha("INFO", tag, msg);
    }

    public void ok(String tag, String msg) {
        linha("OK", tag, msg);
    }

    public void aviso(String tag, String msg) {
        linha("AVISO", tag, msg);
    }

    public void erro(String tag, String msg) {
        linha("ERRO", tag, msg);
    }

    // ==========================================================
    //  Seção (cabeçalho destacado)
    // ==========================================================

    public void secao(String titulo) {
        if (console != null) console.secao(titulo);
        salvar("SECAO", "", "── " + titulo + " ──");
    }

    // ==========================================================
    //  Comando (args em lista, um por linha)
    // ==========================================================

    public void comando(String tag, List<String> args) {
        if (args == null || args.isEmpty()) return;

        linha("DEBUG", tag, "Comando (" + args.size() + " args):");
        for (int i = 0; i < args.size(); i++) {
            linha("DEBUG", tag, "  [" + padLeft(String.valueOf(i), 2) + "] " + args.get(i));
        }

        // Também uma linha "colável" completa
        StringBuilder sb = new StringBuilder();
        for (String a : args) sb.append(a).append(" ");
        linha("DEBUG", tag, "Comando completo: " + sb.toString().trim());
    }

    // ==========================================================
    //  Bloco multi-linha
    // ==========================================================

    /** Envia um texto multilinha, cada linha formatada. */
    public void bloco(String tag, String texto) {
        if (texto == null || texto.isEmpty()) return;
        String[] linhas = texto.split("\n");
        for (String l : linhas) {
            linha("INFO", tag, l);
        }
    }

    /** Saída crua do linker/compilador (sem formatação). */
    public void saidaCrua(String texto) {
        if (texto == null || texto.isEmpty()) return;
        String[] linhas = texto.split("\n");
        for (String l : linhas) {
            if (console != null) console.cru(l);
        }
        salvar("RAW", "", texto);
    }

    // ==========================================================
    //  Sucesso / Falha (com duração)
    // ==========================================================

    public void sucesso(String titulo, long duracaoMs) {
        if (console != null) {
            console.sucesso(titulo + "  (" + formatarDuracao(duracaoMs) + ")");
        }
        salvar("OK", "", titulo + " em " + duracaoMs + "ms");
    }

    public void falha(String titulo, long duracaoMs, String motivo) {
        if (console != null) {
            console.erro(titulo + "  (" + formatarDuracao(duracaoMs) + ")");
        }
        salvar("ERRO", "", titulo + " após " + duracaoMs + "ms — " + motivo);
    }

    // ==========================================================
    //  Utilitários para o compilador
    // ==========================================================

    /** Formata tamanho de arquivo com 2 casas. */
    public static String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /** Formata duração de forma legível. */
    public static String formatarDuracao(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format(Locale.getDefault(), "%.2fs", ms / 1000.0);
        long min = ms / 60_000;
        long seg = (ms % 60_000) / 1000;
        return min + "m " + seg + "s";
    }

    // ==========================================================
    //  Núcleo
    // ==========================================================

    private void linha(String nivel, String tag, String msg) {
        String tagFmt = (tag == null || tag.isEmpty())
                ? ""
                : "[" + padRight(tag, 8) + "] ";

        String prefixo = "[" + fmtHora.format(new Date()) + "] "
                + "[" + padRight(nivel, 5) + "] "
                + tagFmt;

        if (console != null) {
            String linhaCompleta = prefixo + msg;
            switch (nivel) {
                case "OK":    console.ok(linhaCompleta);    break;
                case "ERRO":  console.erro(linhaCompleta);  break;
                case "AVISO": console.aviso(linhaCompleta); break;
                case "DEBUG": console.dim(linhaCompleta);   break;
                default:      console.info(linhaCompleta);  break;
            }
        }

        if (salvarEmArquivo) {
            salvar(nivel, tag, msg);
        }
    }

    private void salvar(String nivel, String tag, String msg) {
        try {
            File pai = arquivoLog.getParentFile();
            if (pai != null && !pai.exists()) pai.mkdirs();

            // Rotaciona se passar do limite
            if (arquivoLog.exists() && arquivoLog.length() > TAMANHO_MAX_ARQUIVO) {
                File bkp = new File(arquivoLog.getParentFile(), "ide.log.old");
                if (bkp.exists()) bkp.delete();
                arquivoLog.renameTo(bkp);
            }

            String linha = "[" + fmtData.format(new Date()) + "] "
                    + "[" + padRight(nivel, 5) + "] "
                    + "[" + padRight(tag == null ? "" : tag, 8) + "] "
                    + msg + "\n";

            try (FileWriter fw = new FileWriter(arquivoLog, true)) {
                fw.write(linha);
            }
        } catch (IOException e) {
            Log.w(TAG, "Erro salvando log: " + e.getMessage());
        }
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    private String padLeft(String s, int n) {
        if (s == null) s = "";
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder();
        while (sb.length() < n - s.length()) sb.append(' ');
        sb.append(s);
        return sb.toString();
    }

    // ==========================================================
    //  Configuração
    // ==========================================================

    public void setSalvarEmArquivo(boolean v) { this.salvarEmArquivo = v; }
    public void setNivelDebug(boolean v)       { this.nivelDebug = v; }

    public File obterArquivoLog() { return arquivoLog; }

    public void limparArquivo() {
        if (arquivoLog.exists()) arquivoLog.delete();
    }
}