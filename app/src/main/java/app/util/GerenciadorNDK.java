package app.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;

/**
 * Localização e validação dos recursos nativos da IDE
 * (sysroot, clang-runtime, etc.).
 *
 * ★ Os recursos NÃO são mais extraídos dos assets do APK.
 *   Agora vêm pelo GerenciadorRecursos (download da nuvem).
 *
 * Esta classe apenas:
 *  - Localiza onde os recursos devem estar
 *  - Valida se estão íntegros
 *  - Devolve null se não estiverem instalados
 */
public final class GerenciadorNDK {

    private static final String TAG = "GerenciadorNDK";

    private static final String PASTA_SYSROOT = "sysroot";
    private static final String PASTA_LIB     = "lib";

    // Tamanho mínimo (bytes) para considerar um arquivo válido.
    private static final long MIN_CRT      = 200;
    private static final long MIN_LIB      = 1000;
    private static final long MIN_HEADER   = 100;
    private static final long MIN_ARCHIVE  = 5000;

    private GerenciadorNDK() { }

    // ==========================================================
    //  API pública
    // ==========================================================

    /**
     * Retorna a pasta do sysroot se estiver íntegra.
     * Se não estiver instalada, retorna null
     * (o usuário deve baixar em Complementos → Recursos).
     */
    public static File obterSysroot(Context ctx) {
        File sysroot = new File(ctx.getFilesDir(), PASTA_SYSROOT);

        if (sysrootEstaIntegro(sysroot)) {
            return sysroot;
        }

        Log.w(TAG, "Sysroot não instalado ou incompleto. "
                + "Baixe em Complementos → Recursos da IDE.");
        return null;
    }

    /**
     * Retorna a pasta das libs extras (clang-runtime, libxml2) se estiver íntegra.
     * Se não estiver instalada, retorna null.
     */
    public static File obterLibsExtras(Context ctx) {
        File destino = new File(ctx.getFilesDir(), PASTA_LIB);

        if (libsExtrasEstaoIntegras(destino)) {
            return destino;
        }

        Log.w(TAG, "Libs extras não instaladas ou incompletas. "
                + "Baixe em Complementos → Recursos da IDE.");
        return null;
    }

    /** Apaga sysroot e libs (útil pra forçar novo download). */
    public static void limparRecursosInstalados(Context ctx) {
        Log.w(TAG, "Apagando sysroot e libs instalados...");

        File sysroot = new File(ctx.getFilesDir(), PASTA_SYSROOT);
        File libs    = new File(ctx.getFilesDir(), PASTA_LIB);

        apagarRecursivo(sysroot);
        apagarRecursivo(libs);
    }

    // ==========================================================
    //  Localização das libs nativas do APK
    // ==========================================================

    public static File obterRunner(Context ctx) {
        String dir = ctx.getApplicationInfo().nativeLibraryDir;
        return new File(dir, "librunner.so");
    }

    public static File obterClang(Context ctx) {
        String dir = ctx.getApplicationInfo().nativeLibraryDir;
        return new File(dir, "libclang.so");
    }

    public static String obterAbi(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            String[] abis = Build.SUPPORTED_ABIS;
            if (abis != null && abis.length > 0) return abis[0];
        }
        return Build.CPU_ABI;
    }

    // ==========================================================
    //  Estado
    // ==========================================================

    /** A toolchain está pronta pra compilar? (clang + sysroot). */
    public static boolean estaPronta(Context ctx) {
        File clang = obterClang(ctx);
        if (!clang.isFile()) return false;

        File sysroot = new File(ctx.getFilesDir(), PASTA_SYSROOT);
        return sysrootEstaIntegro(sysroot);
    }

    // ==========================================================
    //  Validação de integridade
    // ==========================================================

    private static boolean sysrootEstaIntegro(File sysroot) {
        if (!new File(sysroot, "usr/include/c++/v1").isDirectory()) return false;
        if (!new File(sysroot, "usr/include/linux").isDirectory())   return false;

        if (!arquivoValido(new File(sysroot, "usr/include/jni.h"), MIN_HEADER)) {
            return false;
        }
        if (!arquivoValido(new File(sysroot,
                "usr/include/aarch64-linux-android/asm/unistd.h"), MIN_HEADER)) {
            return false;
        }

        if (!arquivoValido(new File(sysroot,
                "usr/lib/aarch64-linux-android/21/crtbegin_so.o"), MIN_CRT)) {
            return false;
        }
        if (!arquivoValido(new File(sysroot,
                "usr/lib/aarch64-linux-android/21/crtend_so.o"), MIN_CRT)) {
            return false;
        }

        if (!arquivoValido(new File(sysroot,
                "usr/lib/aarch64-linux-android/21/libc.so"), MIN_LIB)) {
            return false;
        }
        if (!arquivoValido(new File(sysroot,
                "usr/lib/aarch64-linux-android/libc++_shared.so"), MIN_LIB)) {
            return false;
        }

        if (!arquivoValido(new File(sysroot,
                "usr/lib/arm-linux-androideabi/21/crtbegin_so.o"), MIN_CRT)) {
            return false;
        }
        if (!arquivoValido(new File(sysroot,
                "usr/lib/arm-linux-androideabi/21/libc.so"), MIN_LIB)) {
            return false;
        }

        return true;
    }

    private static boolean libsExtrasEstaoIntegras(File destino) {
        if (!arquivoValido(new File(destino,
                "clang-runtime/include/stddef.h"), MIN_HEADER)) {
            return false;
        }
        if (!arquivoValido(new File(destino,
                "clang-runtime/lib/linux/aarch64/libunwind.a"), MIN_ARCHIVE)) {
            return false;
        }
        if (!arquivoValido(new File(destino,
                "clang-runtime/lib/linux/libclang_rt.builtins-aarch64-android.a"),
                MIN_ARCHIVE)) {
            return false;
        }
        if (!arquivoValido(new File(destino,
                "clang-runtime/lib/linux/arm/libunwind.a"), MIN_ARCHIVE)) {
            return false;
        }
        if (!arquivoValido(new File(destino,
                "clang-runtime/lib/linux/libclang_rt.builtins-arm-android.a"),
                MIN_ARCHIVE)) {
            return false;
        }
        if (!arquivoValido(new File(destino, "libxml2.so.2.9.12"), MIN_LIB)) {
            return false;
        }
        return true;
    }

    private static boolean arquivoValido(File f, long tamanhoMinimo) {
        return f.isFile() && f.length() >= tamanhoMinimo;
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private static void apagarRecursivo(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) for (File x : filhos) apagarRecursivo(x);
        }
        f.delete();
    }
}