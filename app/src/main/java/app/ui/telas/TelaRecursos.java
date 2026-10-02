package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import app.R;
import app.util.GerenciadorRecursos;
import modelo.ManifestoRecursos;
import modelo.RecursoRemoto;
import ui.dialogos.DialogoApp;

/**
 * Tela que lista os recursos essenciais da IDE (sysroot, runtime, etc.)
 * e permite baixar / atualizar / remover cada um.
 *
 * Substitui os antigos recursos que vinham embutidos no APK.
 */
public class TelaRecursos {

    public interface Acoes {
        void aoVoltar();
    }

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());
    private final GerenciadorRecursos gerenciador;

    // Views
    private View raizView;
    private LinearLayout listaView;
    private TextView vazioView;
    private TextView espacoLivreView;
    private TextView espacoUsadoView;
    private ImageButton btnAtualizar;

    // Estado
    private ManifestoRecursos manifesto;
    private boolean operacaoEmAndamento = false;

    public TelaRecursos(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
        this.gerenciador = new GerenciadorRecursos(ctx);
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construir() {
        raizView = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_recursos, null, false);

        listaView        = raizView.findViewById(R.id.recursosLista);
        vazioView        = raizView.findViewById(R.id.recursosVazio);
        espacoLivreView  = raizView.findViewById(R.id.recursosEspacoLivre);
        espacoUsadoView  = raizView.findViewById(R.id.recursosEspacoUsado);
        btnAtualizar     = raizView.findViewById(R.id.recursosBtnAtualizar);

        raizView.findViewById(R.id.recursosBtnVoltar).setOnClickListener(v -> {
            if (operacaoEmAndamento) {
                toast("Aguarde a operação atual terminar.");
                return;
            }
            if (acoes != null) acoes.aoVoltar();
        });

        btnAtualizar.setOnClickListener(v -> carregarManifesto());

        // Estado inicial
        vazioView.setVisibility(View.VISIBLE);
        vazioView.setText("Carregando recursos...");

        atualizarEspaco();
        carregarManifesto();

        return raizView;
    }

    // ==========================================================
    //  Manifesto
    // ==========================================================

    private void carregarManifesto() {
        if (operacaoEmAndamento) return;

        // Primeiro tenta local (rápido, offline)
        ManifestoRecursos local = gerenciador.lerManifestoLocal();
        if (local != null) {
            this.manifesto = local;
            renderizarLista();
        }

        // Depois atualiza da nuvem
        gerenciador.baixarManifesto(new GerenciadorRecursos.CallbackManifesto() {
            @Override
            public void aoConcluir(final ManifestoRecursos m) {
                handlerUI.post(() -> {
                    manifesto = m;
                    renderizarLista();
                });
            }

            @Override
            public void aoErro(final String mensagem) {
                handlerUI.post(() -> {
                    if (manifesto == null) {
                        vazioView.setVisibility(View.VISIBLE);
                        vazioView.setText("Não foi possível carregar a lista.\n\n"
                                + mensagem + "\n\nToque em ↻ para tentar novamente.");
                    } else {
                        toast("Sem conexão. Mostrando versão local.");
                    }
                });
            }
        });
    }

    // ==========================================================
    //  Lista de recursos
    // ==========================================================

    private void renderizarLista() {
        listaView.removeAllViews();

        if (manifesto == null || manifesto.recursos.isEmpty()) {
            vazioView.setVisibility(View.VISIBLE);
            vazioView.setText("Nenhum recurso disponível.");
            return;
        }

        vazioView.setVisibility(View.GONE);

        for (RecursoRemoto r : manifesto.recursos) {
            listaView.addView(criarCardRecurso(r));
        }

        atualizarEspaco();
    }

    private View criarCardRecurso(final RecursoRemoto r) {
        boolean instalado = gerenciador.estaInstalado(r);

        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);

        // ---- Linha 1: bolinha + nome + tamanho ----
        LinearLayout linha1 = new LinearLayout(ctx);
        linha1.setOrientation(LinearLayout.HORIZONTAL);
        linha1.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(linha1);

        TextView status = new TextView(ctx);
        status.setText(instalado ? "● " : "○ ");
        status.setTextColor(instalado
                ? Color.parseColor("#00E676")
                : Color.parseColor("#52525B"));
        status.setTextSize(14);
        LinearLayout.LayoutParams lpStatus =
                new LinearLayout.LayoutParams(dp(20), dp(20));
        lpStatus.rightMargin = dp(10);
        status.setLayoutParams(lpStatus);
        linha1.addView(status);

        TextView nome = new TextView(ctx);
        nome.setText(r.nome);
        nome.setTextColor(Color.parseColor("#F4F4F5"));
        nome.setTextSize(14);
        nome.setTypeface(null, Typeface.BOLD);
        nome.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        linha1.addView(nome);

        if (r.tamanhoBytes > 0) {
            TextView tam = new TextView(ctx);
            tam.setText(GerenciadorRecursos.formatarTamanho(r.tamanhoBytes));
            tam.setTextColor(Color.parseColor("#6B7280"));
            tam.setTextSize(10);
            tam.setTypeface(Typeface.MONOSPACE);
            linha1.addView(tam);
        }

        // ---- Linha 2: descrição ----
        if (r.descricao != null && !r.descricao.isEmpty()) {
            TextView desc = new TextView(ctx);
            desc.setText(r.descricao);
            desc.setTextColor(Color.parseColor("#A1A1AA"));
            desc.setTextSize(11);
            LinearLayout.LayoutParams lpDesc = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lpDesc.topMargin = dp(4);
            desc.setLayoutParams(lpDesc);
            card.addView(desc);
        }

        // ---- Linha 3: status de instalação ----
        TextView statusTxt = new TextView(ctx);
        statusTxt.setText(instalado ? "Instalado" : "Não instalado");
        statusTxt.setTextColor(instalado
                ? Color.parseColor("#00E676")
                : Color.parseColor("#FFC107"));
        statusTxt.setTextSize(10);
        statusTxt.setTypeface(Typeface.MONOSPACE);
        LinearLayout.LayoutParams lpStatusTxt = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpStatusTxt.topMargin = dp(6);
        statusTxt.setLayoutParams(lpStatusTxt);
        card.addView(statusTxt);

        // ---- Linha 4: botões ----
        LinearLayout linhaBotoes = new LinearLayout(ctx);
        linhaBotoes.setOrientation(LinearLayout.HORIZONTAL);
        linhaBotoes.setGravity(Gravity.END);
        LinearLayout.LayoutParams lpB = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lpB.topMargin = dp(10);
        linhaBotoes.setLayoutParams(lpB);

        if (instalado) {
            TextView btnRemover = criarBotao("Remover", "#FF5252", v ->
                    confirmarRemocao(r));
            linhaBotoes.addView(btnRemover);
        }

        String textoBaixar = instalado ? "Reinstalar" : "Baixar";
        TextView btnBaixar = criarBotao(textoBaixar, "#00E676", v ->
                iniciarInstalacao(r));
        linhaBotoes.addView(btnBaixar);

        card.addView(linhaBotoes);

        return card;
    }

    private TextView criarBotao(String texto, String cor, View.OnClickListener onClick) {
        TextView b = new TextView(ctx);
        b.setText(texto);
        b.setTextColor(Color.parseColor(cor));
        b.setTextSize(12);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(14), dp(8), dp(14), dp(8));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(onClick);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(4);
        b.setLayoutParams(lp);
        return b;
    }

    // ==========================================================
    //  Remoção
    // ==========================================================

    private void confirmarRemocao(final RecursoRemoto r) {
        DialogoApp.confirmar(ctx,
                "Remover " + r.nome + "?",
                "Isso vai apagar os arquivos. Você pode baixar novamente depois.",
                "Remover", "Cancelar", true,
                () -> {
                    gerenciador.remover(r);
                    toast(r.nome + " removido.");
                    renderizarLista();
                });
    }

    // ==========================================================
    //  Instalação
    // ==========================================================

    private void iniciarInstalacao(final RecursoRemoto r) {
        if (operacaoEmAndamento) return;
        operacaoEmAndamento = true;

        // Dialog de progresso simples
        final AlertDialog[] refDialog = new AlertDialog[1];
        final TextView[] refTexto = new TextView[1];

        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
        builder.setTitle("Baixando " + r.nome);
        builder.setCancelable(false);

        TextView progresso = new TextView(ctx);
        progresso.setPadding(dp(24), dp(24), dp(24), dp(24));
        progresso.setTextSize(13);
        progresso.setTextColor(Color.parseColor("#F4F4F5"));
        progresso.setText("Preparando...");
        refTexto[0] = progresso;

        builder.setView(progresso);
        builder.setNegativeButton("Cancelar", (d, w) -> {
            operacaoEmAndamento = false;
        });

        final AlertDialog dialog = builder.create();
        refDialog[0] = dialog;
        dialog.show();

        gerenciador.instalar(r, new GerenciadorRecursos.CallbackInstalacao() {

            @Override
            public void aoProgresso(final String etapa, final int pct) {
                handlerUI.post(() -> {
                    if (refTexto[0] == null) return;
                    String txt = pct >= 0 ? etapa + "\n" + pct + "%" : etapa + "...";
                    refTexto[0].setText(txt);
                });
            }

            @Override
            public void aoConcluir() {
                handlerUI.post(() -> {
                    if (refDialog[0] != null) refDialog[0].dismiss();
                    operacaoEmAndamento = false;
                    toast(r.nome + " instalado.");
                    renderizarLista();
                });
            }

            @Override
            public void aoErro(final String mensagem) {
                handlerUI.post(() -> {
                    if (refDialog[0] != null) refDialog[0].dismiss();
                    operacaoEmAndamento = false;
                    toast("Falha: " + mensagem);
                });
            }

            @Override
            public boolean cancelado() {
                return !operacaoEmAndamento;
            }
        });
    }

    // ==========================================================
    //  Espaço em disco
    // ==========================================================

    private void atualizarEspaco() {
        long livre = gerenciador.obterEspacoLivre();
        espacoLivreView.setText("Espaço livre: "
                + GerenciadorRecursos.formatarTamanho(livre));

        long usado = tamanhoRecursivo(new File(ctx.getFilesDir(), "sysroot"))
                   + tamanhoRecursivo(new File(ctx.getFilesDir(), "lib"));
        espacoUsadoView.setText("Recursos instalados: "
                + GerenciadorRecursos.formatarTamanho(usado));
    }

    private long tamanhoRecursivo(File f) {
        if (f == null || !f.exists()) return 0;
        if (f.isFile()) return f.length();
        long t = 0;
        File[] filhos = f.listFiles();
        if (filhos != null) for (File x : filhos) t += tamanhoRecursivo(x);
        return t;
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private void toast(String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}