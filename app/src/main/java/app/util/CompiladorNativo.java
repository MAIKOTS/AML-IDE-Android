package app.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.util.compilacao.AmbienteCompilacao;
import app.util.compilacao.ConstrutorComando;
import app.util.compilacao.ExecutorClang;
import modelo.Projeto;
import ui.util.GerenciadorLogsConsole;

/**
 * Orquestrador de compilação.
 *
 * Delega as etapas para:
 *  - AmbienteCompilacao → validar clang/sysroot/libs
 *  - ConstrutorComando  → montar a linha de comando
 *  - ExecutorClang      → executar o processo
 *
 * Aqui só fica o fluxo (loop de ABIs, callbacks, resumo).
 */
public final class CompiladorNativo {

    private static final String TAG = "CompiladorNativo";

    private CompiladorNativo() { }

    public interface ResultadoCompilacao {
        void aoSucesso(File arquivoSo);
        void aoErro(String mensagemErro);
        default void aoFinalizar(int sucessos, int totalAbis, String resumo) { }
    }

    // ==========================================================
    //  API principal — projeto multi-ABI
    // ==========================================================

    public static void compilarProjeto(Context ctx,
                                       Projeto projeto,
                                       ResultadoCompilacao callback) {
        compilarProjeto(ctx, projeto, callback, null);
    }

    public static void compilarProjeto(Context ctx,
                                       Projeto projeto,
                                       ResultadoCompilacao callback,
                                       GerenciadorLogsConsole log) {

        new Thread(() -> {

            int           sucessos    = 0;
            int           totalAbis   = 0;
            StringBuilder logGlobal   = new StringBuilder();
            boolean       erroFatal   = false;
            long          inicioTotal = System.currentTimeMillis();

            try {
                // ---------- Cabeçalho ----------
                logCabecalhoProjeto(projeto, log);

                // ---------- Validação do projeto ----------
                if (projeto == null || projeto.pastaRaiz == null) {
                    if (log != null) log.erro("Valid", "Projeto inválido (pastaRaiz nula)");
                    callback.aoErro("Projeto inválido.");
                    erroFatal = true;
                    return;
                }

                // ---------- Fontes ----------
                List<File> fontes = projeto.resolverFontes();
                logFontes(fontes, log);
                if (fontes.isEmpty()) {
                    if (log != null) log.erro("Fontes",
                            "Nenhum arquivo .cpp/.c encontrado em src/");
                    callback.aoErro("Nenhum arquivo .cpp/.c encontrado em src/");
                    erroFatal = true;
                    return;
                }

                // ---------- ABIs ----------
                if (projeto.abis == null || projeto.abis.isEmpty()) {
                    projeto.abis = new ArrayList<>();
                    projeto.abis.add("arm64-v8a");
                    if (log != null) log.aviso("ABIs",
                            "Nenhuma ABI definida — usando arm64-v8a padrão");
                }
                totalAbis = projeto.abis.size();
                if (log != null) log.info("ABIs", "Compilando " + totalAbis + " ABI(s)");

                // ---------- Ambiente ----------
                AmbienteCompilacao amb = AmbienteCompilacao.preparar(ctx, log);
                if (!amb.ok) {
                    callback.aoErro(amb.erro);
                    erroFatal = true;
                    return;
                }

                // ---------- Loop de ABIs ----------
                for (String abiBruta : projeto.abis) {
                    String abiNorm = Projeto.normalizarAbi(abiBruta);

                    if (log != null) log.secao("ABI: " + abiNorm);

                    AbiResultado r = compilarUmaAbi(projeto, abiNorm, fontes, amb, log);

                    if (r.sucesso && r.arquivoSo != null) {
                        sucessos++;
                        Log.i(TAG, "[OK] " + abiNorm + " → " + r.arquivoSo.getAbsolutePath());
                        logGlobal.append("[OK] ").append(abiNorm).append("\n");
                        callback.aoSucesso(r.arquivoSo);
                    } else {
                        Log.e(TAG, "[FALHA] " + abiNorm + "\n" + r.log);
                        logGlobal.append("[FALHA] ").append(abiNorm).append("\n");
                        if (r.log != null && !r.log.isEmpty()) {
                            logGlobal.append(r.log).append("\n");
                        }
                    }
                }

                // ---------- Resumo ----------
                long totalMs = System.currentTimeMillis() - inicioTotal;
                logResumoFinal(sucessos, totalAbis, totalMs, log);

                if (sucessos == 0) {
                    callback.aoErro("Nenhuma ABI compilou com sucesso.\n\n"
                            + logGlobal.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, "Exceção ao compilar projeto", e);
                if (log != null) log.erro("Compiler", "Exceção: " + e.getMessage());
                callback.aoErro("Exceção: " + e.getMessage());
                erroFatal = true;

            } finally {
                if (!erroFatal && totalAbis > 0) {
                    callback.aoFinalizar(sucessos, totalAbis, logGlobal.toString());
                }
            }

        }, "compilador-projeto").start();
    }

    // ==========================================================
    //  Compila UMA ABI
    // ==========================================================

    private static class AbiResultado {
        boolean sucesso;
        File    arquivoSo;
        String  log = "";
    }

    private static AbiResultado compilarUmaAbi(Projeto projeto,
                                               String abiNorm,
                                               List<File> fontes,
                                               AmbienteCompilacao amb,
                                               GerenciadorLogsConsole log) {

        AbiResultado r = new AbiResultado();
        long inicioAbi = System.currentTimeMillis();

        try {
            File arquivoSaida = projeto.obterArquivoSaida(abiNorm);

            // 1. Constrói o comando
            List<String> cmd = ConstrutorComando.montar(
                    projeto, abiNorm, fontes, amb, log);

            // 2. Executa
            ExecutorClang.Resultado exec = ExecutorClang.executar(
                    cmd, amb.nativeDir, amb.libsExtras, log);

            // 3. Verifica resultado
            if (exec.codigoSaida == 0 && arquivoSaida.isFile()) {
                r.sucesso   = true;
                r.arquivoSo = arquivoSaida;

                if (log != null) {
                    log.ok("Resultado", arquivoSaida.getName()
                            + "  (" + GerenciadorLogsConsole.formatarTamanho(
                                    arquivoSaida.length()) + ")");
                    log.info("Resultado", "Path: " + arquivoSaida.getAbsolutePath());
                    log.sucesso("ABI " + abiNorm + " compilada",
                            System.currentTimeMillis() - inicioAbi);
                }
            } else {
                r.log = exec.saida;
                if (r.log == null || r.log.isEmpty()) {
                    r.log = "(clang não produziu saída)\n";
                }
                r.log += "Exit code: " + exec.codigoSaida + "\n";

                if (log != null) {
                    log.falha("ABI " + abiNorm + " falhou",
                            System.currentTimeMillis() - inicioAbi,
                            "exit code " + exec.codigoSaida);
                }
            }

            return r;

        } catch (Exception e) {
            Log.e(TAG, "Exceção ao compilar ABI " + abiNorm, e);
            r.log = "Exceção: " + e.getMessage();
            if (log != null) {
                log.erro("Exceção", e.getClass().getSimpleName() + ": " + e.getMessage());
                if (e.getCause() != null) {
                    log.erro("Exceção", "  Causa: " + e.getCause());
                }
            }
            return r;
        }
    }

    // ==========================================================
    //  API LEGADA — compila um único arquivo
    // ==========================================================

    public static void compilarParaSo(Context ctx,
                                      File arquivoCodigo,
                                      File arquivoSaidaSo,
                                      ResultadoCompilacao callback) {
        compilarParaSo(ctx, arquivoCodigo, arquivoSaidaSo, callback, null);
    }

    public static void compilarParaSo(Context ctx,
                                      File arquivoCodigo,
                                      File arquivoSaidaSo,
                                      ResultadoCompilacao callback,
                                      GerenciadorLogsConsole log) {

        new Thread(() -> {
            try {
                if (log != null) {
                    log.secao("COMPILAÇÃO AVULSA");
                    log.info("Fonte", arquivoCodigo != null
                            ? arquivoCodigo.getAbsolutePath() : "(nulo)");
                    log.info("Destino", arquivoSaidaSo != null
                            ? arquivoSaidaSo.getAbsolutePath() : "(nulo)");
                }

                if (arquivoCodigo == null || !arquivoCodigo.isFile()) {
                    if (log != null) log.erro("Fonte", "Não encontrada");
                    callback.aoErro("Arquivo fonte não encontrado.");
                    return;
                }

                // ---------- Ambiente ----------
                AmbienteCompilacao amb = AmbienteCompilacao.preparar(ctx, log);
                if (!amb.ok) {
                    callback.aoErro(amb.erro);
                    return;
                }

                // ---------- ABI atual ----------
                String abi = app.util.GerenciadorNDK.obterAbi(ctx);
                String abiNorm = abi.contains("arm64") ? "arm64-v8a"
                              : abi.contains("armeabi") ? "armeabi-v7a"
                              : null;
                if (abiNorm == null) {
                    if (log != null) log.erro("Target",
                            "Arquitetura não suportada: " + abi);
                    callback.aoErro("Arquitetura não suportada: " + abi);
                    return;
                }

                // ---------- Monta cmd ----------
                List<File> fontes = new ArrayList<>();
                fontes.add(arquivoCodigo);

                List<String> cmd = new ArrayList<>();
                cmd.add(amb.clang.getAbsolutePath());
                cmd.add("-x"); cmd.add("c++");
                cmd.add("-shared");
                cmd.add("-fPIC");
                cmd.add("-O2");
                cmd.add("-std=c++17");
                cmd.add("-fuse-ld=" + amb.linker.getAbsolutePath());

                String target = abiNorm.contains("armeabi")
                        ? "armv7a-linux-androideabi21"
                        : "aarch64-linux-android21";
                cmd.add("--target=" + target);

                String abiLibDir = abiNorm.contains("armeabi")
                        ? "arm-linux-androideabi"
                        : "aarch64-linux-android";

                File sysrootUsr     = new File(amb.sysroot, "usr");
                File sysrootInclude = new File(sysrootUsr, "include");
                File sysrootCxx     = new File(sysrootInclude, "c++/v1");
                File sysrootLib     = new File(sysrootUsr,
                        "lib/" + abiLibDir + "/21");

                File clangRuntime = null;
                if (amb.libsExtras != null) {
                    File cr = new File(amb.libsExtras, "clang-runtime");
                    if (cr.isDirectory()) clangRuntime = cr;
                }

                if (clangRuntime != null) {
                    cmd.add("-resource-dir");
                    cmd.add(clangRuntime.getAbsolutePath());
                }

                cmd.add("-isysroot"); cmd.add(amb.sysroot.getAbsolutePath());
                cmd.add("-B" + sysrootLib.getAbsolutePath());
                cmd.add("-I"); cmd.add(sysrootInclude.getAbsolutePath());
                cmd.add("-I"); cmd.add(sysrootCxx.getAbsolutePath());
                cmd.add("-L"); cmd.add(sysrootLib.getAbsolutePath());
                cmd.add("-L"); cmd.add(amb.nativeDir);

                if (amb.libsExtras != null) {
                    cmd.add("-L"); cmd.add(amb.libsExtras.getAbsolutePath());
                }
                if (clangRuntime != null) {
                    cmd.add("-B" + clangRuntime.getAbsolutePath());
                    cmd.add("-L"); cmd.add(clangRuntime.getAbsolutePath());

                    String runtimeDir = abiNorm.contains("armeabi") ? "arm" : "aarch64";
                    File crAbi = new File(clangRuntime, "lib/linux/" + runtimeDir);
                    if (crAbi.isDirectory()) {
                        cmd.add("-L"); cmd.add(crAbi.getAbsolutePath());
                    }
                    File crLinux = new File(clangRuntime, "lib/linux");
                    if (crLinux.isDirectory()) {
                        cmd.add("-L"); cmd.add(crLinux.getAbsolutePath());
                    }
                }

                File pai = arquivoSaidaSo.getParentFile();
                if (pai != null && !pai.exists()) pai.mkdirs();
                if (arquivoSaidaSo.exists()) arquivoSaidaSo.delete();

                cmd.add("-o"); cmd.add(arquivoSaidaSo.getAbsolutePath());
                cmd.add(arquivoCodigo.getAbsolutePath());

                if (log != null) log.comando("Cmd", cmd);

                // ---------- Executa ----------
                ExecutorClang.Resultado exec = ExecutorClang.executar(
                        cmd, amb.nativeDir, amb.libsExtras, log);

                if (exec.codigoSaida == 0 && arquivoSaidaSo.isFile()) {
                    callback.aoSucesso(arquivoSaidaSo);
                    if (log != null) log.ok("Resultado",
                            arquivoSaidaSo.getName() + "  ("
                                    + GerenciadorLogsConsole.formatarTamanho(
                                            arquivoSaidaSo.length()) + ")");
                } else {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Exit code: ").append(exec.codigoSaida).append("\n");
                    if (exec.saida != null && !exec.saida.isEmpty()) {
                        msg.append("\n--- saída do clang ---\n");
                        msg.append(exec.saida);
                    } else {
                        msg.append("\n(clang não produziu saída)\n");
                    }
                    callback.aoErro(msg.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, "Exceção ao compilar", e);
                if (log != null) log.erro("Exceção", e.getMessage());
                callback.aoErro("Exceção: " + e.getMessage());
            }
        }, "compilador-native").start();
    }

    // ==========================================================
    //  Log helpers
    // ==========================================================

    private static void logCabecalhoProjeto(Projeto projeto,
                                            GerenciadorLogsConsole log) {
        if (log == null) return;
        log.secao("COMPILAÇÃO DO PROJETO");
        log.info("Projeto", projeto != null ? projeto.nome : "(nulo)");
        if (projeto == null) return;

        log.info("Projeto", "Versão: " + projeto.versao);
        log.info("Projeto", "Autor: " + projeto.autor);
        log.info("Projeto", "ABIs: " + String.join(", ", projeto.abis));
        log.info("Projeto", "API: " + projeto.api);
        log.info("Projeto", "Std: " + projeto.std);
        log.info("Projeto", "Otimização: " + projeto.otimizacao);
        log.info("Projeto", "Flags: " + (projeto.flags.isEmpty()
                ? "(nenhuma)" : String.join(" ", projeto.flags)));
        log.info("Projeto", "Libs: " + (projeto.libs.isEmpty()
                ? "(nenhuma)" : String.join(" ", projeto.libs)));
        log.info("Projeto", "Reler fontes: " + (projeto.fontes.isEmpty()
                ? "(automático)" : String.join(", ", projeto.fontes)));
    }

    private static void logFontes(List<File> fontes,
                                  GerenciadorLogsConsole log) {
        if (log == null) return;
        log.info("Fontes", "Total: " + fontes.size());
        for (int i = 0; i < fontes.size(); i++) {
            File f = fontes.get(i);
            log.debug("Fontes", "  [" + i + "] " + f.getName()
                    + "  (" + GerenciadorLogsConsole.formatarTamanho(f.length()) + ")"
                    + "  → " + f.getAbsolutePath());
        }
    }

    private static void logResumoFinal(int sucessos, int totalAbis,
                                       long totalMs,
                                       GerenciadorLogsConsole log) {
        if (log == null) return;
        if (sucessos == totalAbis) {
            log.sucesso("Compilação concluída: "
                    + sucessos + "/" + totalAbis + " ABI(s)", totalMs);
        } else if (sucessos == 0) {
            log.falha("Falha total", totalMs, "Nenhuma ABI compilou");
        } else {
            log.aviso("Parcial", sucessos + "/" + totalAbis
                    + " ABI(s) compilada(s) em "
                    + GerenciadorLogsConsole.formatarDuracao(totalMs));
        }
    }
}