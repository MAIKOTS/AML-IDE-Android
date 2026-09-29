package ui.componentes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
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
 * Um mini-terminal visual: texto monoespaçado, cores por tipo de mensagem,
 * timestamp opcional e auto-scroll para a última linha.
 *
 * Uso:
 *   ConsoleTerminal console = new ConsoleTerminal(ctx);
 *   console.info("Iniciando...");
 *   console.ok("Instalado com sucesso!");
 *   console.erro("Falha ao abrir arquivo.");
 *   console.progresso(45);
 *   console.sucesso("Concluído em 12.4s");
 */
public class ConsoleTerminal extends ScrollView {

    // ---------- Paleta (mesma do design system) ----------
    private static final int COR_INFO       = 0xFFF4F4F5; // branco
    private static final int COR_OK         = 0xFF00E676; // verde destaque
    private static final int COR_ERRO       = 0xFFFF5252; // vermelho
    private static final int COR_AVISO      = 0xFFFFC107; // amarelo
    private static final int COR_PROGRESSO  = 0xFF60A5FA; // azul
    private static final int COR_DIM        = 0xFF6B7280; // cinza apagado
    private static final int COR_TIMESTAMP  = 0xFF52525B; // cinza escuro
    private static final int COR_PROMPT     = 0xFF00E676; // verde do prompt

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

        // Prompt inicial
        append("$ ", COR_PROMPT);
        append("console pronto\n", COR_DIM);
    }

    // ==========================================================
    //  API pública
    // ==========================================================

    public void setMostrarTimestamp(boolean mostrar) {
        this.mostrarTimestamp = mostrar;
    }

    /** Linha neutra. */
    public void info(String texto) {
        linha("  ", texto, COR_INFO);
    }

    /** Linha "OK" (sucesso). */
    public void ok(String texto) {
        linha("✓ ", texto, COR_OK);
    }

    /** Linha de erro. */
    public void erro(String texto) {
        linha("✗ ", texto, COR_ERRO);
    }

    /** Linha de aviso. */
    public void aviso(String texto) {
        linha("! ", texto, COR_AVISO);
    }

    /** Linha de progresso (com barra ASCII). */
    public void progresso(int pct) {
        int largura = 24;
        int cheios = Math.max(0, Math.min(largura, pct * largura / 100));
        StringBuilder barra = new StringBuilder();
        barra.append('[');
        for (int i = 0; i < largura; i++) {
            barra.append(i < cheios ? '█' : '░');
        }
        barra.append("] ").append(pct).append('%');
        linha("  ", barra.toString(), COR_PROGRESSO);
    }

    /** Linha destacada (ex: resultado final). */
    public void sucesso(String texto) {
        append("\n", COR_OK);
        linha("★ ", texto, COR_OK);
    }

    /** Cabeçalho de seção. */
    public void secao(String titulo) {
        append("\n", COR_DIM);
        linha("── ", titulo, COR_PROGRESSO);
    }

    /** Linha em cinza apagado. */
    public void dim(String texto) {
        linha("  ", texto, COR_DIM);
    }

    /** Mostra a árvore do NDK (indentação + emoji). */
    public void arvore(java.io.File pasta, String prefixo, int profundidadeMax) {
        if (pasta == null || !pasta.exists()) {
            erro("Pasta não existe: " + pasta);
            return;
        }
        if (profundidadeMax <= 0) return;

        java.io.File[] filhos = pasta.listFiles();
        if (filhos == null) return;

        // Ordena: pastas primeiro
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

            linha(prefixo + conector, simbolo + f.getName(), cor);

            if (f.isDirectory()) {
                String novoPrefixo = prefixo + (ultimo ? "    " : "│   ");
                arvore(f, novoPrefixo, profundidadeMax - 1);
            }
        }
    }

    /** Limpa o console. */
    public void limpar() {
        container.removeAllViews();
    }

    // ==========================================================
    //  Internos
    // ==========================================================

    private void linha(String prefixo, String texto, int cor) {
        append(prefixo, cor);
        append(texto, cor);
        append("\n", cor);
    }

    private void append(String s, int cor) {
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

        // Auto-scroll para o fim
        post(() -> fullScroll(FOCUS_DOWN));
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