package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import app.R;
import modelo.GerenciadorProjetoProperties;
import modelo.Projeto;

public class MontadorDeHome {

    public interface AcoesHome {
        void aoCliqueMenuHamburguer();
        void aoCliqueConfiguracoes();
        void aoCliqueCriarNovoProjeto();
        void aoCliqueImportarProjeto();
        void aoCliqueClonarGithub();
        void aoCliqueProjetoRecente(File pasta);
    }

    /** Quantos projetos mostrar no carrossel. */
    private static final int LIMITE_RECENTES = 10;

    /** Largura de cada card no carrossel (dp). */
    private static final int LARGURA_CARD_DP = 150;

    /** Debounce pra evitar re-scan em rajada. */
    private static final long INTERVALO_MINIMO_MS = 3000L;

    private final Context contexto;
    private final AcoesHome acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private HorizontalScrollView scrollRecentes;
    private LinearLayout containerRecentes;
    private LinearLayout vazioRecentes;

    private long ultimaAtualizacao = 0L;
    private boolean escaneando = false;
    private Runnable scanAgendado;

    public MontadorDeHome(Context contexto, AcoesHome acoes) {
        this.contexto = contexto;
        this.acoes = acoes;
    }

    public View construirLayout() {
        View layout = LayoutInflater.from(contexto)
                .inflate(R.layout.comp_inicio, null, false);

        View btnHomeMenu     = layout.findViewById(R.id.btn_home_menu);
        View btnHomeConfig   = layout.findViewById(R.id.btn_home_config);
        View btnNovoProjeto  = layout.findViewById(R.id.btn_novo_projeto);
        View btnAbrirProjeto = layout.findViewById(R.id.btn_abrir_projeto);
        View btnClonarGithub = layout.findViewById(R.id.btn_clonar_github);

        if (btnHomeMenu != null) {
            btnHomeMenu.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueMenuHamburguer();
            });
        }
        if (btnHomeConfig != null) {
            btnHomeConfig.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueConfiguracoes();
            });
        }
        if (btnNovoProjeto != null) {
            btnNovoProjeto.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueCriarNovoProjeto();
            });
        }
        if (btnAbrirProjeto != null) {
            btnAbrirProjeto.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueImportarProjeto();
            });
        }
        if (btnClonarGithub != null) {
            btnClonarGithub.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueClonarGithub();
            });
        }

        scrollRecentes    = layout.findViewById(R.id.home_recentes_scroll);
        containerRecentes = layout.findViewById(R.id.home_recentes_container);
        vazioRecentes     = layout.findViewById(R.id.home_recentes_vazio);

        ultimaAtualizacao = 0;
        carregarProjetosRecentes();

        return layout;
    }

    /** Recarrega a lista de recentes (com debounce). */
    public void atualizarRecentes() {
        if (containerRecentes == null) return;

        long agora = System.currentTimeMillis();
        if (agora - ultimaAtualizacao < INTERVALO_MINIMO_MS) {
            return;
        }

        if (scanAgendado != null) handlerUI.removeCallbacks(scanAgendado);
        scanAgendado = this::carregarProjetosRecentes;
        handlerUI.postDelayed(scanAgendado, 150);
    }

    // ==========================================================
    //  Dados prontos pra render
    // ==========================================================

    private static class ItemRecente {
        File   pasta;
        String descricao;   // "v1.0.0 · 3 fontes"
        String tempo;       // "agora", "2h"
        int    totalAbis;   // pra mostrar "2 ABIs" se quiser
    }

    // ==========================================================
    //  Scan + render
    // ==========================================================

    private void carregarProjetosRecentes() {
        if (containerRecentes == null || vazioRecentes == null) return;
        if (escaneando) return;
        escaneando = true;

        new Thread(() -> {

            File pastaProjetos = new File(
                    Environment.getExternalStorageDirectory(),
                    "AML_IDE/Projetos");

            if (!pastaProjetos.isDirectory()) {
                handlerUI.post(() -> mostrarVazio());
                return;
            }

            File[] arquivos = pastaProjetos.listFiles();
            if (arquivos == null) arquivos = new File[0];

            List<File> projetos = new ArrayList<>();
            for (File f : arquivos) {
                if (f.isDirectory() && !f.getName().startsWith(".")) {
                    projetos.add(f);
                }
            }

            if (projetos.isEmpty()) {
                handlerUI.post(() -> mostrarVazio());
                return;
            }

            projetos.sort((a, b) -> Long.compare(
                    obterUltimaModificacao(b),
                    obterUltimaModificacao(a)));

            int limite = Math.min(LIMITE_RECENTES, projetos.size());
            List<ItemRecente> itens = new ArrayList<>();
            for (int i = 0; i < limite; i++) {
                File p = projetos.get(i);
                ItemRecente item = new ItemRecente();
                item.pasta     = p;
                item.descricao = descreverProjeto(p);
                item.tempo     = tempoAtras(obterUltimaModificacao(p));
                itens.add(item);
            }

            handlerUI.post(() -> {
                escaneando = false;
                ultimaAtualizacao = System.currentTimeMillis();

                containerRecentes.removeAllViews();
                containerRecentes.setVisibility(View.VISIBLE);
                if (scrollRecentes != null) scrollRecentes.setVisibility(View.VISIBLE);
                vazioRecentes.setVisibility(View.GONE);

                for (int i = 0; i < itens.size(); i++) {
                    ItemRecente item = itens.get(i);
                    boolean ehUltimo = (i == itens.size() - 1);
                    containerRecentes.addView(
                            criarCardProjetoRecente(item, ehUltimo));
                }
            });

        }, "home-scan-recentes").start();
    }

    private void mostrarVazio() {
        escaneando = false;
        ultimaAtualizacao = System.currentTimeMillis();
        containerRecentes.removeAllViews();
        containerRecentes.setVisibility(View.GONE);
        if (scrollRecentes != null) scrollRecentes.setVisibility(View.GONE);
        vazioRecentes.setVisibility(View.VISIBLE);
    }

    // ==========================================================
    //  Card do carrossel (mesmo estilo dos cards "Abrir"/"Clonar")
    // ==========================================================

    private View criarCardProjetoRecente(ItemRecente item, boolean ehUltimo) {
        LinearLayout card = new LinearLayout(contexto);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        // Largura fixa no carrossel + margem entre cards
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                dp(LARGURA_CARD_DP),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = ehUltimo ? 0 : dp(10);
        card.setLayoutParams(lp);

        // ---------- Círculo com ícone ----------
        LinearLayout circulo = new LinearLayout(contexto);
        LinearLayout.LayoutParams lpCirculo =
                new LinearLayout.LayoutParams(dp(44), dp(44));
        circulo.setLayoutParams(lpCirculo);
        circulo.setGravity(Gravity.CENTER);
        circulo.setBackgroundResource(R.drawable.bg_icon_circle);

        ImageView icone = new ImageView(contexto);
        icone.setLayoutParams(new LinearLayout.LayoutParams(dp(22), dp(22)));
        icone.setImageResource(R.drawable.ic_pasta);
        icone.setColorFilter(Color.parseColor("#00E676"));
        circulo.addView(icone);
        card.addView(circulo);

        // ---------- Nome do projeto ----------
        TextView nome = new TextView(contexto);
        nome.setText(item.pasta.getName());
        nome.setTextColor(Color.parseColor("#F4F4F5"));
        nome.setTextSize(14);
        nome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        nome.setMaxLines(1);
        nome.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams lpNome = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpNome.topMargin = dp(14);
        nome.setLayoutParams(lpNome);
        card.addView(nome);

        // ---------- Descrição (v1.0.0 · 3 fontes) ----------
        TextView detalhe = new TextView(contexto);
        detalhe.setText(item.descricao);
        detalhe.setTextColor(Color.parseColor("#A1A1AA"));
        detalhe.setTextSize(11);
        detalhe.setMaxLines(2);
        detalhe.setEllipsize(android.text.TextUtils.TruncateAt.END);
        detalhe.setLineSpacing(dp(1), 1f);
        LinearLayout.LayoutParams lpDet = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpDet.topMargin = dp(4);
        detalhe.setLayoutParams(lpDet);
        card.addView(detalhe);

        // ---------- Tempo atrás ----------
        TextView tempo = new TextView(contexto);
        tempo.setText(item.tempo);
        tempo.setTextColor(Color.parseColor("#6B7280"));
        tempo.setTextSize(10);
        tempo.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams lpTempo = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpTempo.topMargin = dp(8);
        tempo.setLayoutParams(lpTempo);
        card.addView(tempo);

        // ---------- Clique ----------
        card.setOnClickListener(v -> {
            if (acoes != null) acoes.aoCliqueProjetoRecente(item.pasta);
        });

        return card;
    }

    // ==========================================================
    //  Helpers (I/O)
    // ==========================================================

    private long obterUltimaModificacao(File pasta) {
        long max = pasta.lastModified();
        File[] filhos = pasta.listFiles();
        if (filhos == null) return max;

        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            if (f.isFile()) {
                max = Math.max(max, f.lastModified());
            } else {
                File[] netos = f.listFiles();
                if (netos == null) continue;
                for (File g : netos) {
                    if (g.getName().startsWith(".")) continue;
                    if (g.isFile()) {
                        max = Math.max(max, g.lastModified());
                    }
                }
            }
        }
        return max;
    }

    private String tempoAtras(long millis) {
        if (millis <= 0) return "";

        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000)             return "agora";
        if (diff < 3_600_000)          return (diff / 60_000) + "min";
        if (diff < 86_400_000)         return (diff / 3_600_000) + "h";
        if (diff < 7 * 86_400_000L)    return (diff / 86_400_000) + "d";

        return new SimpleDateFormat("dd/MM", Locale.getDefault())
                .format(new Date(millis));
    }

    private String descreverProjeto(File pasta) {
        StringBuilder sb = new StringBuilder();

        File propFile = new File(pasta, GerenciadorProjetoProperties.NOME_ARQUIVO);
        if (propFile.isFile()) {
            try {
                Projeto p = GerenciadorProjetoProperties.carregar(pasta);
                if (p.versao != null && !p.versao.isEmpty()) {
                    sb.append("v").append(p.versao);
                }
            } catch (Exception ignored) { }
        }

        int fontes = contarFontes(pasta);
        if (fontes > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(fontes).append(fontes != 1 ? " fontes" : " fonte");
        }

        if (sb.length() == 0) return "vazio";
        return sb.toString();
    }

    private int contarFontes(File pasta) {
        File src = new File(pasta, "src");
        if (!src.isDirectory()) return 0;

        File[] filhos = src.listFiles();
        if (filhos == null) return 0;

        int total = 0;
        for (File f : filhos) {
            String n = f.getName().toLowerCase();
            if (n.endsWith(".cpp") || n.endsWith(".c")
                    || n.endsWith(".cc") || n.endsWith(".cxx")) {
                total++;
            }
        }
        return total;
    }

    private int dp(int v) {
        return (int) (v * contexto.getResources().getDisplayMetrics().density);
    }
}