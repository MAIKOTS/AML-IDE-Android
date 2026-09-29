package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.R;
import ui.componentes.GutterView;
import ui.componentes.ScrollView2D;
import ui.editor.sintaxe.FabricaDeSintaxe;
import ui.editor.sintaxe.SintaxeJson;
import ui.util.CompiladorNative;
import ui.util.GerenciadorDeArquivos;
import modelo.Projeto;
import modelo.GerenciadorProjetoProperties;

public class MontadorDeEditorComAbas {

    public interface AcoesEditor {
    void aoFecharEditor();

    /** ★ Usuário pediu pra abrir as configs do projeto. */
    default void aoAbrirConfigs(java.io.File pastaProjeto) { }
}

    private final Context contexto;
    private final AcoesEditor acoesEditor;
    private final List<AbaArquivo> abasAbertas;
    private AbaArquivo abaAtiva;

    // Views do XML
    private LinearLayout conteinerAbas;
    private LinearLayout conteinerSimbolos;
    private EditText campoEditorCodigo;
    private ScrollView2D editorScroll2D;
    private TextView consoleDeSaida;
    private TextView textStatusAba;
    private GutterView editorGutter;

    // Console colapsável
    private LinearLayout consoleContainer;
    private TextView consoleToggle;
    private ScrollView consoleScroll;
    private boolean consoleExpandido = false;
    private int alturaConsoleRecolhido;
    private int alturaConsoleExpandido;

    // Teclado
    private View raizEditor;
    private View scrollSimbolos;
    private ViewTreeObserver.OnGlobalLayoutListener listenerTeclado;
    private boolean tecladoAbertoAnterior = false;
    private int alturaTecladoAtual = 0;

    private boolean estaFormatando = false;
    private boolean ignorarTrocaTexto = false;

    private final Handler handlerSintaxe = new Handler(Looper.getMainLooper());
    private Runnable runnableSintaxe;

    private static final String[] SIMBOLOS = {
            "{", "}", "(", ")", ";", "#", "<", ">",
            "\"", "'", "=", "+", "-", "*", "&", "|", "!"
    };

    public MontadorDeEditorComAbas(Context contexto, AcoesEditor acoesEditor) {
        this.contexto = contexto;
        this.acoesEditor = acoesEditor;
        this.abasAbertas = new ArrayList<>();
        FabricaDeSintaxe.inicializar(contexto);
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construirLayout() {
    raizEditor = LayoutInflater.from(contexto)
            .inflate(R.layout.tela_editor, null, false);

    conteinerAbas     = raizEditor.findViewById(R.id.editorConteinerAbas);
    conteinerSimbolos = raizEditor.findViewById(R.id.editorConteinerSimbolos);
    campoEditorCodigo = raizEditor.findViewById(R.id.editorCampoCodigo);
    editorScroll2D    = raizEditor.findViewById(R.id.editorScroll2D);
    consoleDeSaida    = raizEditor.findViewById(R.id.editorConsole);
    textStatusAba     = raizEditor.findViewById(R.id.editorStatusAba);
    editorGutter      = raizEditor.findViewById(R.id.editorGutter);

    consoleContainer  = raizEditor.findViewById(R.id.editorConsoleContainer);
    consoleToggle     = raizEditor.findViewById(R.id.editorConsoleToggle);
    consoleScroll     = raizEditor.findViewById(R.id.editorConsoleScroll);

    editorGutter.setEditor(campoEditorCodigo);
    editorGutter.setScrollView(editorScroll2D);

    raizEditor.findViewById(R.id.editorBtnVoltar).setOnClickListener(v -> {
        salvarArquivoAtual();
        if (acoesEditor != null) acoesEditor.aoFecharEditor();
    });
    raizEditor.findViewById(R.id.editorBtnSalvar).setOnClickListener(v -> salvarArquivoAtual());
    raizEditor.findViewById(R.id.editorBtnCompilar).setOnClickListener(v -> executarCompilacaoAtual());

    // ★ Botão de Configs do Projeto
    raizEditor.findViewById(R.id.editorBtnConfigs).setOnClickListener(v -> abrirConfigProjeto());

    configurarConsoleColapsavel();
    configurarMonitorDeTexto();
    popularSimbolos();
    configurarListenerTeclado();

    return raizEditor;
}

    // ==========================================================
    //  Console colapsável
    // ==========================================================

    private void configurarConsoleColapsavel() {
        if (consoleContainer == null) return;

        alturaConsoleRecolhido = dp(100);

        int alturaTela = contexto.getResources().getDisplayMetrics().heightPixels;
        alturaConsoleExpandido = (int) (alturaTela * 0.45f);

        // Aplica estado inicial (recolhido)
        aplicarAlturaConsole(false);

        // Toggle no cabeçalho
        View header = raizEditor.findViewById(R.id.editorConsoleHeader);
        if (header != null) {
            header.setOnClickListener(v -> alternarConsole());
        }
    }
    
    // ==========================================================
//  Configurações do projeto (Projeto.properties visual)
// ==========================================================

private void abrirConfigProjeto() {
    if (abaAtiva == null) {
        Toast.makeText(contexto, "Abra um arquivo do projeto primeiro.",
                Toast.LENGTH_SHORT).show();
        return;
    }

    File pastaProjeto = encontrarRaizProjeto(abaAtiva.getArquivo());
    if (pastaProjeto == null) {
        Toast.makeText(contexto,
                "Este arquivo não pertence a nenhum projeto.",
                Toast.LENGTH_SHORT).show();
        return;
    }

    // ★ Delega pra quem chamou (MainActivity) navegar
    if (acoesEditor != null) {
        acoesEditor.aoAbrirConfigs(pastaProjeto);
    }
}

    private void alternarConsole() {
        consoleExpandido = !consoleExpandido;
        aplicarAlturaConsole(consoleExpandido);
    }

    private void aplicarAlturaConsole(boolean expandido) {
        if (consoleContainer == null) return;

        int altura = expandido ? alturaConsoleExpandido : alturaConsoleRecolhido;

        ViewGroup.LayoutParams lp = consoleContainer.getLayoutParams();
        if (lp != null) {
            lp.height = altura;
            consoleContainer.setLayoutParams(lp);
        }

        if (consoleToggle != null) {
            consoleToggle.setText(expandido ? "▼" : "▲");
        }

        // Ao expandir, rola pro final do console
        if (expandido && consoleScroll != null) {
            consoleScroll.post(() -> consoleScroll.fullScroll(View.FOCUS_DOWN));
        }
    }

    // ==========================================================
    //  TECLADO
    // ==========================================================

    private void configurarListenerTeclado() {
        if (raizEditor == null) return;

        if (conteinerSimbolos != null
                && conteinerSimbolos.getParent() instanceof View) {
            scrollSimbolos = (View) conteinerSimbolos.getParent();
        }

        listenerTeclado = () -> {
            if (raizEditor == null) return;

            Rect areaVisivel = new Rect();
            raizEditor.getWindowVisibleDisplayFrame(areaVisivel);

            int alturaTela = raizEditor.getRootView().getHeight();
            int alturaTeclado = alturaTela - areaVisivel.bottom;

            boolean tecladoAberto = alturaTeclado > alturaTela * 0.15f;
            int alturaFinal = tecladoAberto ? Math.max(0, alturaTeclado) : 0;

            if (tecladoAberto == tecladoAbertoAnterior
                    && alturaFinal == alturaTecladoAtual) {
                return;
            }

            tecladoAbertoAnterior = tecladoAberto;
            alturaTecladoAtual = alturaFinal;

            if (tecladoAberto) {
                if (scrollSimbolos != null) scrollSimbolos.setVisibility(View.GONE);
                if (consoleContainer != null) consoleContainer.setVisibility(View.GONE);

                raizEditor.setPadding(0, 0, 0, alturaFinal);
                rolarCursorParaVisivel();

            } else {
                if (scrollSimbolos != null) scrollSimbolos.setVisibility(View.VISIBLE);
                if (consoleContainer != null) consoleContainer.setVisibility(View.VISIBLE);
                raizEditor.setPadding(0, 0, 0, 0);
            }
        };

        raizEditor.getViewTreeObserver()
                .addOnGlobalLayoutListener(listenerTeclado);
    }

    private void rolarCursorParaVisivel() {
        if (campoEditorCodigo == null || editorScroll2D == null) return;

        campoEditorCodigo.postDelayed(() -> {
            if (campoEditorCodigo == null || editorScroll2D == null) return;

            android.text.Layout layout = campoEditorCodigo.getLayout();
            if (layout == null) return;

            int cursor = Math.max(campoEditorCodigo.getSelectionStart(), 0);
            int linhaCursor = layout.getLineForOffset(cursor);

            int yTopoEditor    = layout.getLineTop(linhaCursor);
            int yBaseEditor    = layout.getLineBottom(linhaCursor);
            int alturaViewport = editorScroll2D.getHeight();
            int scrollY        = editorScroll2D.getScrollY();

            int paddingExtra = dp(80);

            if (yBaseEditor + paddingExtra > scrollY + alturaViewport) {
                int novoScrollY = yBaseEditor + paddingExtra - alturaViewport;
                editorScroll2D.scrollTo(editorScroll2D.getScrollX(), novoScrollY);
            } else if (yTopoEditor - paddingExtra < scrollY) {
                int novoScrollY = Math.max(0, yTopoEditor - paddingExtra);
                editorScroll2D.scrollTo(editorScroll2D.getScrollX(), novoScrollY);
            }
        }, 200);
    }

    // ==========================================================
    //  Símbolos rápidos
    // ==========================================================

    private void popularSimbolos() {
        conteinerSimbolos.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(contexto);

        for (String simbolo : SIMBOLOS) {
            TextView btn = (TextView) inflater.inflate(
                    R.layout.item_simbolo, conteinerSimbolos, false);
            btn.setText(simbolo);

            btn.setOnClickListener(v -> {
                if (campoEditorCodigo == null) return;
                int start = Math.max(campoEditorCodigo.getSelectionStart(), 0);
                int end = Math.max(campoEditorCodigo.getSelectionEnd(), 0);
                campoEditorCodigo.getText().replace(
                        Math.min(start, end), Math.max(start, end),
                        simbolo, 0, simbolo.length());
            });

            conteinerSimbolos.addView(btn);
        }
    }

    // ==========================================================
    //  Abas
    // ==========================================================

    private void atualizarVisualizacaoAbas() {
        conteinerAbas.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(contexto);

        for (AbaArquivo aba : abasAbertas) {
            boolean ehAtivo = aba.equals(abaAtiva);

            View itemAba = inflater.inflate(R.layout.item_aba, conteinerAbas, false);

            itemAba.setBackgroundResource(ehAtivo
                    ? R.drawable.bg_aba_editor_ativa
                    : R.drawable.bg_aba_editor);

            TextView titulo = itemAba.findViewById(R.id.abaTitulo);
            TextView fechar = itemAba.findViewById(R.id.abaBtnFechar);

            titulo.setText((aba.isModificado() ? "● " : "") + aba.getArquivo().getName());
            titulo.setTextColor(ehAtivo
                    ? Color.parseColor("#F4F4F5")
                    : Color.parseColor("#888888"));
            titulo.setTypeface(Typeface.MONOSPACE,
                    ehAtivo ? Typeface.BOLD : Typeface.NORMAL);

            itemAba.setOnClickListener(v -> selecionarAba(aba));
            fechar.setOnClickListener(v -> fecharAba(aba));

            conteinerAbas.addView(itemAba);
        }
    }

    private void fecharAba(AbaArquivo aba) {
        if (aba.isModificado()) {
            GerenciadorDeArquivos.salvarArquivo(aba.getArquivo(), aba.getConteudoEmMemoria());
        }

        abasAbertas.remove(aba);

        if (abaAtiva == aba) {
            if (!abasAbertas.isEmpty()) {
                selecionarAba(abasAbertas.get(abasAbertas.size() - 1));
            } else {
                abaAtiva = null;
                ignorarTrocaTexto = true;
                campoEditorCodigo.setText("");
                if (editorScroll2D != null) editorScroll2D.scrollTo(0, 0);
                ignorarTrocaTexto = false;
                textStatusAba.setText("Nenhum arquivo aberto");

                if (editorGutter != null) {
                    editorGutter.requestLayout();
                    editorGutter.invalidate();
                }
            }
        }
        atualizarVisualizacaoAbas();
    }

    // ==========================================================
    //  Abrir / selecionar arquivo
    // ==========================================================

    public void abrirArquivo(File arquivo) {
        if (arquivo == null || !arquivo.exists()) return;

        AbaArquivo abaExistente = null;
        for (AbaArquivo aba : abasAbertas) {
            if (aba.getArquivo().getAbsolutePath().equals(arquivo.getAbsolutePath())) {
                abaExistente = aba;
                break;
            }
        }

        if (abaExistente == null) {
            String conteudo = GerenciadorDeArquivos.lerArquivo(arquivo);
            abaExistente = new AbaArquivo(arquivo, conteudo);
            abasAbertas.add(abaExistente);
        }

        selecionarAba(abaExistente);
    }

    private void selecionarAba(AbaArquivo aba) {
        this.abaAtiva = aba;

        ignorarTrocaTexto = true;
        campoEditorCodigo.setText(aba.getConteudoEmMemoria());
        ignorarTrocaTexto = false;

        campoEditorCodigo.post(() -> {
            if (editorScroll2D != null) {
                editorScroll2D.scrollTo(0, 0);
            }
            if (campoEditorCodigo != null) {
                campoEditorCodigo.invalidate();
            }
            if (editorGutter != null) {
                editorGutter.requestLayout();
                editorGutter.invalidate();
            }
        });

        if (campoEditorCodigo.getText() != null) {
            estaFormatando = true;
            aplicarSintaxeAtiva(campoEditorCodigo.getText());
            estaFormatando = false;
        }

        textStatusAba.setText((aba.isModificado() ? "* " : "") + aba.getArquivo().getName());
        atualizarVisualizacaoAbas();
    }

    // ==========================================================
    //  Salvar / Compilar
    // ==========================================================

    public void salvarArquivoAtual() {
        if (abaAtiva == null) return;

        boolean sucesso = GerenciadorDeArquivos.salvarArquivo(
                abaAtiva.getArquivo(), abaAtiva.getConteudoEmMemoria());

        if (sucesso) {
            abaAtiva.setModificado(false);
            textStatusAba.setText(abaAtiva.getArquivo().getName());
            atualizarVisualizacaoAbas();
            Toast.makeText(contexto, "Arquivo salvo com sucesso!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(contexto, "Erro ao salvar o arquivo.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ==========================================================
    //  COMPILAÇÃO
    // ==========================================================

    public void executarCompilacaoAtual() {
        if (abaAtiva == null) {
            Toast.makeText(contexto, "Nenhum arquivo aberto para compilar.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        salvarArquivoAtual();

        File pastaProjeto = encontrarRaizProjeto(abaAtiva.getArquivo());

        if (pastaProjeto == null) {
            compilarArquivoAvulso();
            return;
        }

        // ★ Expande o console automaticamente ao compilar
        if (!consoleExpandido) {
            consoleExpandido = true;
            aplicarAlturaConsole(true);
        }

        Projeto projeto = GerenciadorProjetoProperties.carregar(pastaProjeto);

        if (projeto.abis == null || projeto.abis.isEmpty()) {
            projeto.abis = new ArrayList<>();
            projeto.abis.add("arm64-v8a");
        }

        final int totalAbis = projeto.abis.size();

        consoleDeSaida.setTextColor(Color.parseColor("#29B6F6"));
        consoleDeSaida.setText(
                "── COMPILAÇÃO DO PROJETO ──\n" +
                "Projeto:  " + projeto.nome + " v" + projeto.versao + "\n" +
                "ABIs:     " + String.join(", ", projeto.abis) + "\n" +
                "API:      " + projeto.api + "\n" +
                "Saída:    " + projeto.libNome + ".so\n" +
                "Std:      " + projeto.std + "\n" +
                "Flags:    " + (projeto.flags.isEmpty() ? "—" : String.join(" ", projeto.flags)) + "\n\n" +
                "Compilando " + totalAbis + " ABI(s)...\n\n");

        final long inicio = System.currentTimeMillis();

        CompiladorNative.compilarProjeto(contexto, projeto,
                new CompiladorNative.ResultadoCompilacao() {

                    @Override
                    public void aoSucesso(File arquivoSo) {
                        String abi = arquivoSo.getParentFile() != null
                                ? arquivoSo.getParentFile().getName()
                                : "?";
                        final String linha = "  ✓ [" + abi + "] "
                                + arquivoSo.getName()
                                + "  (" + arquivoSo.length() + " bytes)\n";
                        new Handler(Looper.getMainLooper()).post(() ->
                                consoleDeSaida.append(linha));
                    }

                    @Override
                    public void aoErro(String mensagemErro) {
                        // resumo vem no aoFinalizar
                    }

                    @Override
                    public void aoFinalizar(int sucessos, int totalAbis, String resumo) {
                        long duracao = System.currentTimeMillis() - inicio;

                        int cor;
                        if (sucessos == totalAbis) {
                            cor = Color.parseColor("#00E676");
                        } else if (sucessos == 0) {
                            cor = Color.parseColor("#FF5252");
                        } else {
                            cor = Color.parseColor("#FFC107");
                        }

                        String bloco =
                                "\n── RESULTADO ──\n" +
                                "Projeto:  " + sucessos + "/" + totalAbis
                                        + " ABI(s) compilada(s)\n" +
                                "Duração:  " + formatarDuracao(duracao) + "\n\n" +
                                (resumo == null || resumo.isEmpty()
                                        ? "(sem detalhes)"
                                        : resumo);

                        final int corFinal = cor;
                        new Handler(Looper.getMainLooper()).post(() -> {
                            consoleDeSaida.setTextColor(corFinal);
                            consoleDeSaida.append(bloco);
                        });
                    }
                });
    }

    private File encontrarRaizProjeto(File arquivo) {
        File pasta = arquivo.getParentFile();
        while (pasta != null) {
            File props = new File(pasta, GerenciadorProjetoProperties.NOME_ARQUIVO);
            if (props.isFile()) return pasta;
            pasta = pasta.getParentFile();
        }
        return null;
    }

    private void compilarArquivoAvulso() {
        File arquivoFonte = abaAtiva.getArquivo();
        File pastaOut = new File(contexto.getFilesDir(), "out");
        pastaOut.mkdirs();

        String nomeBase = arquivoFonte.getName().replaceAll("\\.[^.]+$", "");
        File arquivoSaida = new File(pastaOut, "lib" + nomeBase + ".so");

        if (!consoleExpandido) {
            consoleExpandido = true;
            aplicarAlturaConsole(true);
        }

        consoleDeSaida.setTextColor(Color.parseColor("#FFC107"));
        consoleDeSaida.setText(
                "⚠ Projeto.properties não encontrado.\n" +
                "Compilando apenas: " + arquivoFonte.getName() + "\n\n");

        CompiladorNative.compilarParaSo(contexto, arquivoFonte, arquivoSaida,
                new CompiladorNative.ResultadoCompilacao() {
                    @Override
                    public void aoSucesso(File arquivoSo) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            consoleDeSaida.setTextColor(Color.parseColor("#00E676"));
                            consoleDeSaida.setText("✓ Compilado: " + arquivoSo.getName() + "\n");
                        });
                    }

                    @Override
                    public void aoErro(String mensagemErro) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            consoleDeSaida.setTextColor(Color.parseColor("#FF5252"));
                            consoleDeSaida.setText("✗ Falha:\n\n" + mensagemErro);
                        });
                    }
                });
    }

    private String formatarDuracao(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format("%.1fs", ms / 1000.0);
        return (ms / 60_000) + "m " + ((ms % 60_000) / 1000) + "s";
    }

    // ==========================================================
    //  Monitor de texto + sintaxe
    // ==========================================================

    private void configurarMonitorDeTexto() {
        campoEditorCodigo.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable editable) {
                if (ignorarTrocaTexto || estaFormatando || abaAtiva == null) return;

                abaAtiva.setConteudoEmMemoria(editable.toString());
                if (!abaAtiva.isModificado()) {
                    abaAtiva.setModificado(true);
                    atualizarVisualizacaoAbas();
                }

                if (editorGutter != null) {
                    editorGutter.post(() -> {
                        editorGutter.requestLayout();
                        editorGutter.invalidate();
                    });
                }

                campoEditorCodigo.invalidate();

                if (runnableSintaxe != null) {
                    handlerSintaxe.removeCallbacks(runnableSintaxe);
                }

                runnableSintaxe = () -> {
                    estaFormatando = true;
                    aplicarSintaxeAtiva(editable);
                    estaFormatando = false;
                    campoEditorCodigo.invalidate();
                    if (editorGutter != null) editorGutter.invalidate();
                };

                handlerSintaxe.postDelayed(runnableSintaxe, 300);
            }
        });
    }

    private void aplicarSintaxeAtiva(Editable editable) {
        if (abaAtiva == null) return;

        ForegroundColorSpan[] spansAntigos = editable.getSpans(
                0, editable.length(), ForegroundColorSpan.class);
        for (ForegroundColorSpan span : spansAntigos) {
            editable.removeSpan(span);
        }

        SintaxeJson sintaxe = FabricaDeSintaxe.obterSintaxePorArquivo(abaAtiva.getArquivo());
        if (sintaxe != null) {
            sintaxe.aplicarSintaxe(editable);
        }
    }

    public TextView obterConsoleDeSaida() { return consoleDeSaida; }

    private int dp(int v) {
        return (int) (v * contexto.getResources().getDisplayMetrics().density);
    }
}