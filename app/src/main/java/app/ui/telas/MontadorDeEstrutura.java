package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ui.dialogos.DialogoApp;
import ui.dialogos.ItemAcao;    // ← ADICIONAR
import app.R;

public class MontadorDeEstrutura {

    public interface AcoesEstrutura {
        void aoSelecionarArquivo(File arquivo);
        void aoVoltar();
        default void aoAbrirConfigs(File pastaProjeto) { }
    }

    // ==========================================================
    //  Constantes
    // ==========================================================

    private static final int NUM_ABAS = 2;
    private static final int MAX_RESULTADOS_BUSCA = 300;
    private static final long DEBOUNCE_BUSCA_MS = 250;
    private static final int MAX_BYTES_CONTEUDO = 512 * 1024; // 512 KB por arquivo

    // ==========================================================
    //  Item de menu
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
    //  Resultado de busca (conteúdo)
    // ==========================================================
    private static class ResultadoBusca {
        File   arquivo;
        int    numeroLinha;
        String conteudo;
        String caminhoRelativo;

        ResultadoBusca(File a, int l, String c, String rel) {
            arquivo = a;
            numeroLinha = l;
            conteudo = c;
            caminhoRelativo = rel;
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

    // ==========================================================
    //  Estado
    // ==========================================================
    private final Context contexto;
    private final AcoesEstrutura acoes;

    private File pastaRaiz;

    // Abas
    private int abaAtiva = 0;
    private final Set<String>[] expandidasPorAba;

    // Busca
    private boolean buscaAtiva = false;
    private boolean modoConteudo = false;
    private final Handler handlerBusca = new Handler(Looper.getMainLooper());
    private Runnable runnableBusca;

    // ==========================================================
    //  Views
    // ==========================================================
    private TextView     textNome;
    private TextView     textCaminho;
    private LinearLayout conteinerArvore;

    private LinearLayout barraBusca;
    private View         divisorBusca;
    private EditText     campoBusca;
    private TextView     btnModoBusca;

    private LinearLayout barraAbas;
    private View         divisorAbas;
    private TextView     abaBotao0;
    private TextView     abaBotao1;

    private LinearLayout barraEdicao;
    private TextView     edicaoLabel;
    private EditText     edicaoInput;
    private TextView     edicaoConfirmar;
    private TextView     edicaoCancelar;

    private LinearLayout barraClipboard;
    private TextView     clipboardTexto;

    // ==========================================================
    //  Construtor
    // ==========================================================
    @SuppressWarnings("unchecked")
    public MontadorDeEstrutura(Context contexto, AcoesEstrutura acoes) {
        this.contexto = contexto;
        this.acoes = acoes;
        this.expandidasPorAba = new Set[NUM_ABAS];
        for (int i = 0; i < NUM_ABAS; i++) {
            expandidasPorAba[i] = new HashSet<>();
        }
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construirLayout(File pastaProjeto, String nomeProjeto) {
        View raiz = LayoutInflater.from(contexto)
                .inflate(R.layout.tela_estrutura, null, false);

        // ---- Bind ----
        textNome        = raiz.findViewById(R.id.estruturaNome);
        textCaminho     = raiz.findViewById(R.id.estruturaCaminho);
        conteinerArvore = raiz.findViewById(R.id.estruturaArvore);

        barraBusca   = raiz.findViewById(R.id.estruturaBarraBusca);
        divisorBusca = raiz.findViewById(R.id.estruturaDivisorBusca);
        campoBusca   = raiz.findViewById(R.id.estruturaCampoBusca);
        btnModoBusca = raiz.findViewById(R.id.estruturaBtnModoBusca);

        barraAbas   = raiz.findViewById(R.id.estruturaBarraAbas);
        divisorAbas = raiz.findViewById(R.id.estruturaDivisorAbas);
        abaBotao0   = raiz.findViewById(R.id.estruturaAba0);
        abaBotao1   = raiz.findViewById(R.id.estruturaAba1);

        barraEdicao     = raiz.findViewById(R.id.estruturaBarraEdicao);
        edicaoLabel     = raiz.findViewById(R.id.estruturaEdicaoLabel);
        edicaoInput     = raiz.findViewById(R.id.estruturaEdicaoInput);
        edicaoConfirmar = raiz.findViewById(R.id.estruturaEdicaoConfirmar);
        edicaoCancelar  = raiz.findViewById(R.id.estruturaEdicaoCancelar);

        barraClipboard = raiz.findViewById(R.id.estruturaBarraClipboard);
        clipboardTexto = raiz.findViewById(R.id.estruturaClipboardTexto);

        this.pastaRaiz = pastaProjeto;

        // ---- Toolbar ----
        raiz.findViewById(R.id.estruturaBtnVoltar).setOnClickListener(v -> {
            if (buscaAtiva) {
                fecharBusca();
            } else if (acoes != null) {
                acoes.aoVoltar();
            }
        });

        raiz.findViewById(R.id.estruturaBtnBusca).setOnClickListener(v -> abrirBusca());
        raiz.findViewById(R.id.estruturaBtnConfigs).setOnClickListener(v -> {
            if (pastaRaiz != null && pastaRaiz.exists() && acoes != null) {
                acoes.aoAbrirConfigs(pastaRaiz);
            }
        });
        raiz.findViewById(R.id.estruturaBtnMenu).setOnClickListener(v -> abrirMenuPrincipal());

        // ---- Busca ----
        raiz.findViewById(R.id.estruturaBtnFecharBusca).setOnClickListener(v -> fecharBusca());
        btnModoBusca.setOnClickListener(v -> alternarModoBusca());
        campoBusca.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                agendarBusca(e.toString());
            }
        });
        campoBusca.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executarBuscaImediata(campoBusca.getText().toString());
                esconderTeclado(campoBusca);
                return true;
            }
            return false;
        });

        // ---- Abas ----
        abaBotao0.setOnClickListener(v -> trocarAba(0));
        abaBotao1.setOnClickListener(v -> trocarAba(1));
        atualizarVisualAbas();

        // ---- Edição inline ----
        edicaoConfirmar.setOnClickListener(v -> confirmarEdicao());
        edicaoCancelar.setOnClickListener(v -> cancelarEdicao());
        edicaoInput.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND) {
                confirmarEdicao();
                return true;
            }
            return false;
        });

        // ---- Clipboard ----
        raiz.findViewById(R.id.estruturaBtnColar).setOnClickListener(v -> colarClipboard(pastaRaiz));
        raiz.findViewById(R.id.estruturaBtnCancelarClipboard).setOnClickListener(v -> {
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
        });

        // ---- Título ----
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
    //  Abas
    // ==========================================================

    private void trocarAba(int nova) {
        if (nova == abaAtiva) return;
        abaAtiva = nova;
        atualizarVisualAbas();
        recarregarArvore();
    }

    private void atualizarVisualAbas() {
        if (abaBotao0 == null || abaBotao1 == null) return;

        boolean eh0 = (abaAtiva == 0);

        abaBotao0.setTextColor(eh0
                ? Color.parseColor("#00E676")
                : Color.parseColor("#A1A1AA"));
        abaBotao0.setTypeface(null,
                eh0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);

        abaBotao1.setTextColor(!eh0
                ? Color.parseColor("#00E676")
                : Color.parseColor("#A1A1AA"));
        abaBotao1.setTypeface(null,
                !eh0 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
    }

    // ==========================================================
    //  Busca
    // ==========================================================

    private void abrirBusca() {
        buscaAtiva = true;
        barraBusca.setVisibility(View.VISIBLE);
        divisorBusca.setVisibility(View.VISIBLE);

        // Esconde abas durante busca
        barraAbas.setVisibility(View.GONE);
        divisorAbas.setVisibility(View.GONE);

        campoBusca.setText("");
        campoBusca.requestFocus();

        campoBusca.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager)
                    contexto.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(campoBusca, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);
    }

    private void fecharBusca() {
        buscaAtiva = false;
        modoConteudo = false;
        barraBusca.setVisibility(View.GONE);
        divisorBusca.setVisibility(View.GONE);

        // Restaura abas
        barraAbas.setVisibility(View.VISIBLE);
        divisorAbas.setVisibility(View.VISIBLE);

        campoBusca.setText("");
        esconderTeclado(campoBusca);
        atualizarTextoModoBusca();
        recarregarArvore();
    }

    private void alternarModoBusca() {
        modoConteudo = !modoConteudo;
        atualizarTextoModoBusca();

        String termo = campoBusca.getText().toString();
        if (!termo.isEmpty()) {
            executarBuscaImediata(termo);
        }
    }

    private void atualizarTextoModoBusca() {
        if (btnModoBusca == null) return;
        btnModoBusca.setText(modoConteudo ? "Ab" : "Aa");
        btnModoBusca.setTextColor(modoConteudo
                ? Color.parseColor("#FFC107")
                : Color.parseColor("#00E676"));
    }

    /** Debounce da busca conforme digita. */
    private void agendarBusca(String termo) {
        if (runnableBusca != null) handlerBusca.removeCallbacks(runnableBusca);
        runnableBusca = () -> executarBuscaImediata(termo);
        handlerBusca.postDelayed(runnableBusca, DEBOUNCE_BUSCA_MS);
    }

    /** Executa a busca agora. */
    private void executarBuscaImediata(String termo) {
        if (!buscaAtiva) return;
        recarregarArvore();
    }

    private void esconderTeclado(View v) {
        InputMethodManager imm = (InputMethodManager)
                contexto.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // ==========================================================
    //  Renderização
    // ==========================================================

    private void recarregarArvore() {
        if (conteinerArvore == null) return;
        conteinerArvore.removeAllViews();

        if (pastaRaiz == null || !pastaRaiz.exists()) {
            addAvisoVazio("Nenhum projeto carregado.");
            return;
        }

        if (buscaAtiva && campoBusca != null) {
            String termo = campoBusca.getText().toString().trim();
            if (termo.isEmpty()) {
                addAvisoVazio("Digite algo para buscar.");
                return;
            }
            if (modoConteudo) {
                executarBuscaConteudo(termo);
            } else {
                executarBuscaNome(termo);
            }
            return;
        }

        // Árvore normal
        Set<String> expandidas = expandidasPorAba[abaAtiva];
        desenharConteudo(pastaRaiz, conteinerArvore, 0, expandidas, null);
    }

    private void addAvisoVazio(String texto) {
        TextView v = new TextView(contexto);
        v.setText(texto);
        v.setTextColor(Color.parseColor("#52525B"));
        v.setTextSize(12);
        v.setPadding(dp(24), dp(24), dp(24), dp(24));
        conteinerArvore.addView(v);
    }

    // ---- Árvore normal ----

    private void desenharConteudo(File diretorio,
                                  LinearLayout pai,
                                  int recuo,
                                  Set<String> expandidas,
                                  Set<String> filtroCaminhosValidos) {

        File[] filhos = diretorio.listFiles();
        if (filhos == null) return;

        List<File> validos = new ArrayList<>();
        for (File f : filhos) {
            String nome = f.getName();
            if (nome.startsWith(".")) continue;
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
            vazio.setText("(vazia)");
            vazio.setTextColor(Color.parseColor("#3F3F46"));
            vazio.setTextSize(10);
            vazio.setPadding(dp(12) + recuo + dp(14) + dp(8), dp(8), dp(8), dp(8));
            pai.addView(vazio);
            return;
        }

        for (File f : validos) {
            if (f.isDirectory()) {
                boolean expandida = expandidas.contains(f.getAbsolutePath());
                pai.addView(criarItemPasta(f, recuo, expandida));

                if (expandida) {
                    LinearLayout filho = new LinearLayout(contexto);
                    filho.setOrientation(LinearLayout.VERTICAL);
                    filho.setLayoutParams(new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT));
                    pai.addView(filho);
                    desenharConteudo(f, filho, recuo + 1, expandidas, filtroCaminhosValidos);
                }
            } else {
                pai.addView(criarItemArquivo(f, recuo));
            }
        }
    }

    // ---- Busca por nome ----

    private void executarBuscaNome(String termo) {
        String t = termo.toLowerCase();
        Set<String> expandidas = new HashSet<>();
        List<View> views = new ArrayList<>();

        buscarPorNome(pastaRaiz, t, 0, expandidas, views);

        if (views.isEmpty()) {
            addAvisoVazio("Nada encontrado para \"" + termo + "\"");
            return;
        }

        for (View v : views) conteinerArvore.addView(v);
    }

    private boolean buscarPorNome(File pasta, String termo, int recuo,
                                  Set<String> expandidas, List<View> saida) {
        File[] filhos = pasta.listFiles();
        if (filhos == null) return false;

        // Ordena
        List<File> validos = new ArrayList<>();
        for (File f : filhos) {
            String nome = f.getName();
            if (nome.startsWith(".")) continue;
            if (nome.equals("Projeto.properties")) continue;
            validos.add(f);
        }
        validos.sort((a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        boolean algumMatch = false;

        for (File f : validos) {
            boolean nomeCasa = f.getName().toLowerCase().contains(termo);

            if (f.isDirectory()) {
                // Tenta dentro primeiro
                List<View> subviews = new ArrayList<>();
                boolean dentroCasa = buscarPorNome(f, termo, recuo + 1, expandidas, subviews);

                if (nomeCasa || dentroCasa) {
                    saida.add(criarItemPasta(f, recuo, dentroCasa));
                    expandidas.add(f.getAbsolutePath());
                    if (dentroCasa) {
                        saida.addAll(subviews);
                    }
                    algumMatch = true;
                }
            } else {
                if (nomeCasa) {
                    saida.add(criarItemArquivo(f, recuo));
                    algumMatch = true;
                }
            }
        }

        return algumMatch;
    }

    // ---- Busca por conteúdo ----

    private void executarBuscaConteudo(String termo) {
        List<ResultadoBusca> resultados = new ArrayList<>();
        buscarPorConteudo(pastaRaiz, termo, resultados);

        if (resultados.isEmpty()) {
            addAvisoVazio("Nenhuma linha contém \"" + termo + "\"");
            return;
        }

        for (ResultadoBusca r : resultados) {
            conteinerArvore.addView(criarItemResultado(r));
        }

        if (resultados.size() >= MAX_RESULTADOS_BUSCA) {
            TextView aviso = new TextView(contexto);
            aviso.setText("Mostrando os primeiros " + MAX_RESULTADOS_BUSCA
                    + " resultados. Refine sua busca.");
            aviso.setTextColor(Color.parseColor("#FFC107"));
            aviso.setTextSize(10);
            aviso.setPadding(dp(16), dp(12), dp(16), dp(12));
            conteinerArvore.addView(aviso);
        }
    }

    private void buscarPorConteudo(File pasta,
                                   String termo,
                                   List<ResultadoBusca> saida) {
        if (saida.size() >= MAX_RESULTADOS_BUSCA) return;

        File[] filhos = pasta.listFiles();
        if (filhos == null) return;

        for (File f : filhos) {
            if (saida.size() >= MAX_RESULTADOS_BUSCA) return;
            String nome = f.getName();
            if (nome.startsWith(".")) continue;
            if (nome.equals("Projeto.properties")) continue;

            if (f.isDirectory()) {
                // Evita entrar em deps/ (muito grande) e lib/ (binários)
                if (nome.equals("deps")) continue;
                buscarPorConteudo(f, termo, saida);
            } else {
                if (f.length() > MAX_BYTES_CONTEUDO) continue;
                if (!ehTexto(f.getName())) continue;
                buscarNoArquivo(f, termo, saida);
            }
        }
    }

    private void buscarNoArquivo(File arquivo, String termo, List<ResultadoBusca> saida) {
        String termoLower = termo.toLowerCase();

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(arquivo), "UTF-8"))) {

            String linha;
            int numero = 0;
            while ((linha = br.readLine()) != null) {
                numero++;
                if (linha.toLowerCase().contains(termoLower)) {
                    String rel = caminhoRelativo(pastaRaiz, arquivo);
                    saida.add(new ResultadoBusca(
                            arquivo, numero, linha.trim(), rel));
                    if (saida.size() >= MAX_RESULTADOS_BUSCA) return;
                }
            }
        } catch (IOException ignored) {
            // arquivo não-texto ou ilegível — ignora
        }
    }

    private boolean ehTexto(String nome) {
        String n = nome.toLowerCase();
        if (n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg")
                || n.endsWith(".gif") || n.endsWith(".so") || n.endsWith(".a")
                || n.endsWith(".o") || n.endsWith(".zip") || n.endsWith(".jar")
                || n.endsWith(".ttf") || n.endsWith(".otf") || n.endsWith(".mp3")
                || n.endsWith(".mp4") || n.endsWith(".apk") || n.endsWith(".dex")) {
            return false;
        }
        return true;
    }

    // ---- Itens de árvore ----

    private View criarItemPasta(File pasta, int recuo, boolean expandida) {
        View item = LayoutInflater.from(contexto)
                .inflate(R.layout.item_estrutura_pasta, conteinerArvore, false);

        // Indentação
        item.setPadding(
                dp(12) + recuo * dp(16),
                dp(6), dp(12), dp(6));

        TextView seta    = item.findViewById(R.id.itemPastaSeta);
        ImageView icone  = item.findViewById(R.id.itemPastaIcone);
        TextView nome    = item.findViewById(R.id.itemPastaNome);
        TextView detalhe = item.findViewById(R.id.itemPastaDetalhe);

        nome.setText(pasta.getName());
        detalhe.setText(contarConteudo(pasta));
        seta.setText(expandida ? "▼" : "▶");

        item.setOnClickListener(v -> {
            Set<String> expandidas = expandidasPorAba[abaAtiva];
            String path = pasta.getAbsolutePath();
            if (expandidas.contains(path)) {
                expandidas.remove(path);
            } else {
                expandidas.add(path);
            }
            recarregarArvore();
        });

        item.setOnLongClickListener(v -> {
            menuItem(pasta, true);
            return true;
        });

        return item;
    }

    private View criarItemArquivo(File arquivo, int recuo) {
        View item = LayoutInflater.from(contexto)
                .inflate(R.layout.item_estrutura_arquivo, conteinerArvore, false);

        item.setPadding(
                dp(12) + recuo * dp(16),
                dp(6), dp(12), dp(6));

        ImageView icone  = item.findViewById(R.id.itemArquivoIcone);
        TextView nome    = item.findViewById(R.id.itemArquivoNome);
        TextView detalhe = item.findViewById(R.id.itemArquivoDetalhe);

        nome.setText(arquivo.getName());
        detalhe.setText(formatarTamanho(arquivo.length()));
        aplicarIconeArquivo(icone, arquivo.getName());

        item.setOnClickListener(v -> {
            if (acoes != null) acoes.aoSelecionarArquivo(arquivo);
        });

        item.setOnLongClickListener(v -> {
            menuItem(arquivo, false);
            return true;
        });

        return item;
    }

    private View criarItemResultado(ResultadoBusca r) {
        View item = LayoutInflater.from(contexto)
                .inflate(R.layout.item_estrutura_resultado, conteinerArvore, false);

        ImageView icone  = item.findViewById(R.id.itemResultadoIcone);
        TextView nome    = item.findViewById(R.id.itemResultadoNome);
        TextView linha   = item.findViewById(R.id.itemResultadoLinha);
        TextView caminho = item.findViewById(R.id.itemResultadoCaminho);
        TextView conteudo = item.findViewById(R.id.itemResultadoConteudo);

        nome.setText(r.arquivo.getName());
        linha.setText("L" + r.numeroLinha);
        caminho.setText(r.caminhoRelativo);
        conteudo.setText(r.conteudo);
        aplicarIconeArquivo(icone, r.arquivo.getName());

        item.setOnClickListener(v -> {
            if (acoes != null) acoes.aoSelecionarArquivo(r.arquivo);
        });

        return item;
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

        itens.add(new ItemMenu(
                R.drawable.ic_config,
                "Configurações do projeto",
                () -> {
                    if (acoes != null) acoes.aoAbrirConfigs(pastaRaiz);
                }));

        mostrarMenuComIcones("Ações", itens);
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
                if (valorInicial != null) edicaoInput.setSelection(valorInicial.length());
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
        esconderTeclado(edicaoInput);
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
            Toast.makeText(contexto, "Já existe.", Toast.LENGTH_SHORT).show();
            return false;
        }

        try {
            if (ehArquivo) {
                if (!novo.createNewFile()) {
                    Toast.makeText(contexto, "Falha ao criar.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            } else {
                if (!novo.mkdirs()) {
                    Toast.makeText(contexto, "Falha ao criar.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            Toast.makeText(contexto, "Criado.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(contexto, "Já existe.", Toast.LENGTH_SHORT).show();
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

    // ==========================================================
    //  Menu com ícones (AlertDialog customizado)
    // ==========================================================

    private void mostrarMenuComIcones(String titulo, List<ItemMenu> itens) {
    List<ItemAcao> acoes = new ArrayList<>();
    for (ItemMenu it : itens) {
        acoes.add(new ItemAcao(it.iconeRes, it.texto, it.acao));
    }
    DialogoApp.listaAcoes(contexto, titulo, acoes);
}
    

    // ==========================================================
    //  Menu de contexto
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

    private void confirmarDeletar(final File item, boolean ehPasta) {
    String aviso = ehPasta
            ? "Todos os arquivos dentro serão apagados. Esta ação não pode ser desfeita."
            : "Esta ação não pode ser desfeita.";

    DialogoApp.confirmar(
            contexto,
            "Excluir " + (ehPasta ? "pasta" : "arquivo") + "?",
            item.getName() + "\n\n" + aviso,
            "Excluir",
            "Cancelar",
            true,
            () -> {
                boolean ok = deletarRecursivo(item);
                Toast.makeText(contexto,
                        ok ? "Excluído." : "Falha ao excluir.",
                        Toast.LENGTH_SHORT).show();
                recarregarArvore();
            });
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
    //  Ícones
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
        } else if (nome.endsWith(".md") || nome.endsWith(".txt")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#A1A1AA");
        } else if (nome.endsWith(".json") || nome.endsWith(".xml")) {
            iv.setImageResource(R.drawable.ic_arquivo_generico);
            cor = Color.parseColor("#60A5FA");
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

    // ==========================================================
    //  Helpers
    // ==========================================================

    private String contarConteudo(File pasta) {
        File[] filhos = pasta.listFiles();
        if (filhos == null) return "";

        int qtd = 0;
        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue;
            if (f.getName().equals("Projeto.properties")) continue;
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

    private String caminhoRelativo(File raiz, File arquivo) {
        try {
            String base = raiz.getAbsolutePath();
            String full = arquivo.getAbsolutePath();
            if (full.startsWith(base + File.separator)) {
                return full.substring(base.length() + 1);
            }
        } catch (Exception ignored) { }
        return arquivo.getName();
    }

    private int dp(int v) {
        return (int) (v * contexto.getResources().getDisplayMetrics().density);
    }
}