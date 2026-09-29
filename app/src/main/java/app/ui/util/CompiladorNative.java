package ui.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import modelo.Projeto;

public final class CompiladorNative {

    private static final String TAG = "CompiladorNative";

    private CompiladorNative() { }

    public interface ResultadoCompilacao {
        void aoSucesso(File arquivoSo);
        void aoErro(String mensagemErro);
        default void aoFinalizar(int sucessos, int totalAbis, String resumo) { }
    }

    // ==========================================================
    //  API NOVA — compila o projeto inteiro (multi-ABI)
    // ==========================================================

    public static void compilarProjeto(Context ctx,
                                       Projeto projeto,
                                       ResultadoCompilacao callback) {

        new Thread(() -> {

            int           sucessos   = 0;
            int           totalAbis  = 0;
            StringBuilder logGlobal  = new StringBuilder();
            boolean       erroFatal  = false;

            try {
                // -------- Validações básicas --------
                if (projeto == null || projeto.pastaRaiz == null) {
                    callback.aoErro("Projeto inválido.");
                    erroFatal = true;
                    return;
                }

                List<File> fontes = projeto.resolverFontes();
                if (fontes.isEmpty()) {
                    callback.aoErro("Nenhum arquivo .cpp/.c encontrado em src/");
                    erroFatal = true;
                    return;
                }

                if (projeto.abis == null || projeto.abis.isEmpty()) {
                    projeto.abis = new ArrayList<>();
                    projeto.abis.add("arm64-v8a");
                }
                totalAbis = projeto.abis.size();

                // -------- Recursos compartilhados --------
                File clang = GerenciadorNDK.obterClang(ctx);
                if (clang == null || !clang.isFile()) {
                    callback.aoErro("Clang não encontrado.");
                    erroFatal = true;
                    return;
                }

                File sysroot = GerenciadorNDK.obterSysroot(ctx);
                if (sysroot == null || !sysroot.isDirectory()) {
                    callback.aoErro("Sysroot não disponível.");
                    erroFatal = true;
                    return;
                }

                File libsExtras = GerenciadorNDK.obterLibsExtras(ctx);

                // -------- Loop de ABIs --------
                for (String abiBruta : projeto.abis) {
                    String abiNorm = Projeto.normalizarAbi(abiBruta);
                    Log.i(TAG, "=== Compilando ABI: " + abiNorm + " ===");

                    AbiResultado r = compilarUmaAbi(
                            ctx, projeto, abiNorm,
                            fontes, clang, sysroot, libsExtras);

                    if (r.sucesso && r.arquivoSo != null) {
                        sucessos++;
                        Log.i(TAG, "[OK] " + abiNorm + " → "
                                + r.arquivoSo.getAbsolutePath());
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

                if (sucessos == 0) {
                    callback.aoErro("Nenhuma ABI compilou com sucesso.\n\n"
                            + logGlobal.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, "Exceção ao compilar projeto", e);
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

    private static AbiResultado compilarUmaAbi(Context ctx,
                                           Projeto projeto,
                                           String abiNorm,
                                           List<File> fontes,
                                           File clang,
                                           File sysroot,
                                           File libsExtras) {

    AbiResultado r = new AbiResultado();
    StringBuilder log = new StringBuilder();

    try {
        // -------- Alvo --------
        String target = montarTarget(abiNorm, projeto.api);
        if (target == null) {
            r.log = "ABI não suportada: " + abiNorm;
            return r;
        }

        String abiLibDir     = montarPastaLib(abiNorm);    // aarch64-linux-android | arm-linux-androideabi
        String abiRuntimeDir = abiRuntimeDir(abiNorm);     // aarch64 | arm

        // -------- Caminhos do sysroot --------
        File sysrootUsr         = new File(sysroot, "usr");
        File sysrootInclude     = new File(sysrootUsr, "include");
        File sysrootIncludeAbi  = new File(sysrootInclude, abiLibDir);  // ★ ABI-específico
        File sysrootCxx         = new File(sysrootInclude, "c++/v1");
        File sysrootLib         = new File(sysrootUsr,
                "lib/" + abiLibDir + "/" + projeto.api);

        String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
        File linker = new File(nativeDir, "libldwrapper.so");

        // -------- Clang runtime --------
        File clangRuntime = null;
        if (libsExtras != null) {
            File cr = new File(libsExtras, "clang-runtime");
            if (cr.isDirectory()) clangRuntime = cr;
        }

        // -------- Saída por ABI --------
        File arquivoSaida = projeto.obterArquivoSaida(abiNorm);
        if (arquivoSaida.exists()) arquivoSaida.delete();

        // -------- Monta comando --------
        List<String> cmd = new ArrayList<>();
        cmd.add(clang.getAbsolutePath());
        cmd.add("-x"); cmd.add("c++");
        cmd.add("-shared");
        cmd.add("-fPIC");
        cmd.add("-" + projeto.otimizacao);
        cmd.add("-std=" + projeto.std);
        cmd.add("-fuse-ld=" + linker.getAbsolutePath());
        cmd.add("--target=" + target);

        // ARM32: flags de arquitetura
        if (abiNorm.contains("armeabi")) {
            cmd.add("-march=armv7-a");
            cmd.add("-mthumb");
            cmd.add("-mfpu=neon");
            cmd.add("-mfloat-abi=softfp");
        }

        if (clangRuntime != null) {
            cmd.add("-resource-dir");
            cmd.add(clangRuntime.getAbsolutePath());
        }

        cmd.add("-isysroot"); cmd.add(sysroot.getAbsolutePath());
        cmd.add("-B" + sysrootLib.getAbsolutePath());

        // ★★★ ORDEM CRÍTICA ★★★
        // 1) ABI-específico PRIMEIRO (contém asm/ correto)
        // 2) genérico DEPOIS (contém stdio.h, jni.h, c++/v1...)
        // Sem essa ordem, <asm/sigcontext.h> resolve pro header ARM64
        // quando compilando ARM32 → erro "__uint128_t"
        cmd.add("-I"); cmd.add(sysrootIncludeAbi.getAbsolutePath());
        cmd.add("-I"); cmd.add(sysrootInclude.getAbsolutePath());
        cmd.add("-I"); cmd.add(sysrootCxx.getAbsolutePath());

        // Includes do projeto + deps
        for (String inc : projeto.obterPastasIncludes()) {
            cmd.add("-I"); cmd.add(inc);
        }

        cmd.add("-L"); cmd.add(sysrootLib.getAbsolutePath());
        cmd.add("-L"); cmd.add(nativeDir);

        if (libsExtras != null) {
            cmd.add("-L"); cmd.add(libsExtras.getAbsolutePath());
        }

        if (clangRuntime != null) {
            cmd.add("-L"); cmd.add(clangRuntime.getAbsolutePath());

            File crAbi = new File(clangRuntime, "lib/linux/" + abiRuntimeDir);
            if (crAbi.isDirectory()) {
                cmd.add("-L"); cmd.add(crAbi.getAbsolutePath());
                cmd.add("-B" + crAbi.getAbsolutePath());
            }

            File crLinux = new File(clangRuntime, "lib/linux");
            if (crLinux.isDirectory()) {
                cmd.add("-L"); cmd.add(crLinux.getAbsolutePath());
            }
        }

        // Libs de deps por ABI
        for (String libDir : projeto.obterPastasLibs(abiNorm)) {
            cmd.add("-L"); cmd.add(libDir);
        }

        for (String flag : projeto.flags) cmd.add(flag);
        for (String lib  : projeto.libs)  cmd.add(lib);

        cmd.add("-o"); cmd.add(arquivoSaida.getAbsolutePath());

        for (File f : fontes) cmd.add(f.getAbsolutePath());

        Log.i(TAG, "Comando [" + abiNorm + "]: " + cmd);

        // -------- Executa --------
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        String pathAtual = pb.environment().get("PATH");
        pb.environment().put("PATH",
                nativeDir + ":" + (pathAtual != null ? pathAtual : "/system/bin"));

        String ldPath = nativeDir;
        if (libsExtras != null) {
            ldPath = libsExtras.getAbsolutePath() + ":" + ldPath;
        }
        pb.environment().put("LD_LIBRARY_PATH", ldPath);

        Process processo = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(processo.getInputStream()))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                log.append(linha).append("\n");
            }
        }

        int codigoSaida = processo.waitFor();
        Log.i(TAG, "Exit code [" + abiNorm + "]: " + codigoSaida);

        if (codigoSaida == 0 && arquivoSaida.isFile()) {
            r.sucesso   = true;
            r.arquivoSo = arquivoSaida;
        } else {
            if (log.length() == 0) {
                log.append("(clang não produziu saída)\n");
            }
            log.append("Exit code: ").append(codigoSaida).append("\n");
        }

        r.log = log.toString();
        return r;

    } catch (Exception e) {
        Log.e(TAG, "Exceção ao compilar ABI " + abiNorm, e);
        r.log = "Exceção: " + e.getMessage();
        return r;
    }
}

    // ==========================================================
    //  API ANTIGA — compila um único arquivo (mantida)
    // ==========================================================

    public static void compilarParaSo(Context ctx,
                                      File arquivoCodigo,
                                      File arquivoSaidaSo,
                                      ResultadoCompilacao callback) {

        new Thread(() -> {
            try {
                if (arquivoCodigo == null || !arquivoCodigo.isFile()) {
                    callback.aoErro("Arquivo fonte não encontrado.");
                    return;
                }

                File clang = GerenciadorNDK.obterClang(ctx);
                if (clang == null || !clang.isFile()) {
                    callback.aoErro("Clang não encontrado.");
                    return;
                }

                File sysroot = GerenciadorNDK.obterSysroot(ctx);
                if (sysroot == null || !sysroot.isDirectory()) {
                    callback.aoErro("Sysroot não disponível.");
                    return;
                }

                File libsExtras = GerenciadorNDK.obterLibsExtras(ctx);

                File pai = arquivoSaidaSo.getParentFile();
                if (pai != null && !pai.exists()) pai.mkdirs();
                if (arquivoSaidaSo.exists()) arquivoSaidaSo.delete();

                String abi = GerenciadorNDK.obterAbi(ctx);
                String target;
                if (abi.contains("arm64")) {
                    target = "aarch64-linux-android21";
                } else if (abi.contains("armeabi")) {
                    target = "armv7a-linux-androideabi21";
                } else {
                    callback.aoErro("Arquitetura não suportada: " + abi);
                    return;
                }

                File sysrootUsr     = new File(sysroot, "usr");
                File sysrootInclude = new File(sysrootUsr, "include");
                File sysrootCxx     = new File(sysrootInclude, "c++/v1");
                File sysrootLib     = new File(sysrootUsr,
                        "lib/aarch64-linux-android/21");
                String nativeDir = ctx.getApplicationInfo().nativeLibraryDir;

                File linker = new File(nativeDir, "libldwrapper.so");

                File clangRuntime = null;
                if (libsExtras != null) {
                    File cr = new File(libsExtras, "clang-runtime");
                    if (cr.isDirectory()) clangRuntime = cr;
                }

                List<String> cmd = new ArrayList<>();
                cmd.add(clang.getAbsolutePath());
                cmd.add("-x"); cmd.add("c++");
                cmd.add("-shared");
                cmd.add("-fPIC");
                cmd.add("-O2");
                cmd.add("-std=c++17");
                cmd.add("-fuse-ld=" + linker.getAbsolutePath());
                cmd.add("--target=" + target);

                if (clangRuntime != null) {
                    cmd.add("-resource-dir");
                    cmd.add(clangRuntime.getAbsolutePath());
                }

                cmd.add("-isysroot"); cmd.add(sysroot.getAbsolutePath());
                cmd.add("-B" + sysrootLib.getAbsolutePath());
                cmd.add("-I"); cmd.add(sysrootInclude.getAbsolutePath());
                cmd.add("-I"); cmd.add(sysrootCxx.getAbsolutePath());
                cmd.add("-L"); cmd.add(sysrootLib.getAbsolutePath());
                cmd.add("-L"); cmd.add(nativeDir);

                if (libsExtras != null) {
                    cmd.add("-L"); cmd.add(libsExtras.getAbsolutePath());
                }
                if (clangRuntime != null) {
                    cmd.add("-B" + clangRuntime.getAbsolutePath());
                    cmd.add("-L"); cmd.add(clangRuntime.getAbsolutePath());

                    File crAarch64 = new File(clangRuntime, "lib/linux/aarch64");
                    if (crAarch64.isDirectory()) {
                        cmd.add("-L"); cmd.add(crAarch64.getAbsolutePath());
                    }
                    File crLinux = new File(clangRuntime, "lib/linux");
                    if (crLinux.isDirectory()) {
                        cmd.add("-L"); cmd.add(crLinux.getAbsolutePath());
                    }
                }

                cmd.add("-o"); cmd.add(arquivoSaidaSo.getAbsolutePath());
                cmd.add(arquivoCodigo.getAbsolutePath());

                Log.i(TAG, "Comando: " + cmd);

                ProcessBuilder pb = new ProcessBuilder(cmd);
                pb.redirectErrorStream(true);

                String pathAtual = pb.environment().get("PATH");
                pb.environment().put("PATH",
                        nativeDir + ":" + (pathAtual != null ? pathAtual : "/system/bin"));

                String ldPath = nativeDir;
                if (libsExtras != null) {
                    ldPath = libsExtras.getAbsolutePath() + ":" + ldPath;
                }
                pb.environment().put("LD_LIBRARY_PATH", ldPath);

                Process processo = pb.start();

                StringBuilder logs = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(processo.getInputStream()))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        logs.append(linha).append("\n");
                    }
                }

                int codigoSaida = processo.waitFor();

                if (codigoSaida == 0 && arquivoSaidaSo.isFile()) {
                    callback.aoSucesso(arquivoSaidaSo);
                } else {
                    StringBuilder msg = new StringBuilder();
                    msg.append("Exit code: ").append(codigoSaida).append("\n");
                    if (logs.length() > 0) {
                        msg.append("\n--- saída do clang ---\n");
                        msg.append(logs);
                    } else {
                        msg.append("\n(clang não produziu saída)\n");
                    }
                    callback.aoErro(msg.toString());
                }

            } catch (Exception e) {
                Log.e(TAG, "Exceção ao compilar", e);
                callback.aoErro("Exceção: " + e.getMessage());
            }
        }, "compilador-native").start();
    }

    // ==========================================================
    //  Helpers de alvo
    // ==========================================================

    private static String montarTarget(String abi, int api) {
        if (abi == null) return null;
        if (abi.contains("arm64"))   return "aarch64-linux-android" + api;
        if (abi.contains("armeabi")) return "armv7a-linux-androideabi" + api;
        if (abi.contains("x86_64"))  return "x86_64-linux-android" + api;
        if (abi.contains("x86"))     return "i686-linux-android" + api;
        return null;
    }

    private static String montarPastaLib(String abi) {
        if (abi == null) return "aarch64-linux-android";
        if (abi.contains("arm64"))   return "aarch64-linux-android";
        if (abi.contains("armeabi")) return "arm-linux-androideabi";
        if (abi.contains("x86_64"))  return "x86_64-linux-android";
        if (abi.contains("x86"))     return "i686-linux-android";
        return "aarch64-linux-android";
    }

    private static String abiRuntimeDir(String abi) {
        if (abi == null) return "aarch64";
        if (abi.contains("arm64"))   return "aarch64";
        if (abi.contains("armeabi")) return "arm";
        if (abi.contains("x86_64"))  return "x86_64";
        if (abi.contains("x86"))     return "i686";
        return "aarch64";
    }
}