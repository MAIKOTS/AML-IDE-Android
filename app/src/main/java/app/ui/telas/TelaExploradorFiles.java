package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import ui.explorador.AdapterArvore;
import ui.explorador.NoExploradorFiles;

/**
 * Tela que exibe a árvore de arquivos da pasta files/ do app,
 * com expand/collapse e opções de renomear/deletar.
 */
public class TelaExploradorFiles {

    public interface Acoes {
        void aoVoltar();
        void aoRecarregar();
    }

    private final Context ctx;
    private final Acoes acoes;

    private LinearLayout raizView;
    private TextView breadcrumb;
    private AdapterArvore adapter;
    private File pastaRaiz;

    public TelaExploradorFiles(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção da UI
    // ==========================================================

    public View construir(File pastaInicial) {
        this.pastaRaiz = pastaInicial;

        raizView = new LinearLayout(ctx);
        raizView.setOrientation(LinearLayout.VERTICAL);
        raizView.setBackgroundColor(0xFF09090B);

        // ---------- Barra superior ----------
        raizView.addView(criarBarraSuperior());

        // ---------- Breadcrumb ----------
        breadcrumb = new TextView(ctx);
        breadcrumb.setTextSize(11);
        breadcrumb.setTextColor(0xFFA1A1AA);
        breadcrumb.setPadding(dp(16), dp(8), dp(16), dp(8));
        breadcrumb.setText(pastaInicial.getAbsolutePath());
        raizView.addView(breadcrumb);

        // ---------- Ações rápidas ----------
        raizView.addView(criarBarraAcoes());

        // ---------- ListView (árvore) ----------
        ListView lista = new ListView(ctx);
        LinearLayout.LayoutParams lpLista = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        lista.setLayoutParams(lpLista);
        lista.setBackgroundColor(0xFF09090B);
        lista.setDivider(null);
        lista.setDividerHeight(0);

        adapter = new AdapterArvore(ctx, new AdapterArvore.Acoes() {
            @Override
            public void aoClicar(NoExploradorFiles no) {
                if (no.isPasta()) {
                    adapter.alternar(no);
                } else {
                    Toast.makeText(ctx, no.getNome(),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void aoClicarLongo(NoExploradorFiles no) {
                menuContexto(no);
            }
        });
        lista.setAdapter(adapter);

        // Carrega a raiz
        NoExploradorFiles raizNo = new NoExploradorFiles(pastaInicial, 0, null);
        raizNo.setExpandido(true);
        adapter.setRaiz(raizNo);
        adapter.alternar(raizNo);

        raizView.addView(lista);

        return raizView;
    }

    // ==========================================================
    //  Toolbar
    // ==========================================================

    private View criarBarraSuperior() {
        LinearLayout barra = new LinearLayout(ctx);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setBackgroundColor(0xFF0F0F11);
        barra.setPadding(dp(12), dp(10), dp(12), dp(10));

        TextView voltar = new TextView(ctx);
        voltar.setText("← Voltar");
        voltar.setTextColor(0xFFF4F4F5);
        voltar.setTextSize(14);
        voltar.setPadding(dp(8), dp(8), dp(16), dp(8));
        voltar.setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });
        barra.addView(voltar);

        TextView titulo = new TextView(ctx);
        titulo.setText("Explorador de Arquivos");
        titulo.setTextColor(0xFF00E676);
        titulo.setTextSize(15);
        titulo.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titulo.setLayoutParams(lp);
        titulo.setGravity(Gravity.CENTER);
        barra.addView(titulo);

        TextView espacador = new TextView(ctx);
        espacador.setText("");
        espacador.setWidth(dp(80));
        barra.addView(espacador);

        return barra;
    }

    private View criarBarraAcoes() {
        LinearLayout barra = new LinearLayout(ctx);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.CENTER_VERTICAL);
        barra.setPadding(dp(16), 0, dp(16), dp(8));

        barra.addView(criarBotaoAcao("↻ Recarregar", v -> recarregar()));
        barra.addView(criarBotaoAcao("📂 Nova pasta", v -> novaPasta()));
        barra.addView(criarBotaoAcao("✗ Limpar tudo", v -> confirmarLimparTudo()));

        return barra;
    }

    private Button criarBotaoAcao(String texto, View.OnClickListener click) {
        Button b = new Button(ctx);
        b.setText(texto);
        b.setTextSize(11);
        b.setAllCaps(false);
        b.setOnClickListener(click);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    // ==========================================================
    //  Menu de contexto
    // ==========================================================

    private void menuContexto(NoExploradorFiles no) {
        String[] opcoes;
        if (no.isPasta()) {
            opcoes = new String[] { "Renomear pasta", "Deletar pasta" };
        } else {
            opcoes = new String[] { "Renomear arquivo", "Deletar arquivo" };
        }

        new AlertDialog.Builder(ctx)
                .setTitle(no.getNome())
                .setItems(opcoes, (d, w) -> {
                    if (w == 0) {
                        dialogoRenomear(no);
                    } else if (w == 1) {
                        confirmarDeletar(no);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================================
    //  Ações
    // ==========================================================

    private void dialogoRenomear(NoExploradorFiles no) {
        EditText campo = new EditText(ctx);
        campo.setText(no.getNome());
        campo.setSelection(no.getNome().length());
        campo.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(ctx)
                .setTitle("Renomear")
                .setView(campo)
                .setPositiveButton("Renomear", (d, w) -> {
                    String novoNome = campo.getText().toString().trim();
                    if (novoNome.isEmpty() || novoNome.equals(no.getNome())) return;

                    File antigo = no.getArquivo();
                    File novo = new File(antigo.getParentFile(), novoNome);

                    if (novo.exists()) {
                        Toast.makeText(ctx, "Já existe um item com esse nome.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (antigo.renameTo(novo)) {
                        Toast.makeText(ctx, "Renomeado.", Toast.LENGTH_SHORT).show();
                        recarregar();
                    } else {
                        Toast.makeText(ctx, "Falha ao renomear.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarDeletar(NoExploradorFiles no) {
        String tipo = no.isPasta() ? "pasta" : "arquivo";
        String aviso = no.isPasta()
                ? "Todos os arquivos dentro dela serão apagados. Esta ação não pode ser desfeita."
                : "Esta ação não pode ser desfeita.";

        new AlertDialog.Builder(ctx)
                .setTitle("Deletar " + tipo + "?")
                .setMessage(no.getNome() + "\n\n" + aviso)
                .setPositiveButton("Deletar", (d, w) -> {
                    boolean ok = deletarRecursivo(no.getArquivo());
                    Toast.makeText(ctx,
                            ok ? "Deletado." : "Falha ao deletar.",
                            Toast.LENGTH_SHORT).show();
                    recarregar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void novaPasta() {
        EditText campo = new EditText(ctx);
        campo.setHint("nome-da-pasta");
        campo.setPadding(dp(16), dp(16), dp(16), dp(16));

        new AlertDialog.Builder(ctx)
                .setTitle("Nova pasta em " + pastaRaiz.getName())
                .setView(campo)
                .setPositiveButton("Criar", (d, w) -> {
                    String nome = campo.getText().toString().trim();
                    if (nome.isEmpty()) return;

                    File nova = new File(pastaRaiz, nome);
                    if (nova.exists()) {
                        Toast.makeText(ctx, "Já existe.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (nova.mkdirs()) {
                        Toast.makeText(ctx, "Criada.", Toast.LENGTH_SHORT).show();
                        recarregar();
                    } else {
                        Toast.makeText(ctx, "Falha ao criar.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarLimparTudo() {
        new AlertDialog.Builder(ctx)
                .setTitle("Limpar tudo?")
                .setMessage("Isso vai apagar TODO o conteúdo de:\n\n" +
                        pastaRaiz.getAbsolutePath() +
                        "\n\nEsta ação não pode ser desfeita.")
                .setPositiveButton("Apagar tudo", (d, w) -> {
                    File[] filhos = pastaRaiz.listFiles();
                    if (filhos != null) {
                        for (File f : filhos) deletarRecursivo(f);
                    }
                    Toast.makeText(ctx, "Conteúdo apagado.", Toast.LENGTH_SHORT).show();
                    recarregar();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private void recarregar() {
        if (adapter != null) adapter.recarregar();
        if (acoes != null) acoes.aoRecarregar();
    }

    private boolean deletarRecursivo(File f) {
        if (f == null || !f.exists()) return false;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) {
                for (File x : filhos) deletarRecursivo(x);
            }
        }
        return f.delete();
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}