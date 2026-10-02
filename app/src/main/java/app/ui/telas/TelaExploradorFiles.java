package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import app.R;
import ui.dialogos.DialogoApp;
import ui.dialogos.ItemAcao;

/**
 * Explorador de arquivos da memória privada do app (files/).
 *
 * Estilo clean, igual à tela de estrutura:
 *  - Árvore desenhada em LinearLayout (sem ListView/adapter)
 *  - Toolbar minimalista (voltar, título, menu)
 *  - Menu de ações com ícones via DialogoApp
 *  - Clipboard com colar/cancelar
 *  - Suporte a editar arquivo no editor da IDE
 */
public class TelaExploradorFiles {

    public interface Acoes {
        void aoVoltar();
        void aoRecarregar();
        /** Abre o arquivo no editor da IDE. */
        default void aoEditarArquivo(File arquivo) { }
    }

    // ==========================================================
    //  Clipboard (estático, persiste entre aberturas)
    // ==========================================================
    private static File    clipboardArquivo   = null;
    private static boolean clipboardEhRecorte = false;

    // ==========================================================
    //  Estado
    // ==========================================================
    private final Context ctx;
    private final Acoes   acoes;
    private       File    pastaRaiz;
    private final Set<String> expandidas = new HashSet<>();

    // ==========================================================
    //  Views
    // ==========================================================
    private TextView     textTitulo;
    private TextView     textCaminho;
    private LinearLayout conteinerArvore;
    private LinearLayout barraClipboard;
    private TextView     clipboardTexto;

    public TelaExploradorFiles(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construir(File pastaInicial) {
        this.pastaRaiz = pastaInicial;

        View raiz = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_explorador_files, null, false);

        // ---- Bind ----
        textTitulo      = raiz.findViewById(R.id.exploradorTitulo);
        textCaminho     = raiz.findViewById(R.id.exploradorCaminho);
        conteinerArvore = raiz.findViewById(R.id.exploradorArvore);
        barraClipboard  = raiz.findViewById(R.id.exploradorBarraClipboard);
        clipboardTexto  = raiz.findViewById(R.id.exploradorClipboardTexto);

        // ---- Toolbar ----
        raiz.findViewById(R.id.exploradorBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });
        raiz.findViewById(R.id.exploradorBtnMenu).setOnClickListener(v -> abrirMenu());

        // ---- Clipboard ----
        raiz.findViewById(R.id.exploradorBtnColar).setOnClickListener(v -> colar(pastaRaiz));
        raiz.findViewById(R.id.exploradorBtnCancelarClipboard).setOnClickListener(v -> {
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
        });

        // ---- Cabeçalho ----
        textTitulo.setText("Memória privada");
        textCaminho.setText(relativizarCaminho(pastaRaiz));

        atualizarBarraClipboard();
        recarregarArvore();
        return raiz;
    }

    // ==========================================================
    //  Árvore
    // ==========================================================

    private void recarregarArvore() {
        conteinerArvore.removeAllViews();

        if (pastaRaiz == null || !pastaRaiz.exists()) {
            addAviso("Pasta privada não encontrada.");
            return;
        }

        desenharConteudo(pastaRaiz, conteinerArvore, 0);
    }

    private void desenharConteudo(File diretorio, LinearLayout pai, int recuo) {
        File[] filhos = diretorio.listFiles();
        if (filhos == null) return;

        List<File> pastas   = new ArrayList<>();
        List<File> arquivos = new ArrayList<>();

        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) pastas.add(f);
            else arquivos.add(f);
        }

        pastas.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        arquivos.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        if (pastas.isEmpty() && arquivos.isEmpty()) {
            TextView vazio = new TextView(ctx);
            vazio.setText("(vazia)");
            vazio.setTextColor(Color.parseColor("#3F3F46"));
            vazio.setTextSize(10);
            vazio.setPadding(dp(12) + recuo * dp(16) + dp(30),
                    dp(8), dp(8), dp(8));
            pai.addView(vazio);
            return;
        }

        for (File p : pastas) {
            boolean expandida = expandidas.contains(p.getAbsolutePath());
            pai.addView(criarItemPasta(p, recuo, expandida));

            if (expandida) {
                LinearLayout sub = new LinearLayout(ctx);
                sub.setOrientation(LinearLayout.VERTICAL);
                sub.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                pai.addView(sub);
                desenharConteudo(p, sub, recuo + 1);
            }
        }

        for (File a : arquivos) {
            pai.addView(criarItemArquivo(a, recuo));
        }
    }

    private View criarItemPasta(final File pasta, int recuo, boolean expandida) {
        View item = LayoutInflater.from(ctx)
                .inflate(R.layout.item_estrutura_pasta, conteinerArvore, false);

        item.setPadding(dp(12) + recuo * dp(16), dp(6), dp(12), dp(6));

        TextView seta    = item.findViewById(R.id.itemPastaSeta);
        TextView nome    = item.findViewById(R.id.itemPastaNome);
        TextView detalhe = item.findViewById(R.id.itemPastaDetalhe);

        nome.setText(pasta.getName());
        detalhe.setText(contarConteudo(pasta));
        seta.setText(expandida ? "▼" : "▶");

        item.setOnClickListener(v -> {
            String path = pasta.getAbsolutePath();
            if (expandidas.contains(path)) {
                expandidas.remove(path);
            } else {
                expandidas.add(path);
            }
            recarregarArvore();
        });

        item.setOnLongClickListener(v -> {
            menuContexto(pasta, true);
            return true;
        });

        return item;
    }

    private View criarItemArquivo(final File arquivo, int recuo) {
        View item = LayoutInflater.from(ctx)
                .inflate(R.layout.item_estrutura_arquivo, conteinerArvore, false);

        item.setPadding(dp(12) + recuo * dp(16), dp(6), dp(12), dp(6));

        ImageView icone  = item.findViewById(R.id.itemArquivoIcone);
        TextView nome    = item.findViewById(R.id.itemArquivoNome);
        TextView detalhe = item.findViewById(R.id.itemArquivoDetalhe);

        nome.setText(arquivo.getName());
        detalhe.setText(formatarTamanho(arquivo.length()));
        aplicarIconeArquivo(icone, arquivo.getName());

        item.setOnClickListener(v -> {
            if (acoes != null) acoes.aoEditarArquivo(arquivo);
        });

        item.setOnLongClickListener(v -> {
            menuContexto(arquivo, false);
            return true;
        });

        return item;
    }

    private void addAviso(String texto) {
        TextView v = new TextView(ctx);
        v.setText(texto);
        v.setTextColor(Color.parseColor("#52525B"));
        v.setTextSize(12);
        v.setPadding(dp(24), dp(24), dp(24), dp(24));
        conteinerArvore.addView(v);
    }

    // ==========================================================
    //  Menu principal (⋮)
    // ==========================================================

    private void abrirMenu() {
        List<ItemAcao> itens = new ArrayList<>();

        itens.add(new ItemAcao(R.drawable.ic_novo_ficheiro,
                "Novo arquivo",
                () -> dialogoCriar(pastaRaiz, true)));

        itens.add(new ItemAcao(R.drawable.ic_pasta,
                "Nova pasta",
                () -> dialogoCriar(pastaRaiz, false)));

        itens.add(new ItemAcao(R.drawable.ic_importar,
                "Importar da memória",
                this::abrirImportador));

        if (clipboardArquivo != null) {
            itens.add(new ItemAcao(R.drawable.ic_colar,
                    "Colar aqui",
                    () -> colar(pastaRaiz)));
        }

        itens.add(new ItemAcao(R.drawable.ic_atualizar,
                "Recarregar",
                this::recarregarArvore));

        itens.add(new ItemAcao(R.drawable.ic_limpar,
                "Limpar tudo",
                0xFFFF5252,
                this::confirmarLimparTudo));

        DialogoApp.listaAcoes(ctx, "Memória privada", itens);
    }

    // ==========================================================
    //  Menu de contexto
    // ==========================================================

    private void menuContexto(final File item, boolean ehPasta) {
        List<ItemAcao> itens = new ArrayList<>();

        if (!ehPasta) {
            itens.add(new ItemAcao(R.drawable.ic_ficheiro_codigo,
                    "Editar no editor",
                    () -> {
                        if (acoes != null) acoes.aoEditarArquivo(item);
                    }));
        }

        itens.add(new ItemAcao(R.drawable.ic_renomear,
                "Renomear",
                () -> dialogoRenomear(item)));

        itens.add(new ItemAcao(R.drawable.ic_copiar,
                "Copiar",
                () -> {
                    clipboardArquivo = item;
                    clipboardEhRecorte = false;
                    atualizarBarraClipboard();
                    toast("Copiado: " + item.getName());
                }));

        itens.add(new ItemAcao(R.drawable.ic_recortar,
                "Recortar",
                () -> {
                    clipboardArquivo = item;
                    clipboardEhRecorte = true;
                    atualizarBarraClipboard();
                    toast("Recortado: " + item.getName());
                }));

        if (ehPasta) {
            itens.add(new ItemAcao(R.drawable.ic_novo_ficheiro,
                    "Novo arquivo aqui",
                    () -> dialogoCriar(item, true)));

            itens.add(new ItemAcao(R.drawable.ic_pasta,
                    "Nova pasta aqui",
                    () -> dialogoCriar(item, false)));

            if (clipboardArquivo != null) {
                itens.add(new ItemAcao(R.drawable.ic_colar,
                        "Colar dentro",
                        () -> colar(item)));
            }
        }

        itens.add(new ItemAcao(R.drawable.ic_limpar,
                "Excluir",
                0xFFFF5252,
                () -> confirmarDeletar(item, ehPasta)));

        DialogoApp.listaAcoes(ctx, item.getName(), itens);
    }

    // ==========================================================
    //  Ações — Criar
    // ==========================================================

    private void dialogoCriar(final File pastaDestino, final boolean ehArquivo) {
        String titulo = ehArquivo ? "Novo arquivo" : "Nova pasta";
        String hint   = ehArquivo ? "nome.cpp" : "nome-da-pasta";

        DialogoApp.input(ctx, titulo, hint, "", valor -> {
            if (valor.isEmpty()) {
                toast("Digite um nome.");
                return;
            }

            File novo = new File(pastaDestino, valor);
            if (novo.exists()) {
                toast("Já existe.");
                return;
            }

            boolean ok;
            if (ehArquivo) {
                try {
                    ok = novo.createNewFile();
                } catch (IOException e) {
                    ok = false;
                }
            } else {
                ok = novo.mkdirs();
            }

            if (ok) {
                toast(ehArquivo ? "Arquivo criado." : "Pasta criada.");
                recarregarArvore();
            } else {
                toast("Falha ao criar.");
            }
        });
    }

    // ==========================================================
    //  Ações — Renomear
    // ==========================================================

    private void dialogoRenomear(final File alvo) {
        DialogoApp.input(ctx, "Renomear", alvo.getName(), alvo.getName(), valor -> {
            if (valor.isEmpty() || valor.equals(alvo.getName())) return;

            File novo = new File(alvo.getParentFile(), valor);
            if (novo.exists()) {
                toast("Já existe.");
                return;
            }

            if (alvo.renameTo(novo)) {
                toast("Renomeado.");
                recarregarArvore();
            } else {
                toast("Falha ao renomear.");
            }
        });
    }

    // ==========================================================
    //  Ações — Deletar
    // ==========================================================

    private void confirmarDeletar(final File item, boolean ehPasta) {
        String aviso = ehPasta
                ? "Todos os arquivos dentro serão apagados. Esta ação não pode ser desfeita."
                : "Esta ação não pode ser desfeita.";

        DialogoApp.confirmar(ctx,
                "Excluir " + (ehPasta ? "pasta" : "arquivo") + "?",
                item.getName() + "\n\n" + aviso,
                "Excluir", "Cancelar", true,
                () -> {
                    boolean ok = deletarRecursivo(item);
                    toast(ok ? "Excluído." : "Falha ao excluir.");
                    recarregarArvore();
                });
    }

    private void confirmarLimparTudo() {
        DialogoApp.confirmar(ctx,
                "Limpar tudo?",
                "Isso vai apagar TODO o conteúdo de:\n\n" + pastaRaiz.getAbsolutePath()
                        + "\n\nEsta ação não pode ser desfeita.",
                "Apagar tudo", "Cancelar", true,
                () -> {
                    File[] filhos = pastaRaiz.listFiles();
                    if (filhos != null) {
                        for (File f : filhos) deletarRecursivo(f);
                    }
                    toast("Conteúdo apagado.");
                    recarregarArvore();
                });
    }

    // ==========================================================
    //  Ações — Clipboard
    // ==========================================================

    private void atualizarBarraClipboard() {
        if (clipboardArquivo == null) {
            barraClipboard.setVisibility(View.GONE);
        } else {
            barraClipboard.setVisibility(View.VISIBLE);
            String prefixo = clipboardEhRecorte ? "✂ " : "📋 ";
            clipboardTexto.setText(prefixo + clipboardArquivo.getName());
        }
    }

    private void colar(File destinoPasta) {
        if (clipboardArquivo == null) return;
        if (destinoPasta == null || !destinoPasta.isDirectory()) {
            toast("Destino inválido.");
            return;
        }

        if (clipboardArquivo.equals(destinoPasta)
                || destinoPasta.getAbsolutePath().startsWith(
                        clipboardArquivo.getAbsolutePath() + File.separator)) {
            toast("Não é possível colar em si mesmo.");
            return;
        }

        File destino = new File(destinoPasta, clipboardArquivo.getName());

        if (destino.exists()) {
            String base = clipboardArquivo.getName();
            int i = 1;
            while (destino.exists()) {
                destino = new File(destinoPasta, base + "_copia" + i);
                i++;
            }
        }

        boolean ok;
        if (clipboardEhRecorte) {
            ok = clipboardArquivo.renameTo(destino);
        } else {
            ok = copiarRecursivo(clipboardArquivo, destino);
        }

        if (ok) {
            toast((clipboardEhRecorte ? "Movido: " : "Colado: ") + destino.getName());
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
            recarregarArvore();
        } else {
            toast("Falha na operação.");
        }
    }

    // ==========================================================
    //  Ações — Importar
    // ==========================================================

    private void abrirImportador() {
        File raiz = Environment.getExternalStorageDirectory();
        if (raiz == null || !raiz.isDirectory()) {
            toast("Armazenamento externo indisponível.");
            return;
        }
        navegarPicker(raiz);
    }

    private void navegarPicker(final File pasta) {
        File[] filhos = pasta.listFiles();
        if (filhos == null) {
            toast("Sem acesso a esta pasta.");
            return;
        }

        List<File> pastas   = new ArrayList<>();
        List<File> arquivos = new ArrayList<>();

        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) pastas.add(f);
            else arquivos.add(f);
        }

        pastas.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        arquivos.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        List<ItemAcao> itens = new ArrayList<>();

        // "Subir"
        File pai = pasta.getParentFile();
        if (pai != null && pai.canRead()) {
            itens.add(new ItemAcao(R.drawable.ic_seta_voltar,
                    "↑  ..",
                    () -> navegarPicker(pai)));
        }

        // Pastas
        for (final File f : pastas) {
            itens.add(new ItemAcao(R.drawable.ic_pasta,
                    f.getName(),
                    () -> navegarPicker(f)));
        }

        // Arquivos
        for (final File f : arquivos) {
            itens.add(new ItemAcao(R.drawable.ic_arquivo_generico,
                    f.getName() + "   " + formatarTamanho(f.length()),
                    () -> importarArquivo(f)));
        }

        if (itens.isEmpty()) {
            toast("Pasta vazia.");
            return;
        }

        DialogoApp.listaAcoes(ctx, "Importar — " + pasta.getName(), itens);
    }

    private void importarArquivo(final File origem) {
        File destino = new File(pastaRaiz, origem.getName());

        if (destino.exists()) {
            String base = origem.getName();
            int i = 1;
            while (destino.exists()) {
                destino = new File(pastaRaiz, base + "_" + i);
                i++;
            }
        }

        if (copiarArquivo(origem, destino)) {
            toast("Importado: " + destino.getName());
            recarregarArvore();
        } else {
            toast("Falha ao importar.");
        }
    }

    // ==========================================================
    //  Helpers de arquivo
    // ==========================================================

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

    private boolean copiarRecursivo(File origem, File destino) {
        try {
            if (origem.isDirectory()) {
                if (!destino.exists() && !destino.mkdirs()) return false;
                File[] filhos = origem.listFiles();
                if (filhos != null) {
                    for (File f : filhos) {
                        if (!copiarRecursivo(f, new File(destino, f.getName()))) return false;
                    }
                }
                return true;
            }
            return copiarArquivo(origem, destino);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean copiarArquivo(File origem, File destino) {
        try (InputStream in = new FileInputStream(origem);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==========================================================
    //  Helpers visuais
    // ==========================================================

    private void aplicarIconeArquivo(ImageView iv, String nomeArquivo) {
        String nome = nomeArquivo.toLowerCase();
        int cor;

        if (nome.endsWith(".cpp") || nome.endsWith(".c") || nome.endsWith(".cc")
                || nome.endsWith(".cxx")) {
            iv.setImageResource(R.drawable.ic_arquivo_cpp);
            cor = Color.parseColor("#00E676");
        } else if (nome.endsWith(".h") || nome.endsWith(".hpp") || nome.endsWith(".hxx")) {
            iv.setImageResource(R.drawable.ic_arquivo_header);
            cor = Color.parseColor("#A78BFA");
        } else if (nome.endsWith(".so")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#F59E0B");
        } else if (nome.endsWith(".json") || nome.endsWith(".xml")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#60A5FA");
        } else if (nome.endsWith(".md") || nome.endsWith(".txt")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#A1A1AA");
        } else if (nome.endsWith(".sh") || nome.endsWith(".properties")
                || nome.endsWith(".gradle")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#FBBF24");
        } else {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#6B7280");
        }

        iv.setColorFilter(cor);
    }

    private String contarConteudo(File pasta) {
        File[] filhos = pasta.listFiles();
        if (filhos == null) return "";
        int qtd = 0;
        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            qtd++;
        }
        return qtd == 0 ? "" : String.valueOf(qtd);
    }

    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f K", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f M", bytes / (1024.0 * 1024));
        return String.format("%.2f G", bytes / (1024.0 * 1024 * 1024));
    }

    private String relativizarCaminho(File pasta) {
        String full = pasta.getAbsolutePath();
        int idx = full.indexOf("/files");
        if (idx >= 0) return "files" + full.substring(idx + 6);
        return full;
    }

    private void toast(String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}