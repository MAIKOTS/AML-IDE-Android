package ui.telas;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import app.R;
import ui.componentes.ConsoleTerminal;
import ui.util.CompiladorNative;
import ui.util.GerenciadorDeArquivos;
import ui.util.GerenciadorNDK;

/**
 * Tela de Complementos da IDE.
 *
 * Layout: res/layout/tela_complementos.xml
 *
 * Responsabilidades:
 *  - Mostrar status da toolchain
 *  - Extrair o sysroot (preparar)
 *  - Compilar main.cpp de teste
 *  - Abrir o explorador de arquivos
 *  - Mostrar tudo em um ConsoleTerminal ao vivo
 */
public class TelaComplementos {

    public interface Acoes {
        void aoVoltar();
        void aoAbrirExploradorFiles();
    }

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private View raizView;
    private ConsoleTerminal console;
    private TextView statusTopo;
    private Button btnPreparar;
    private Button btnCompilar;
    private Button btnInspecionar;

    private final AtomicBoolean operacaoEmAndamento = new AtomicBoolean(false);

    public TelaComplementos(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção da tela
    // ==========================================================

    public View construir() {
        raizView = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_complementos, null, false);

        // Bind
        console         = raizView.findViewById(R.id.complementosConsole);
        statusTopo      = raizView.findViewById(R.id.complementosStatus);
        btnPreparar     = raizView.findViewById(R.id.btnComplementosPreparar);
        btnCompilar     = raizView.findViewById(R.id.btnComplementosCompilar);
        btnInspecionar  = raizView.findViewById(R.id.btnComplementosInspecionar);

        // Voltar
        raizView.findViewById(R.id.btnComplementosVoltar).setOnClickListener(v -> {
            if (operacaoEmAndamento.get()) {
                log("! Operação em andamento. Aguarde ou aguarde o fim.");
                return;
            }
            if (acoes != null) acoes.aoVoltar();
        });

        // Preparar toolchain
        btnPreparar.setOnClickListener(v -> prepararToolchain());

        // Compilar teste
        btnCompilar.setOnClickListener(v -> iniciarCompilacao());

        // Inspecionar files/
        btnInspecionar.setOnClickListener(v -> {
            if (acoes != null) acoes.aoAbrirExploradorFiles();
        });

        // Estado inicial
        atualizarStatusTopo();
        reabilitarBotoes();

        log("Sistema de complementos iniciado.");
        log("ABI: " + GerenciadorNDK.obterAbi(ctx));
        log("Status: " + (GerenciadorNDK.estaPronta(ctx)
                ? "toolchain pronta" : "toolchain não preparada"));

        return raizView;
    }

    // ==========================================================
    //  Preparar toolchain
    // ==========================================================

    private void prepararToolchain() {
        operacaoEmAndamento.set(true);
        travarBotoesDuranteOperacao();
        atualizarStatusTopo();

        final long inicio = System.currentTimeMillis();

        console.secao("PREPARAÇÃO DA TOOLCHAIN");
        log("Extraindo sysroot para: " + new File(ctx.getFilesDir(), "sysroot").getAbsolutePath());
        log("Isso pode demorar ~30 segundos na primeira vez.");
        log("");

        new Thread(() -> {
            final File sysroot = GerenciadorNDK.obterSysroot(ctx);
            final long duracao = System.currentTimeMillis() - inicio;

            handlerUI.post(() -> {
                operacaoEmAndamento.set(false);

                if (sysroot != null && sysroot.isDirectory()) {
                    console.sucesso("Toolchain pronta em " + formatarDuracao(duracao));
                    log("");
                    log("Sysroot: " + sysroot.getAbsolutePath());
                    log("");
                    console.secao("ESTRUTURA DO SYSROOT");
                    log("  sysroot/");
                    console.arvore(sysroot, "  ", 2);
                    log("");
                    log("  (mostrando 2 níveis de profundidade)");
                } else {
                    console.erro("Falha após " + formatarDuracao(duracao));
                    log("Não foi possível extrair o sysroot.");
                }

                reabilitarBotoes();
                atualizarStatusTopo();
            });
        }, "preparar-toolchain").start();
    }

    // ==========================================================
    //  Compilação de teste
    // ==========================================================

    private void iniciarCompilacao() {
        File pastaTeste = new File(ctx.getFilesDir(), "teste_build");
        File pastaSrc = new File(pastaTeste, "src");
        File pastaOut = new File(pastaTeste, "out");
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

        final long inicio = System.currentTimeMillis();

        console.secao("COMPILAÇÃO DE TESTE");
        log("Fonte:  " + fonte.getName());
        log("Alvo:   " + GerenciadorNDK.obterAbi(ctx));
        log("");

        CompiladorNative.compilarParaSo(ctx, fonte, saida,
                new CompiladorNative.ResultadoCompilacao() {
            @Override
            public void aoSucesso(File arquivoSo) {
                long duracao = System.currentTimeMillis() - inicio;
                handlerUI.post(() -> {
                    operacaoEmAndamento.set(false);
                    console.sucesso("Compilado em " + formatarDuracao(duracao));
                    log("  → " + arquivoSo.getAbsolutePath());
                    log("  → " + formatarTamanho(arquivoSo.length()));
                    reabilitarBotoes();
                    atualizarStatusTopo();
                });
            }

            @Override
            public void aoErro(String mensagemErro) {
                long duracao = System.currentTimeMillis() - inicio;
                handlerUI.post(() -> {
                    operacaoEmAndamento.set(false);
                    console.erro("Falha após " + formatarDuracao(duracao));
                    log("");
                    log("--- saída do compilador ---");
                    for (String linha : mensagemErro.split("\n")) {
                        log(linha);
                    }
                    log("---------------------------");
                    reabilitarBotoes();
                    atualizarStatusTopo();
                });
            }
        });
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private void travarBotoesDuranteOperacao() {
        btnPreparar.setEnabled(false);
        btnCompilar.setEnabled(false);
        if (btnInspecionar != null) btnInspecionar.setEnabled(false);
    }

    private void reabilitarBotoes() {
        boolean pronta = GerenciadorNDK.estaPronta(ctx);
        btnPreparar.setEnabled(!pronta);
        btnCompilar.setEnabled(pronta);
        if (btnInspecionar != null) btnInspecionar.setEnabled(true);
    }

    private void atualizarStatusTopo() {
        if (operacaoEmAndamento.get()) {
            statusTopo.setText("⏳ Operação em andamento...");
        } else if (GerenciadorNDK.estaPronta(ctx)) {
            statusTopo.setText("✓ Toolchain pronta para compilar");
        } else {
            statusTopo.setText("⚠ Toolchain não preparada");
        }
    }

    private void log(String linha) {
        if (console == null) return;
        console.info(linha);
    }

    private String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
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