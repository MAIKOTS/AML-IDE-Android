package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import app.R;
import ui.componentes.ConsoleTerminal;
import ui.dialogos.DialogoApp;
import app.util.CompiladorNativo;
import app.util.GerenciadorDeArquivos;
import ui.util.GerenciadorLogsConsole;
import app.util.GerenciadorNDK;

/**
 * Ferramentas da IDE.
 *
 * Central de ferramentas e diagnóstico:
 *  - Status da toolchain
 *  - Verificar / baixar recursos
 *  - Compilar main.cpp de teste
 *  - Abrir o explorador de arquivos
 *  - Ver logs em um ConsoleTerminal ao vivo
 */
public class FerramentasDaIDE {

    public interface Acoes {
        void aoVoltar();
        void aoAbrirExploradorFiles();
        void aoAbrirRecursos();
    }

    private static final int COR_OK     = 0xFF00E676;
    private static final int COR_AVISO  = 0xFFFFC107;
    private static final int COR_ERRO   = 0xFFFF5252;
    private static final int COR_NEUTRO = 0xFFA1A1AA;

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private View raizView;
    private ConsoleTerminal console;
    private TextView statusTopo;
    private TextView statusDetalhe;
    private ImageView statusIcone;
    private LinearLayout btnPreparar;
    private LinearLayout btnCompilar;
    private LinearLayout btnInspecionar;

    private final AtomicBoolean operacaoEmAndamento = new AtomicBoolean(false);

    public FerramentasDaIDE(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção da tela
    // ==========================================================

    public View construir() {
    raizView = LayoutInflater.from(ctx)
            .inflate(R.layout.tela_ferramentas_ide, null, false);

    // Bind
    console        = raizView.findViewById(R.id.ferramentasConsole);
    statusTopo     = raizView.findViewById(R.id.ferramentasStatus);
    statusDetalhe  = raizView.findViewById(R.id.ferramentasStatusDetalhe);
    statusIcone    = raizView.findViewById(R.id.ferramentasStatusIcone);
    btnPreparar    = raizView.findViewById(R.id.btnFerramentasPreparar);
    btnCompilar    = raizView.findViewById(R.id.btnFerramentasCompilar);
    btnInspecionar = raizView.findViewById(R.id.btnFerramentasInspecionar);

    raizView.findViewById(R.id.btnFerramentasRecursos)
            .setOnClickListener(v -> {
                if (acoes != null) acoes.aoAbrirRecursos();
            });

    // Voltar
    raizView.findViewById(R.id.btnFerramentasVoltar).setOnClickListener(v -> {
        if (operacaoEmAndamento.get()) {
            log("! Operação em andamento. Aguarde o fim.");
            return;
        }
        if (acoes != null) acoes.aoVoltar();
    });

    // Ações
    btnPreparar.setOnClickListener(v -> verificarToolchain());
    btnCompilar.setOnClickListener(v -> iniciarCompilacao());
    btnInspecionar.setOnClickListener(v -> {
        if (acoes != null) acoes.aoAbrirExploradorFiles();
    });

    // Estado inicial
    atualizarStatusTopo();
    reabilitarBotoes();

    log("Ferramentas da IDE iniciadas.");
    log("ABI: " + GerenciadorNDK.obterAbi(ctx));
    log("Status: " + (GerenciadorNDK.estaPronta(ctx)
            ? "toolchain pronta" : "toolchain não instalada"));

    return raizView;
}

    // ==========================================================
    //  Verificar toolchain
    // ==========================================================

    private void verificarToolchain() {
        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();
        atualizarStatusTopo();

        console.secao("VERIFICAÇÃO DA TOOLCHAIN");

        File sysroot = GerenciadorNDK.obterSysroot(ctx);
        File libs    = GerenciadorNDK.obterLibsExtras(ctx);

        boolean temSysroot = (sysroot != null && sysroot.isDirectory());
        boolean temLibs    = (libs    != null && libs.isDirectory());

        if (temSysroot) {
            log("✓ Sysroot:  " + sysroot.getAbsolutePath());
        } else {
            log("✗ Sysroot não instalado");
        }

        if (temLibs) {
            log("✓ Libs:     " + libs.getAbsolutePath());
        } else {
            log("✗ Libs extras não instaladas");
        }

        log("");
        log("ABI: " + GerenciadorNDK.obterAbi(ctx));

        operacaoEmAndamento.set(false);
        reabilitarBotoes();
        atualizarStatusTopo();

        if (temSysroot && temLibs) {
            console.sucesso("Toolchain completa e pronta para compilar.");
            return;
        }

        console.erro("Toolchain incompleta.");
        log("");
        log("Baixe os recursos em:");
        log("   Menu → Ferramentas da IDE → Recursos");

        DialogoApp.confirmar(ctx,
                "Baixar recursos?",
                "A toolchain não está completa.\n\n" +
                "Deseja abrir a tela de Recursos agora para baixar?",
                "Abrir Recursos", "Cancelar", false,
                () -> {
                    if (acoes != null) acoes.aoAbrirRecursos();
                });
    }

    // ==========================================================
    //  Compilação de teste
    // ==========================================================

    private void iniciarCompilacao() {
        File pastaTeste = new File(ctx.getFilesDir(), "teste_build");
        File pastaSrc   = new File(pastaTeste, "src");
        File pastaOut   = new File(pastaTeste, "out");
        pastaSrc.mkdirs();
        pastaOut.mkdirs();

        File fonte = new File(pastaSrc, "main.cpp");
        String codigo =
                "// Teste de compilação da AML IDE\n" +
                "#include <cstdio>\n\n" +
                "extern \"C\" void AMLMain() {\n" +
                "    printf(\"Ola do AML IDE!\\n\");\n" +
                "}\n";

        if (!GerenciadorDeArquivos.salvarArquivo(fonte, codigo)) {
            log("✗ Não foi possível criar main.cpp de teste.");
            return;
        }

        File saida = new File(pastaOut, "libmain.so");

        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();
        atualizarStatusTopo();

        GerenciadorLogsConsole logDetalhado = new GerenciadorLogsConsole(ctx, console);

        CompiladorNativo.compilarParaSo(ctx, fonte, saida,
                new CompiladorNativo.ResultadoCompilacao() {
                    @Override
                    public void aoSucesso(File arquivoSo) {
                        handlerUI.post(() -> {
                            operacaoEmAndamento.set(false);
                            reabilitarBotoes();
                            atualizarStatusTopo();
                        });
                    }

                    @Override
                    public void aoErro(String mensagemErro) {
                        handlerUI.post(() -> {
                            operacaoEmAndamento.set(false);
                            reabilitarBotoes();
                            atualizarStatusTopo();
                        });
                    }
                },
                logDetalhado);
    }

    // ==========================================================
    //  Status e botões
    // ==========================================================

    private void travarBotoesDuranteOperacao() {
        setCardHabilitado(btnPreparar, false);
        setCardHabilitado(btnCompilar, false);
        setCardHabilitado(btnInspecionar, false);
    }

    private void reabilitarBotoes() {
        boolean pronta = GerenciadorNDK.estaPronta(ctx);

        // Verificar: só se NÃO está pronta
        setCardHabilitado(btnPreparar, !pronta);

        // Compilar teste: só se está pronta
        setCardHabilitado(btnCompilar, pronta);

        // Inspecionar: sempre
        setCardHabilitado(btnInspecionar, true);
    }

    /** Aplica o estado visual (alpha + clickable) de um card de ação. */
    private void setCardHabilitado(View card, boolean habilitado) {
        if (card == null) return;
        card.setEnabled(habilitado);
        card.setClickable(habilitado);
        card.setAlpha(habilitado ? 1.0f : 0.4f);
    }

    private void atualizarStatusTopo() {
        if (operacaoEmAndamento.get()) {
            aplicarStatus("Operação em andamento...",
                    "Aguarde a conclusão", COR_AVISO);
            return;
        }

        if (GerenciadorNDK.estaPronta(ctx)) {
            aplicarStatus("Toolchain pronta",
                    "Clang 14 • NDK r24 • ABI " + GerenciadorNDK.obterAbi(ctx),
                    COR_OK);
        } else {
            aplicarStatus("Toolchain não instalada",
                    "Baixe os recursos em Ferramentas da IDE → Recursos",
                    COR_AVISO);
        }
    }

    /** Aplica status com ícone, texto e cor. */
    private void aplicarStatus(String titulo, String detalhe, int cor) {
        if (statusTopo != null) statusTopo.setText(titulo);
        if (statusDetalhe != null) statusDetalhe.setText(detalhe);
        if (statusIcone != null) {
            statusIcone.setColorFilter(cor);
            if (cor == COR_OK) {
                statusIcone.setImageResource(R.drawable.ic_pasta_aberta);
            } else {
                statusIcone.setImageResource(R.drawable.ic_config);
            }
        }
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private void log(String linha) {
        if (console == null) return;
        console.info(linha);
    }

    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    private String formatarDuracao(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60_000) return String.format("%.1fs", ms / 1000.0);
        long min = ms / 60_000;
        long seg = (ms % 60_000) / 1000;
        return min + "m " + seg + "s";
    }
}