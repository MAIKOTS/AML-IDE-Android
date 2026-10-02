package ui.componentes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Mini-terminal visual.
 *
 * ★ Thread-safe: pode ser chamado de qualquer thread.
 *   Se não estiver no UI thread, as alterações são postadas automaticamente.
 */
public class ConsoleTerminal extends ScrollView {

    // ---------- Paleta ----------
    private static final int COR_INFO       = 0xFFF4F4F5;
    private static final int COR_OK         = 0xFF00E676;
    private static final int COR_ERRO       = 0xFFFF5252;
    private static final int COR_AVISO      = 0xFFFFC107;
    private static final int COR_PROGRESSO  = 0xFF60A5FA;
    private static final int COR_DIM        = 0xFF6B7280;
    private static final int COR_TIMESTAMP  = 0xFF52525B;
    private static final int COR_PROMPT     = 0xFF00E676;

    private final LinearLayout container;
    private final SimpleDateFormat fmtHora =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private boolean mostrarTimestamp = true;

    public ConsoleTerminal(Context ctx) {
        this(ctx, null);
    }

    public ConsoleTerminal(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        setBackgroundColor(0xFF0A0A0C);
        setVerticalScrollBarEnabled(true);
        int pad = dp(ctx, 12);
        setPadding(pad, pad, pad, pad);

        container = new LinearLayout(ctx);
        container.setOrientation(LinearLayout.VERTICAL);
        addView(container);

        appendInterno("$ ", COR_PROMPT);
        appendInterno("console pronto\n", COR_DIM);
    }

    // ==========================================================
    //  API pública — todas thread-safe
    // ==========================================================

    public void setMostrarTimestamp(boolean mostrar) {
        this.mostrarTimestamp = mostrar;
    }

    public void info(final String texto) {
        executarNoUi(() -> linhaInterno("  ", texto, COR_INFO));
    }

    public void ok(final String texto) {
        executarNoUi(() -> linhaInterno("✓ ", texto, COR_OK));
    }

    public void erro(final String texto) {
        executarNoUi(() -> linhaInterno("✗ ", texto, COR_ERRO));
    }

    public void aviso(final String texto) {
        executarNoUi(() -> linhaInterno("! ", texto, COR_AVISO));
    }

    public void progresso(final int pct) {
        executarNoUi(() -> {
            int largura = 24;
            int cheios = Math.max(0, Math.min(largura, pct * largura / 100));
            StringBuilder barra = new StringBuilder();
            barra.append('[');
            for (int i = 0; i < largura; i++) {
                barra.append(i < cheios ? '█' : '░');
            }
            barra.append("] ").append(pct).append('%');
            linhaInterno("  ", barra.toString(), COR_PROGRESSO);
        });
    }

    public void sucesso(final String texto) {
        executarNoUi(() -> {
            appendInterno("\n", COR_OK);
            linhaInterno("★ ", texto, COR_OK);
        });
    }

    public void secao(final String titulo) {
        executarNoUi(() -> {
            appendInterno("\n", COR_DIM);
            linhaInterno("── ", titulo, COR_PROGRESSO);
        });
    }

    public void dim(final String texto) {
        executarNoUi(() -> linhaInterno("  ", texto, COR_DIM));
    }

    /** Linha sem prefixo — saída crua (ex: stderr do linker). */
    public void cru(final String texto) {
        if (texto == null) return;
        executarNoUi(() -> {
            appendInterno(texto, COR_INFO);
            appendInterno("\n", COR_INFO);
        });
    }

    public void limpar() {
        executarNoUi(() -> container.removeAllViews());
    }

    public void arvore(final java.io.File pasta,
                       final String prefixo,
                       final int profundidadeMax) {
        executarNoUi(() -> arvoreInterno(pasta, prefixo, profundidadeMax));
    }

    // ==========================================================
    //  Internos — SEMPRE no UI thread
    // ==========================================================

    private void arvoreInterno(java.io.File pasta, String prefixo, int profundidadeMax) {
        if (pasta == null || !pasta.exists()) {
            linhaInterno("✗ ", "Pasta não existe: " + pasta, COR_ERRO);
            return;
        }
        if (profundidadeMax <= 0) return;

        java.io.File[] filhos = pasta.listFiles();
        if (filhos == null) return;

        java.util.Arrays.sort(filhos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (int i = 0; i < filhos.length; i++) {
            java.io.File f = filhos[i];
            boolean ultimo = (i == filhos.length - 1);
            String conector = ultimo ? "└── " : "├── ";
            String simbolo = f.isDirectory() ? "📁 " : iconeArquivo(f.getName());
            int cor = f.isDirectory() ? COR_PROGRESSO : COR_INFO;

            linhaInterno(prefixo + conector, simbolo + f.getName(), cor);

            if (f.isDirectory()) {
                String novoPrefixo = prefixo + (ultimo ? "    " : "│   ");
                arvoreInterno(f, novoPrefixo, profundidadeMax - 1);
            }
        }
    }

    private void linhaInterno(String prefixo, String texto, int cor) {
        appendInterno(prefixo, cor);
        appendInterno(texto, cor);
        appendInterno("\n", cor);
    }

    private void appendInterno(String s, int cor) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        if (mostrarTimestamp && s.length() > 1 && !s.equals("\n")) {
            String hora = fmtHora.format(new Date());
            String prefixoHora = "[" + hora + "] ";
            int start = sb.length();
            sb.append(prefixoHora);
            sb.setSpan(new ForegroundColorSpan(COR_TIMESTAMP),
                    start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        int start = sb.length();
        sb.append(s);
        sb.setSpan(new ForegroundColorSpan(cor),
                start, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView tv = new TextView(getContext());
        tv.setText(sb);
        tv.setTextSize(11);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setGravity(Gravity.START);
        container.addView(tv);

        post(() -> fullScroll(FOCUS_DOWN));
    }

    /**
     * Executa o Runnable no UI thread.
     * Se já estamos nele, roda direto; senão, posta.
     */
    private void executarNoUi(Runnable r) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            r.run();
        } else {
            post(r);
        }
    }

    private String iconeArquivo(String nome) {
        if (nome.endsWith(".so")) return "🔧 ";
        if (nome.endsWith(".h") || nome.endsWith(".hpp")) return "📜 ";
        if (nome.endsWith(".cpp") || nome.endsWith(".c")) return "📄 ";
        if (nome.equals("clang") || nome.equals("clang++")) return "⚙ ";
        return "· ";
    }

    private static int dp(Context ctx, int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}