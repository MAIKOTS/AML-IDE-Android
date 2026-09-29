package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

    /** Quantos projetos recentes mostrar na home. */
    private static final int LIMITE_RECENTES = 5;

    private final Context contexto;
    private final AcoesHome acoes;

    // Views atualizadas a cada construirLayout()
    private LinearLayout containerRecentes;
    private LinearLayout vazioRecentes;

    public MontadorDeHome(Context contexto, AcoesHome acoes) {
        this.contexto = contexto;
        this.acoes = acoes;
    }

    public View construirLayout() {
        View layout = LayoutInflater.from(contexto)
                .inflate(R.layout.comp_inicio, null, false);

        // -------- Mapeia ações rápidas --------
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

        // -------- Seção de projetos recentes --------
        containerRecentes = layout.findViewById(R.id.home_recentes_container);
        vazioRecentes     = layout.findViewById(R.id.home_recentes_vazio);

        carregarProjetosRecentes();

        return layout;
    }

    /**
     * Recarrega a lista de recentes sem reconstruir todo o layout.
     * Use quando já estiver na Home e quiser atualizar (ex: depois
     * de criar/renomear/deletar um projeto em outra tela).
     */
    public void atualizarRecentes() {
        if (containerRecentes != null) {
            carregarProjetosRecentes();
        }
    }

    // ==========================================================
    //  Lista de projetos recentes
    // ==========================================================

    private void carregarProjetosRecentes() {
        if (containerRecentes == null || vazioRecentes == null) return;

        containerRecentes.removeAllViews();

        File pastaProjetos = new File(
                Environment.getExternalStorageDirectory(),
                "AML_IDE/Projetos");

        if (!pastaProjetos.isDirectory()) {
            vazioRecentes.setVisibility(View.VISIBLE);
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
            vazioRecentes.setVisibility(View.VISIBLE);
            return;
        }

        vazioRecentes.setVisibility(View.GONE);

        // Ordena por última modificação (mais recente primeiro)
        projetos.sort((a, b) -> Long.compare(
                obterUltimaModificacao(b),
                obterUltimaModificacao(a)));

        int limite = Math.min(LIMITE_RECENTES, projetos.size());
        for (int i = 0; i < limite; i++) {
            containerRecentes.addView(
                    criarCardProjetoRecente(projetos.get(i)));
        }

        // Link "Ver todos" se tiver mais que o limite
        if (projetos.size() > limite) {
            TextView verTodos = new TextView(contexto);
            verTodos.setText("Ver todos os " + projetos.size() + " projetos  ›");
            verTodos.setTextColor(Color.parseColor("#00E676"));
            verTodos.setTextSize(12);
            verTodos.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            verTodos.setGravity(Gravity.CENTER);
            verTodos.setPadding(dp(8), dp(14), dp(8), dp(4));
            verTodos.setOnClickListener(v -> {
                if (acoes != null) acoes.aoCliqueImportarProjeto();
            });
            containerRecentes.addView(verTodos);
        }
    }

    // ==========================================================
    //  Card de um projeto
    // ==========================================================

    private View criarCardProjetoRecente(File projeto) {
        LinearLayout card = new LinearLayout(contexto);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setClickable(true);
        card.setFocusable(true);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(lpCard);

        // ---- Círculo com ícone ----
        LinearLayout circuloIcone = new LinearLayout(contexto);
        LinearLayout.LayoutParams lpCirculo =
                new LinearLayout.LayoutParams(dp(38), dp(38));
        lpCirculo.setMargins(0, 0, dp(14), 0);
        circuloIcone.setLayoutParams(lpCirculo);
        circuloIcone.setGravity(Gravity.CENTER);
        circuloIcone.setBackgroundResource(R.drawable.bg_icon_circle);

        ImageView icone = new ImageView(contexto);
        icone.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        icone.setImageResource(R.drawable.ic_pasta);
        icone.setColorFilter(Color.parseColor("#00E676"));
        circuloIcone.addView(icone);
        card.addView(circuloIcone);

        // ---- Coluna: nome + detalhes ----
        LinearLayout coluna = new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpColuna = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        coluna.setLayoutParams(lpColuna);

        TextView nome = new TextView(contexto);
        nome.setText(projeto.getName());
        nome.setTextColor(Color.parseColor("#F4F4F5"));
        nome.setTextSize(14);
        nome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        coluna.addView(nome);

        TextView detalhe = new TextView(contexto);
        detalhe.setText(descreverProjeto(projeto));
        detalhe.setTextColor(Color.parseColor("#A1A1AA"));
        detalhe.setTextSize(11);
        detalhe.setPadding(0, dp(3), 0, 0);
        coluna.addView(detalhe);

        card.addView(coluna);

        // ---- Tempo atrás ----
        TextView tempo = new TextView(contexto);
        tempo.setText(tempoAtras(obterUltimaModificacao(projeto)));
        tempo.setTextColor(Color.parseColor("#6B7280"));
        tempo.setTextSize(10);
        LinearLayout.LayoutParams lpTempo = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lpTempo.setMargins(dp(8), 0, dp(10), 0);
        tempo.setLayoutParams(lpTempo);
        card.addView(tempo);

        // ---- Seta ----
        TextView seta = new TextView(contexto);
        seta.setText("›");
        seta.setTextColor(Color.parseColor("#A1A1AA"));
        seta.setTextSize(20);
        card.addView(seta);

        // ---- Clique ----
        card.setOnClickListener(v -> {
            if (acoes != null) acoes.aoCliqueProjetoRecente(projeto);
        });

        return card;
    }

    // ==========================================================
    //  Helpers de conteúdo
    // ==========================================================

    /**
     * Retorna a data mais recente de modificação dentro do projeto.
     *
     * Olha a pasta, os arquivos diretos e um nível dentro de cada
     * subpasta (src/, include/, lib/, deps/). Ignora arquivos ocultos.
     */
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

    /** Formata timestamp como "agora", "5min", "2h", "3d" ou "dd/MM". */
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

    /** Descrição curta: "v1.0.0 · arm64-v8a, armeabi-v7a · 3 fontes". */
    private String descreverProjeto(File pasta) {
        StringBuilder sb = new StringBuilder();

        File propFile = new File(pasta, GerenciadorProjetoProperties.NOME_ARQUIVO);
        if (propFile.isFile()) {
            try {
                Projeto p = GerenciadorProjetoProperties.carregar(pasta);
                if (p.versao != null && !p.versao.isEmpty()) {
                    sb.append("v").append(p.versao);
                }
                if (p.abis != null && !p.abis.isEmpty()) {
                    if (sb.length() > 0) sb.append("  ·  ");
                    sb.append(String.join(", ", p.abis));
                }
            } catch (Exception ignored) { }
        }

        int fontes = contarFontes(pasta);
        if (fontes > 0) {
            if (sb.length() > 0) sb.append("  ·  ");
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