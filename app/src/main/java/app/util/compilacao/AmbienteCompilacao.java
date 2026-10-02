package app.util.compilacao;

import android.content.Context;

import java.io.File;

import ui.util.GerenciadorLogsConsole;
import app.util.GerenciadorNDK;

/**
 * Snapshot dos recursos necessários pra compilar:
 * clang + sysroot + libs extras + diretório nativo.
 *
 * Faz toda a validação em um lugar só.
 */
public class AmbienteCompilacao {

    public File    clang;
    public File    sysroot;
    public File    libsExtras;    // pode ser null
    public String  nativeDir;
    public File    linker;        // libldwrapper.so

    public boolean ok;
    public String  erro;

    private AmbienteCompilacao() { }

    /**
     * Prepara o ambiente. Se algo essencial faltar, devolve com
     * ok=false e erro preenchido.
     */
    public static AmbienteCompilacao preparar(Context ctx,
                                              GerenciadorLogsConsole log) {
        AmbienteCompilacao a = new AmbienteCompilacao();

        // ---------- Clang ----------
        if (log != null) log.debug("Ambiente", "Localizando clang...");
        a.clang = GerenciadorNDK.obterClang(ctx);
        if (a.clang == null || !a.clang.isFile()) {
            a.erro = "Clang não encontrado.";
            if (log != null) log.erro("Clang", a.erro);
            return a;
        }
        if (log != null) {
            log.ok("Clang", "Encontrado");
            log.info("Clang", "Path: " + a.clang.getAbsolutePath());
            log.info("Clang", "Tamanho: "
                    + GerenciadorLogsConsole.formatarTamanho(a.clang.length()));
        }

        // ---------- Sysroot ----------
        if (log != null) log.debug("Ambiente", "Localizando sysroot...");
        a.sysroot = GerenciadorNDK.obterSysroot(ctx);
        if (a.sysroot == null || !a.sysroot.isDirectory()) {
            a.erro = "Sysroot não instalado.\n\n" +
                    "Baixe a toolchain em:\n" +
                    "   Menu → Ferramentas da IDE → Recursos";
            if (log != null) log.erro("Sysroot", "Não instalado");
            return a;
        }
        if (log != null) {
            log.ok("Sysroot", "Encontrado");
            log.info("Sysroot", "Path: " + a.sysroot.getAbsolutePath());
        }

        // ---------- Libs extras (opcional) ----------
        if (log != null) log.debug("Ambiente", "Localizando clang-runtime...");
        a.libsExtras = GerenciadorNDK.obterLibsExtras(ctx);
        if (log != null) {
            if (a.libsExtras != null) {
                log.ok("LibsExtras", "Disponível");
                log.info("LibsExtras", "Path: " + a.libsExtras.getAbsolutePath());
            } else {
                log.aviso("LibsExtras",
                        "Não instalado. Baixe em Ferramentas da IDE → Recursos.");
                log.aviso("LibsExtras",
                        "A compilação pode falhar por falta de clang-runtime/libunwind.");
            }
        }

        // ---------- Diretório nativo ----------
        a.nativeDir = ctx.getApplicationInfo().nativeLibraryDir;
        a.linker    = new File(a.nativeDir, "libldwrapper.so");
        if (log != null) {
            log.info("NativeDir", a.nativeDir);
            if (a.linker.isFile()) {
                log.info("NativeDir", "libldwrapper.so: "
                        + GerenciadorLogsConsole.formatarTamanho(a.linker.length()));
            } else {
                log.aviso("NativeDir", "libldwrapper.so NÃO encontrado!");
            }
        }

        a.ok = true;
        return a;
    }
}