package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import app.R;

public class MontadorDeSeletorDeProjetos {

    public interface AcoesSeletor {
        void aoSelecionarPastaProjeto(File pasta);
        void aoCancelar();
    }

    private final Context contexto;
    private final AcoesSeletor acoes;

    /** Raiz permitida — o usuário não pode subir acima disso. */
    private final File raizPermitida;

    private File pastaAtual;

    private TextView textCaminhoAtual;
    private LinearLayout conteinerLista;
    private TextView btnSubir;

    public MontadorDeSeletorDeProjetos(Context contexto, AcoesSeletor acoes) {
        this.contexto = contexto;
        this.acoes = acoes;
        this.raizPermitida = Environment.getExternalStorageDirectory();
        this.pastaAtual = raizPermitida;
    }

    public View construirLayout() {
        View raiz = LayoutInflater.from(contexto)
                .inflate(R.layout.tela_seletor_projetos, null, false);

        // Bind
        textCaminhoAtual = raiz.findViewById(R.id.seletorCaminhoAtual);
        conteinerLista   = raiz.findViewById(R.id.seletorLista);
        btnSubir         = raiz.findViewById(R.id.btnSeletorSubir);

        // Voltar
        raiz.findViewById(R.id.btnSeletorVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoCancelar();
        });

        // Nova pasta
        raiz.findViewById(R.id.btnSeletorNovaPasta)
                .setOnClickListener(v -> exibirDialogoCriarPasta());

        // Subir
        btnSubir.setOnClickListener(v -> subirUmNivel());

        // Confirmar
        raiz.findViewById(R.id.btnSeletorConfirmar).setOnClickListener(v -> {
            if (pastaAtual != null && pastaAtual.isDirectory() && acoes != null) {
                acoes.aoSelecionarPastaProjeto(pastaAtual);
            }
        });

        atualizarListaDiretorios();
        return raiz;
    }

    // ==========================================================
    //  Lista de diretórios e arquivos
    // ==========================================================

    private void atualizarListaDiretorios() {
        if (pastaAtual == null) return;

        textCaminhoAtual.setText(pastaAtual.getAbsolutePath());
        conteinerLista.removeAllViews();
        atualizarBotaoSubir();

        File[] arquivos = pastaAtual.listFiles();
        if (arquivos == null || arquivos.length == 0) {
            conteinerLista.addView(criarMensagemVazia("Esta pasta está vazia."));
            return;
        }

        // Ordena: pastas primeiro, depois arquivos, alfabético em cada grupo
        Arrays.sort(arquivos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        int totalPastas = 0;
        int totalArquivos = 0;

        for (File item : arquivos) {
            if (item.getName().startsWith(".")) continue;

            if (item.isDirectory()) {
                conteinerLista.addView(criarItemPasta(item));
                totalPastas++;
            } else {
                conteinerLista.addView(criarItemArquivo(item));
                totalArquivos++;
            }
        }

        // Se não mostrou nada (só ocultos), avisa
        if (totalPastas == 0 && totalArquivos == 0) {
            conteinerLista.addView(criarMensagemVazia("Nada para mostrar aqui."));
        }
    }

    /** Item visual para PASTA (clicável, navega para dentro). */
    private View criarItemPasta(File pasta) {
        LinearLayout card = new LinearLayout(contexto);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackgroundResource(R.drawable.bg_seletor_item);

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(lpCard);

        // Círculo com ícone
        LinearLayout circulo = new LinearLayout(contexto);
        LinearLayout.LayoutParams lpCirculo = new LinearLayout.LayoutParams(dp(36), dp(36));
        lpCirculo.setMargins(0, 0, dp(12), 0);
        circulo.setLayoutParams(lpCirculo);
        circulo.setGravity(Gravity.CENTER);
        circulo.setBackgroundResource(R.drawable.bg_menu_icon_circle);

        Drawable icone = contexto.getDrawable(R.drawable.ic_pasta);
        if (icone != null) {
            icone = icone.mutate();
            icone.setTint(Color.parseColor("#00E676"));
            ImageView iv = new ImageView(contexto);
            iv.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
            iv.setImageDrawable(icone);
            circulo.addView(iv);
        }
        card.addView(circulo);

        // Coluna: nome + contagem
        LinearLayout coluna = new LinearLayout(contexto);
        coluna.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lpCol = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        coluna.setLayoutParams(lpCol);

        TextView nome = new TextView(contexto);
        nome.setText(pasta.getName());
        nome.setTextColor(Color.parseColor("#F4F4F5"));
        nome.setTextSize(14);
        nome.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        coluna.addView(nome);

        // Contagem de conteúdo
        File[] filhos = pasta.listFiles();
        int qtdPastas = 0, qtdArquivos = 0;
        if (filhos != null) {
            for (File f : filhos) {
                if (f.getName().startsWith(".")) continue;
                if (f.isDirectory()) qtdPastas++; else qtdArquivos++;
            }
        }
        String resumo;
        if (qtdPastas == 0 && qtdArquivos == 0) {
            resumo = "vazia";
        } else {
            resumo = qtdPastas + " pasta" + (qtdPastas != 1 ? "s" : "")
                   + ", " + qtdArquivos + " arquivo" + (qtdArquivos != 1 ? "s" : "");
        }
        TextView detalhe = new TextView(contexto);
        detalhe.setText(resumo);
        detalhe.setTextColor(Color.parseColor("#A1A1AA"));
        detalhe.setTextSize(10);
        detalhe.setPadding(0, dp(2), 0, 0);
        coluna.addView(detalhe);

        card.addView(coluna);

        // Seta
        TextView seta = new TextView(contexto);
        seta.setText("›");
        seta.setTextColor(Color.parseColor("#A1A1AA"));
        seta.setTextSize(18);
        card.addView(seta);

        card.setOnClickListener(v -> {
            pastaAtual = pasta;
            atualizarListaDiretorios();
        });

        return card;
    }

    /** Item visual para ARQUIVO (não clicável, apagado). */
    private View criarItemArquivo(File arquivo) {
        LinearLayout card = new LinearLayout(contexto);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(10), dp(12), dp(10));
        card.setClickable(false);
        card.setFocusable(false);
        card.setAlpha(0.55f); // apagado
        card.setBackgroundResource(R.drawable.bg_seletor_item);

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(0, dp(4), 0, dp(4));
        card.setLayoutParams(lpCard);

        // Círculo com ícone de arquivo (cinza)
        LinearLayout circulo = new LinearLayout(contexto);
        LinearLayout.LayoutParams lpCirculo = new LinearLayout.LayoutParams(dp(36), dp(36));
        lpCirculo.setMargins(0, 0, dp(12), 0);
        circulo.setLayoutParams(lpCirculo);
        circulo.setGravity(Gravity.CENTER);
        circulo.setBackgroundResource(R.drawable.bg_menu_icon_circle);

        Drawable icone = contexto.getDrawable(R.drawable.ic_arquivo_cpp);
        if (icone != null) {
            icone = icone.mutate();
            icone.setTint(Color.parseColor("#6B7280")); // cinza
            ImageView iv = new ImageView(contexto);
            iv.setLayoutParams(new LinearLayout.LayoutParams(dp(18), dp(18)));
            iv.setImageDrawable(icone);
            circulo.addView(iv);
        }
        card.addView(circulo);

        // Nome
        TextView nome = new TextView(contexto);
        nome.setText(arquivo.getName());
        nome.setTextColor(Color.parseColor("#A1A1AA"));
        nome.setTextSize(13);
        LinearLayout.LayoutParams lpNome = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        nome.setLayoutParams(lpNome);
        card.addView(nome);

        // Tamanho
        TextView tamanho = new TextView(contexto);
        tamanho.setText(formatarTamanho(arquivo.length()));
        tamanho.setTextColor(Color.parseColor("#6B7280"));
        tamanho.setTextSize(10);
        card.addView(tamanho);

        return card;
    }

    private TextView criarMensagemVazia(String texto) {
        TextView tv = new TextView(contexto);
        tv.setText(texto);
        tv.setTextColor(Color.parseColor("#6B7280"));
        tv.setPadding(dp(24), dp(24), dp(24), dp(24));
        return tv;
    }

    // ==========================================================
    //  Navegação
    // ==========================================================

    private void subirUmNivel() {
        if (pastaAtual == null) return;
        File pai = pastaAtual.getParentFile();
        if (pai == null) return;

        if (estaDentroDaRaiz(pai)) {
            pastaAtual = pai;
            atualizarListaDiretorios();
        } else {
            Toast.makeText(contexto, "Você já está no topo.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean estaDentroDaRaiz(File destino) {
        try {
            String cDestino = destino.getCanonicalPath();
            String cRaiz    = raizPermitida.getCanonicalPath();
            return cDestino.equals(cRaiz)
                    || cDestino.startsWith(cRaiz + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    private void atualizarBotaoSubir() {
        File pai = pastaAtual != null ? pastaAtual.getParentFile() : null;
        boolean podeSubir = pai != null && estaDentroDaRaiz(pai);
        btnSubir.setEnabled(podeSubir);
        btnSubir.setAlpha(podeSubir ? 1.0f : 0.4f);
    }

    // ==========================================================
    //  Nova pasta
    // ==========================================================

    private void exibirDialogoCriarPasta() {
        AlertDialog.Builder builder = new AlertDialog.Builder(contexto);
        builder.setTitle("Criar Nova Pasta");

        final EditText input = new EditText(contexto);
        input.setHint("Nome da pasta");
        input.setPadding(dp(20), dp(20), dp(20), dp(20));
        builder.setView(input);

        builder.setPositiveButton("Criar", (dialog, which) -> {
            String nomePasta = input.getText().toString().trim();
            if (!nomePasta.isEmpty() && pastaAtual != null) {
                File novaPasta = new File(pastaAtual, nomePasta);
                if (novaPasta.mkdirs()) {
                    Toast.makeText(contexto, "Pasta criada com sucesso!",
                            Toast.LENGTH_SHORT).show();
                    atualizarListaDiretorios();
                } else {
                    Toast.makeText(contexto, "Falha ao criar pasta.",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private int dp(int v) {
        return (int) (v * contexto.getResources().getDisplayMetrics().density);
    }
}