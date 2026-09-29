package ui.explorador;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter que exibe uma lista achatada de nós com indentação por nível.
 * Usa ListView + BaseAdapter (100% nativo).
 */
public class AdapterArvore extends BaseAdapter {

    // ---------- Paleta ----------
    private static final int COR_TEXTO        = 0xFFF4F4F5;
    private static final int COR_TEXTO_DIM    = 0xFFA1A1AA;
    private static final int COR_TEXTO_MINI   = 0xFF6B7280;
    private static final int COR_BG_ITEM      = 0xFF0F0F12;
    private static final int COR_BG_ITEM_ALT  = 0xFF0C0C0E;

    public interface Acoes {
        void aoClicar(NoExploradorFiles no);
        void aoClicarLongo(NoExploradorFiles no);
    }

    private final Context ctx;
    private final Acoes acoes;

    private final List<NoExploradorFiles> visiveis = new ArrayList<>();
    private NoExploradorFiles raiz;

    public AdapterArvore(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  API pública
    // ==========================================================

    public void setRaiz(NoExploradorFiles novaRaiz) {
        this.raiz = novaRaiz;
        reconstruirListaVisivel();
        notifyDataSetChanged();
    }

    public NoExploradorFiles getRaiz() {
        return raiz;
    }

    public void alternar(NoExploradorFiles no) {
        if (no == null || !no.isPasta()) return;

        if (no.isExpandido()) {
            no.setExpandido(false);
        } else {
            if (no.getFilhos().isEmpty()) {
                carregarFilhos(no);
            }
            no.setExpandido(true);
        }

        reconstruirListaVisivel();
        notifyDataSetChanged();
    }

    public void recarregar() {
        if (raiz == null) return;
        carregarFilhos(raiz);
        reconstruirListaVisivel();
        notifyDataSetChanged();
    }

    // ==========================================================
    //  BaseAdapter
    // ==========================================================

    @Override
    public int getCount() { return visiveis.size(); }

    @Override
    public Object getItem(int pos) { return visiveis.get(pos); }

    @Override
    public long getItemId(int pos) { return pos; }

    @Override
    public View getView(int pos, View convertView, ViewGroup parent) {
        NoExploradorFiles no = visiveis.get(pos);

        // Reaproveita a view se possível (padrão ViewHolder simplificado)
        LinearLayout linha;
        if (convertView instanceof LinearLayout) {
            linha = (LinearLayout) convertView;
        } else {
            linha = criarLinha();
        }

        // Pega as referências (tags)
        TextView seta    = (TextView) linha.getChildAt(0);
        TextView icone   = (TextView) linha.getChildAt(1);
        LinearLayout col = (LinearLayout) linha.getChildAt(2);
        TextView nome    = (TextView) col.getChildAt(0);
        TextView detalhe = (TextView) col.getChildAt(1);

        // ---- Indentação por nível ----
        int indentPx = dp(16) * no.getNivel();
        linha.setPadding(dp(12) + indentPx, dp(10), dp(12), dp(10));

        // ---- Fundo alternado ----
        linha.setBackgroundColor(pos % 2 == 0 ? COR_BG_ITEM : COR_BG_ITEM_ALT);

        // ---- Ícone de expansão ----
        if (no.isPasta()) {
            seta.setVisibility(View.VISIBLE);
            seta.setText(no.isExpandido() ? "▼" : "▶");
            seta.setTextColor(COR_TEXTO_DIM);
        } else {
            seta.setVisibility(View.INVISIBLE);
        }

        // ---- Emoji / ícone ----
        icone.setText(iconePara(no));

        // ---- Nome ----
        nome.setText(no.getNome());
        nome.setTextColor(no.isPasta() ? COR_TEXTO : COR_TEXTO_DIM);
        nome.setTypeface(Typeface.DEFAULT,
                no.isPasta() ? Typeface.BOLD : Typeface.NORMAL);

        // ---- Detalhe ----
        if (no.isPasta()) {
            int qtd = no.getTotalFilhos();
            detalhe.setText(qtd == 0 ? "" : qtd + " itens");
        } else {
            detalhe.setText(formatarTamanho(no.getTamanho()));
        }
        detalhe.setTextColor(COR_TEXTO_MINI);

        // ---- Clique ----
        final NoExploradorFiles noFinal = no;
        linha.setOnClickListener(v -> {
            if (acoes != null) acoes.aoClicar(noFinal);
        });
        linha.setOnLongClickListener(v -> {
            if (acoes != null) acoes.aoClicarLongo(noFinal);
            return true;
        });

        return linha;
    }

    // ==========================================================
    //  Internos
    // ==========================================================

    private LinearLayout criarLinha() {
        LinearLayout linha = new LinearLayout(ctx);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        linha.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Seta
        TextView seta = new TextView(ctx);
        seta.setTextSize(10);
        seta.setPadding(0, 0, dp(8), 0);
        linha.addView(seta);

        // Ícone
        TextView icone = new TextView(ctx);
        icone.setTextSize(14);
        icone.setPadding(0, 0, dp(10), 0);
        linha.addView(icone);

        // Coluna (nome + detalhe)
        LinearLayout coluna = new LinearLayout(ctx);
        coluna.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        coluna.setLayoutParams(lpCol);
        linha.addView(coluna);

        // Nome
        TextView nome = new TextView(ctx);
        nome.setTextSize(13);
        coluna.addView(nome);

        // Detalhe
        TextView detalhe = new TextView(ctx);
        detalhe.setTextSize(10);
        detalhe.setPadding(0, dp(2), 0, 0);
        coluna.addView(detalhe);

        return linha;
    }

    private void carregarFilhos(NoExploradorFiles pai) {
        pai.getFilhos().clear();

        File[] filhos = pai.getArquivo().listFiles();
        if (filhos == null) return;

        java.util.Arrays.sort(filhos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File f : filhos) {
            pai.getFilhos().add(new NoExploradorFiles(f, pai.getNivel() + 1, pai));
        }
    }

    private void reconstruirListaVisivel() {
        visiveis.clear();
        if (raiz == null) return;

        visiveis.add(raiz);
        adicionarFilhosVisiveis(raiz);
    }

    private void adicionarFilhosVisiveis(NoExploradorFiles pai) {
        if (!pai.isExpandido()) return;
        for (NoExploradorFiles filho : pai.getFilhos()) {
            visiveis.add(filho);
            adicionarFilhosVisiveis(filho);
        }
    }

    private String iconePara(NoExploradorFiles no) {
        if (no.isPasta()) return "📁";
        String nome = no.getNome().toLowerCase();
        if (nome.endsWith(".so"))    return "🔧";
        if (nome.endsWith(".h")
         || nome.endsWith(".hpp"))   return "📜";
        if (nome.endsWith(".cpp")
         || nome.endsWith(".c")
         || nome.endsWith(".cc"))    return "📄";
        if (nome.endsWith(".txt")
         || nome.endsWith(".md"))    return "📝";
        if (nome.endsWith(".zip")
         || nome.endsWith(".tar")
         || nome.endsWith(".gz"))    return "📦";
        if (nome.equals("clang")
         || nome.equals("clang++"))  return "⚙";
        return "·";
    }

    private String formatarTamanho(long bytes) {
        if (bytes < 0) return "";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}