package app.util.compilacao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import modelo.Projeto;
import ui.util.GerenciadorLogsConsole;

/**
 * Monta a lista de argumentos do clang pra uma ABI específica.
 * Não executa nada — só constrói a linha de comando.
 */
public final class ConstrutorComando {

    private ConstrutorComando() { }

    public static List<String> montar(Projeto projeto,
                                      String abiNorm,
                                      List<File> fontes,
                                      AmbienteCompilacao amb,
                                      GerenciadorLogsConsole log) {

        String target        = AbiUtils.montarTarget(abiNorm, projeto.api);
        String abiLibDir     = AbiUtils.montarPastaLib(abiNorm);
        String abiRuntimeDir = AbiUtils.abiRuntimeDir(abiNorm);

        File sysrootUsr        = new File(amb.sysroot, "usr");
        File sysrootInclude    = new File(sysrootUsr, "include");
        File sysrootIncludeAbi = new File(sysrootInclude, abiLibDir);
        File sysrootCxx        = new File(sysrootInclude, "c++/v1");
        File sysrootLib        = new File(sysrootUsr,
                "lib/" + abiLibDir + "/" + projeto.api);

        // -------- Prepara saída --------
        File arquivoSaida = projeto.obterArquivoSaida(abiNorm);
        if (arquivoSaida.exists()) {
            if (log != null) log.debug("Saída", "Apagando .so antigo: " + arquivoSaida);
            arquivoSaida.delete();
        }
        File pai = arquivoSaida.getParentFile();
        if (pai != null && !pai.exists()) pai.mkdirs();
        if (log != null) log.debug("Saída", "Destino: " + arquivoSaida.getAbsolutePath());

        // -------- Comando base --------
        List<String> cmd = new ArrayList<>();
        cmd.add(amb.clang.getAbsolutePath());
        cmd.add("-x");    cmd.add("c++");
        cmd.add("-shared");
        cmd.add("-fPIC");
        cmd.add("-" + projeto.otimizacao);
        cmd.add("-std=" + projeto.std);
        cmd.add("-fuse-ld=" + amb.linker.getAbsolutePath());
        cmd.add("--target=" + target);

        // -------- ARM32: flags específicas --------
        if (abiNorm.contains("armeabi")) {
            cmd.add("-march=armv7-a");
            cmd.add("-mthumb");
            cmd.add("-mfpu=neon");
            cmd.add("-mfloat-abi=softfp");
        }

        // -------- Clang runtime --------
        File clangRuntime = null;
        if (amb.libsExtras != null) {
            File cr = new File(amb.libsExtras, "clang-runtime");
            if (cr.isDirectory()) clangRuntime = cr;
        }
        if (clangRuntime != null) {
            cmd.add("-resource-dir");
            cmd.add(clangRuntime.getAbsolutePath());
        }

        // -------- Sysroot --------
        cmd.add("-isysroot"); cmd.add(amb.sysroot.getAbsolutePath());
        cmd.add("-B" + sysrootLib.getAbsolutePath());

        // ★ Includes: ABI-específico primeiro, depois genérico, depois C++
        cmd.add("-I"); cmd.add(sysrootIncludeAbi.getAbsolutePath());
        cmd.add("-I"); cmd.add(sysrootInclude.getAbsolutePath());
        cmd.add("-I"); cmd.add(sysrootCxx.getAbsolutePath());

        for (String inc : projeto.obterPastasIncludes()) {
            cmd.add("-I"); cmd.add(inc);
        }

        // -------- Libs --------
        cmd.add("-L"); cmd.add(sysrootLib.getAbsolutePath());
        cmd.add("-L"); cmd.add(amb.nativeDir);

        if (amb.libsExtras != null) {
            cmd.add("-L"); cmd.add(amb.libsExtras.getAbsolutePath());
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

        for (String libDir : projeto.obterPastasLibs(abiNorm)) {
            cmd.add("-L"); cmd.add(libDir);
        }

        for (String flag : projeto.flags) cmd.add(flag);
        for (String lib  : projeto.libs)  cmd.add(lib);

        cmd.add("-o"); cmd.add(arquivoSaida.getAbsolutePath());

        for (File f : fontes) cmd.add(f.getAbsolutePath());

        if (log != null) log.comando("Cmd", cmd);

        return cmd;
    }
}