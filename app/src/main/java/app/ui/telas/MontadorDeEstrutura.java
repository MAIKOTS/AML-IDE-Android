package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
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
import java.util.List;

import app.R;

public class MontadorDeEstrutura {

    public interface AcoesEstrutura {
        void aoSelecionarArquivo(File arquivo);
        void aoVoltar();
    }

    // ==========================================================
    //  Item de menu (ícone + texto + ação)
    // ==========================================================
    private static class ItemMenu {
        final int iconeRes;
        final String texto;
        final Runnable acao;

        ItemMenu(int iconeRes, String texto, Runnable acao) {
            this.iconeRes = iconeRes;
            this.texto = texto;
            this.acao = acao;
        }
    }

    // ==========================================================
    //  Clipboard estático
    // ==========================================================
    private static File clipboardArquivo = null;
    private static boolean clipboardEhRecorte = false;

    // ==========================================================
    //  Modo de edição inline
    // ==========================================================
    private enum ModoEdicao { NENHUM, CRIAR_ARQUIVO, CRIAR_PASTA, RENOMEAR }

    private ModoEdicao modoAtual = ModoEdicao.NENHUM;
    private File pastaDestino = null;
    private File arquivoAlvo = null;

    private final Context contexto;
    private final AcoesEstrutura acoes;

    private File pastaRaiz;

    private TextView textNome;
    private TextView textCaminho;
    private LinearLayout conteinerArvore;

    private LinearLayout barraEdicao;
    private TextView edicaoLabel;
    private EditText edicaoInput;
    private TextView edicaoConfirmar;
    private TextView edicaoCancelar;

    private LinearLayout barraClipboard;
    private TextView clipboardTexto;

    public MontadorDeEstrutura(Context contexto, AcoesEstrutura acoes) {
        this.contexto = contexto;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construirLayout(File pastaProjeto, String nomeProjeto) {
        View raiz = LayoutInflater.from(contexto)
                .inflate(R.layout.tela_estrutura, null, false);

        textNome        = raiz.findViewById(R.id.estruturaNome);
        textCaminho     = raiz.findViewById(R.id.estruturaCaminho);
        conteinerArvore = raiz.findViewById(R.id.estruturaArvore);

        barraEdicao     = raiz.findViewById(R.id.estruturaBarraEdicao);
        edicaoLabel     = raiz.findViewById(R.id.estruturaEdicaoLabel);
        edicaoInput     = raiz.findViewById(R.id.estruturaEdicaoInput);
        edicaoConfirmar = raiz.findViewById(R.id.estruturaEdicaoConfirmar);
        edicaoCancelar  = raiz.findViewById(R.id.estruturaEdicaoCancelar);

        barraClipboard  = raiz.findViewById(R.id.estruturaBarraClipboard);
        clipboardTexto  = raiz.findViewById(R.id.estruturaClipboardTexto);

        this.pastaRaiz = pastaProjeto;

        raiz.findViewById(R.id.estruturaBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });

        raiz.findViewById(R.id.estruturaBtnMenu).setOnClickListener(v -> abrirMenuPrincipal());

        edicaoConfirmar.setOnClickListener(v -> confirmarEdicao());
        edicaoCancelar.setOnClickListener(v -> cancelarEdicao());

        edicaoInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND) {
                confirmarEdicao();
                return true;
            }
            return false;
        });

        raiz.findViewById(R.id.estruturaBtnColar).setOnClickListener(v -> colarClipboard(pastaRaiz));
        raiz.findViewById(R.id.estruturaBtnCancelarClipboard).setOnClickListener(v -> {
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
        });

        if (pastaProjeto != null && pastaProjeto.exists()) {
            textNome.setText(nomeProjeto != null ? nomeProjeto : pastaProjeto.getName());
            textCaminho.setText(pastaProjeto.getAbsolutePath());
        } else {
            textNome.setText("Nenhum projeto carregado");
            textCaminho.setText("");
        }

        atualizarBarraClipboard();
        recarregarArvore();
        return raiz;
    }

    // ==========================================================
    //  Barra de edição inline
    // ==========================================================

    private void abrirBarraEdicao(ModoEdicao modo, File pastaDestino,
                                  File arquivoAlvo, String valorInicial) {
        this.modoAtual = modo;
        this.pastaDestino = pastaDestino;
        this.arquivoAlvo = arquivoAlvo;

        switch (modo) {
            case CRIAR_ARQUIVO:
                edicaoLabel.setText("Novo arquivo:");
                edicaoInput.setHint("nome.cpp");
                edicaoInput.setText("");
                break;
            case CRIAR_PASTA:
                edicaoLabel.setText("Nova pasta:");
                edicaoInput.setHint("nome-da-pasta");
                edicaoInput.setText("");
                break;
            case RENOMEAR:
                edicaoLabel.setText("Renomear:");
                edicaoInput.setHint("novo-nome");
                edicaoInput.setText(valorInicial != null ? valorInicial : "");
                if (valorInicial != null) {
                    edicaoInput.setSelection(valorInicial.length());
                }
                break;
            default:
                return;
        }

        barraEdicao.setVisibility(View.VISIBLE);
        edicaoInput.requestFocus();
        edicaoInput.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                    contexto.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(edicaoInput, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
    }

    private void cancelarEdicao() {
        this.modoAtual = ModoEdicao.NENHUM;
        this.pastaDestino = null;
        this.arquivoAlvo = null;
        barraEdicao.setVisibility(View.GONE);
        edicaoInput.setText("");
        esconderTeclado();
    }

    private void confirmarEdicao() {
        String texto = edicaoInput.getText().toString().trim();
        if (texto.isEmpty()) {
            Toast.makeText(contexto, "Digite um nome.", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean sucesso = false;

        switch (modoAtual) {
            case CRIAR_ARQUIVO:
                sucesso = criarNovo(pastaDestino, texto, true);
                break;
            case CRIAR_PASTA:
                sucesso = criarNovo(pastaDestino, texto, false);
                break;
            case RENOMEAR:
                sucesso = renomear(arquivoAlvo, texto);
                break;
            default:
                return;
        }

        if (sucesso) {
            cancelarEdicao();
            recarregarArvore();
        }
    }

    private boolean criarNovo(File pastaDestino, String nome, boolean ehArquivo) {
        if (pastaDestino == null || !pastaDestino.isDirectory()) return false;

        File novo = new File(pastaDestino, nome);
        if (novo.exists()) {
            Toast.makeText(contexto, "Já existe um item com esse nome.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            if (ehArquivo) {
                if (!novo.createNewFile()) {
                    Toast.makeText(contexto, "Falha ao criar arquivo.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                if (!novo.mkdirs()) {
                    Toast.makeText(contexto, "Falha ao criar pasta.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            Toast.makeText(contexto,
                    (ehArquivo ? "Arquivo criado" : "Pasta criada"),
                    Toast.LENGTH_SHORT).show();
            return true;
        } catch (IOException e) {
            Toast.makeText(contexto, "Erro: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private boolean renomear(File alvo, String novoNome) {
        if (alvo == null || !alvo.exists()) return false;
        if (novoNome.equals(alvo.getName())) return false;

        File novo = new File(alvo.getParentFile(), novoNome);
        if (novo.exists()) {
            Toast.makeText(contexto, "Já existe um item com esse nome.", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (alvo.renameTo(novo)) {
            Toast.makeText(contexto, "Renomeado.", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            Toast.makeText(contexto, "Falha ao renomear.", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    private void esconderTeclado() {
        InputMethodManager imm = (InputMethodManager)
                contexto.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(edicaoInput.getWindowToken(), 0);
    }

    // ==========================================================
    //  Menu com ícones (AlertDialog customizado)
    // ==========================================================

    private void mostrarMenuComIcones(String titulo, List<ItemMenu> itens) {
        // Adapter customizado
        MenuComIconeAdapter adapter = new MenuComIconeAdapter(itens);

        AlertDialog.Builder builder = new AlertDialog.Builder(contexto);
        builder.setTitle(titulo);

        final AlertDialog dialog = builder.setAdapter(adapter, null).create();

        adapter.setOnItemClick(pos -> {
            dialog.dismiss();
            itens.get(pos).acao.run();
        });

        dialog.show();
    }

    /** Adapter que exibe ícone + texto no AlertDialog. */
    private class MenuComIconeAdapter extends BaseAdapter {

        interface OnItemClick { void onClick(int pos); }

        private final List<ItemMenu> itens;
        private OnItemClick listener;

        MenuComIconeAdapter(List<ItemMenu> itens) {
            this.itens = itens;
        }

        void setOnItemClick(OnItemClick l) { this.listener = l; }

        @Override public int getCount() { return itens.size(); }
        @Override public Object getItem(int i) { return itens.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ItemMenu item = itens.get(pos);

            LinearLayout linha = new LinearLayout(contexto);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.CENTER_VERTICAL);
            linha.setPadding(dp(20), dp(14), dp(20), dp(14));
            linha.setClickable(true);

            ImageView icone = new ImageView(contexto);
            LinearLayout.LayoutParams lpIcone = new LinearLayout.LayoutParams(dp(22), dp(22));
            lpIcone.setMargins(0, 0, dp(18), 0);
            icone.setLayoutParams(lpIcone);
            icone.setImageResource(item.iconeRes);
            icone.setColorFilter(Color.parseColor("#00E676"));
            linha.addView(icone);

            TextView texto = new TextView(contexto);
            texto.setText(item.texto);
            texto.setTextColor(Color.parseColor("#F4F4F5"));
            texto.setTextSize(14);
            linha.addView(texto);

            linha.setOnClickListener(v -> {
                if (listener != null) listener.onClick(pos);
            });

            return linha;
        }
    }

    // ==========================================================
    //  Menu principal (toolbar ⋮)
    // ==========================================================

    private void abrirMenuPrincipal() {
        if (pastaRaiz == null || !pastaRaiz.exists()) return;

        List<ItemMenu> itens = new ArrayList<>();

        itens.add(new ItemMenu(
                R.drawable.ic_novo_ficheiro,
                "Novo arquivo",
                () -> abrirBarraEdicao(ModoEdicao.CRIAR_ARQUIVO, pastaRaiz, null, null)));

        itens.add(new ItemMenu(
                R.drawable.ic_pasta,
                "Nova pasta",
                () -> abrirBarraEdicao(ModoEdicao.CRIAR_PASTA, pastaRaiz, null, null)));

        if (clipboardArquivo != null) {
            itens.add(new ItemMenu(
                    R.drawable.ic_colar,
                    "Colar aqui",
                    () -> colarClipboard(pastaRaiz)));
        }

        itens.add(new ItemMenu(
                R.drawable.ic_atualizar,
                "Atualizar",
                this::recarregarArvore));

        mostrarMenuComIcones("Ações", itens);
    }

    // ==========================================================
    //  Recarrega a árvore
    // ==========================================================

    private void recarregarArvore() {
        conteinerArvore.removeAllViews();

        if (pastaRaiz == null || !pastaRaiz.exists()) {
            TextView vazio = new TextView(contexto);
            vazio.setText("Nenhum projeto carregado.");
            vazio.setTextColor(Color.parseColor("#6B7280"));
            vazio.setPadding(dp(24), dp(24), dp(24), dp(24));
            conteinerArvore.addView(vazio);
            return;
        }

        desenharConteudo(pastaRaiz, conteinerArvore, 0);
    }

    private void desenharConteudo(File diretorio, LinearLayout conteinerPai, int recuo) {
    File[] filhos = diretorio.listFiles();
    if (filhos == null) return;

    List<File> validos = new ArrayList<>();
    for (File f : filhos) {
        String nome = f.getName();

        // Ignora ocultos (.git, .idea, etc.)
        if (nome.startsWith(".")) continue;

        // ★ Projeto.properties é editado via botão "Configs"
        //   no editor — não aparece na árvore.
        if (nome.equals("Projeto.properties")) continue;

        validos.add(f);
    }

    validos.sort((a, b) -> {
        if (a.isDirectory() && !b.isDirectory()) return -1;
        if (!a.isDirectory() && b.isDirectory()) return 1;
        return a.getName().compareToIgnoreCase(b.getName());
    });

    if (validos.isEmpty()) {
        TextView vazio = new TextView(contexto);
        vazio.setText("(pasta vazia)");
        vazio.setTextColor(Color.parseColor("#52525B"));
        vazio.setTextSize(11);
        vazio.setPadding(dp(24) + recuo, dp(16), dp(16), dp(16));
        conteinerPai.addView(vazio);
        return;
    }

    for (File f : validos) {
        if (f.isDirectory()) {
            conteinerPai.addView(criarItemPasta(f, recuo));
        } else {
            conteinerPai.addView(criarItemArquivo(f, recuo));
        }
    }
}

    // ==========================================================
    //  Item de pasta
    // ==========================================================

    private View criarItemPasta(File pasta, int recuo) {
        LinearLayout wrapper = new LinearLayout(contexto);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout conteinerFilhos = new LinearLayout(contexto);
        conteinerFilhos.setOrientation(LinearLayout.VERTICAL);
        conteinerFilhos.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View card = LayoutInflater.from(contexto)
                .inflate(R.layout.item_estrutura_pasta, wrapper, false);

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(recuo, dp(3), 0, dp(3));
        card.setLayoutParams(lpCard);

        TextView nome    = card.findViewById(R.id.itemPastaNome);
        TextView detalhe = card.findViewById(R.id.itemPastaDetalhe);
        TextView seta    = card.findViewById(R.id.itemPastaSeta);
        TextView menu    = card.findViewById(R.id.itemPastaMenu);

        nome.setText(pasta.getName());
        detalhe.setText(contarConteudo(pasta));

        final boolean[] expandido = {false};

        card.setOnClickListener(v -> {
            if (expandido[0]) {
                conteinerFilhos.removeAllViews();
                seta.setText("▶");
                expandido[0] = false;
            } else {
                desenharConteudo(pasta, conteinerFilhos, recuo + dp(16));
                seta.setText("▼");
                expandido[0] = true;
            }
        });

        card.setOnLongClickListener(v -> {
            menuItem(pasta, true);
            return true;
        });

        menu.setOnClickListener(v -> menuItem(pasta, true));

        wrapper.addView(card);
        wrapper.addView(conteinerFilhos);
        return wrapper;
    }

    // ==========================================================
    //  Item de arquivo
    // ==========================================================

    private View criarItemArquivo(File arquivo, int recuo) {
        View card = LayoutInflater.from(contexto)
                .inflate(R.layout.item_estrutura_arquivo, null, false);

        LinearLayout.LayoutParams lpCard = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCard.setMargins(recuo, dp(3), 0, dp(3));
        card.setLayoutParams(lpCard);

        TextView nome    = card.findViewById(R.id.itemArquivoNome);
        TextView detalhe = card.findViewById(R.id.itemArquivoDetalhe);
        TextView menu    = card.findViewById(R.id.itemArquivoMenu);
        ImageView icone  = card.findViewById(R.id.itemArquivoIcone);

        nome.setText(arquivo.getName());
        detalhe.setText(formatarTamanho(arquivo.length()));
        aplicarIconeArquivo(icone, arquivo.getName());

        card.setOnClickListener(v -> {
            if (acoes != null) acoes.aoSelecionarArquivo(arquivo);
        });

        card.setOnLongClickListener(v -> {
            menuItem(arquivo, false);
            return true;
        });

        menu.setOnClickListener(v -> menuItem(arquivo, false));

        return card;
    }

    // ==========================================================
    //  Menu de contexto (arquivo ou pasta)
    // ==========================================================

    private void menuItem(File item, boolean ehPasta) {
        List<ItemMenu> itens = new ArrayList<>();

        if (!ehPasta) {
            itens.add(new ItemMenu(
                    R.drawable.ic_pasta_aberta,
                    "Abrir",
                    () -> {
                        if (acoes != null) acoes.aoSelecionarArquivo(item);
                    }));
        }

        itens.add(new ItemMenu(
                R.drawable.ic_renomear,
                "Renomear",
                () -> abrirBarraEdicao(ModoEdicao.RENOMEAR, null, item, item.getName())));

        itens.add(new ItemMenu(
                R.drawable.ic_copiar,
                "Copiar",
                () -> {
                    clipboardArquivo = item;
                    clipboardEhRecorte = false;
                    atualizarBarraClipboard();
                    Toast.makeText(contexto, "Copiado: " + item.getName(),
                            Toast.LENGTH_SHORT).show();
                }));

        itens.add(new ItemMenu(
                R.drawable.ic_recortar,
                "Recortar",
                () -> {
                    clipboardArquivo = item;
                    clipboardEhRecorte = true;
                    atualizarBarraClipboard();
                    Toast.makeText(contexto, "Recortado: " + item.getName(),
                            Toast.LENGTH_SHORT).show();
                }));

        if (ehPasta) {
            itens.add(new ItemMenu(
                    R.drawable.ic_novo_ficheiro,
                    "Novo arquivo aqui",
                    () -> abrirBarraEdicao(ModoEdicao.CRIAR_ARQUIVO, item, null, null)));

            itens.add(new ItemMenu(
                    R.drawable.ic_pasta,
                    "Nova pasta aqui",
                    () -> abrirBarraEdicao(ModoEdicao.CRIAR_PASTA, item, null, null)));

            if (clipboardArquivo != null) {
                itens.add(new ItemMenu(
                        R.drawable.ic_colar,
                        "Colar dentro",
                        () -> colarClipboard(item)));
            }
        }

        itens.add(new ItemMenu(
                R.drawable.ic_limpar,
                "Excluir",
                () -> confirmarDeletar(item, ehPasta)));

        mostrarMenuComIcones(item.getName(), itens);
    }

    // ==========================================================
    //  Excluir
    // ==========================================================

    private void confirmarDeletar(File item, boolean ehPasta) {
        String aviso = ehPasta
                ? "Todos os arquivos dentro serão apagados. Esta ação não pode ser desfeita."
                : "Esta ação não pode ser desfeita.";

        new AlertDialog.Builder(contexto)
                .setTitle("Excluir " + (ehPasta ? "pasta" : "arquivo") + "?")
                .setMessage(item.getName() + "\n\n" + aviso)
                .setPositiveButton("Excluir", (d, w) -> {
                    boolean ok = deletarRecursivo(item);
                    Toast.makeText(contexto,
                            ok ? "Excluído." : "Falha ao excluir.",
                            Toast.LENGTH_SHORT).show();
                    recarregarArvore();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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

    // ==========================================================
    //  Clipboard
    // ==========================================================

    private void atualizarBarraClipboard() {
        if (clipboardArquivo == null) {
            barraClipboard.setVisibility(View.GONE);
        } else {
            barraClipboard.setVisibility(View.VISIBLE);
            String prefixo = clipboardEhRecorte ? "Recortado: " : "Copiado: ";
            clipboardTexto.setText(prefixo + clipboardArquivo.getName());
        }
    }

    private void colarClipboard(File destinoPasta) {
        if (clipboardArquivo == null) return;
        if (destinoPasta == null || !destinoPasta.isDirectory()) {
            Toast.makeText(contexto, "Destino inválido.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (clipboardArquivo.equals(destinoPasta)
                || destinoPasta.getAbsolutePath().startsWith(
                        clipboardArquivo.getAbsolutePath() + File.separator)) {
            Toast.makeText(contexto, "Não é possível colar em si mesmo.",
                    Toast.LENGTH_SHORT).show();
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
            Toast.makeText(contexto,
                    (clipboardEhRecorte ? "Movido: " : "Colado: ") + destino.getName(),
                    Toast.LENGTH_SHORT).show();
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
            recarregarArvore();
        } else {
            Toast.makeText(contexto, "Falha na operação.", Toast.LENGTH_SHORT).show();
        }
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
            } else {
                return copiarArquivo(origem, destino);
            }
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
    //  Ícones por extensão
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
        } else if (nome.endsWith(".txt") || nome.endsWith(".md")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#A1A1AA");
        } else {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#6B7280");
        }

        iv.setColorFilter(cor);
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private String contarConteudo(File pasta) {
        File[] filhos = pasta.listFiles();
        if (filhos == null || filhos.length == 0) return "vazia";

        int pastas = 0, arquivos = 0;
        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            if (f.isDirectory()) pastas++; else arquivos++;
        }

        if (pastas == 0 && arquivos == 0) return "vazia";
        StringBuilder sb = new StringBuilder();
        if (pastas > 0) sb.append(pastas).append(" pasta").append(pastas != 1 ? "s" : "");
        if (pastas > 0 && arquivos > 0) sb.append(", ");
        if (arquivos > 0) sb.append(arquivos).append(" arq.");
        return sb.toString();
    }

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