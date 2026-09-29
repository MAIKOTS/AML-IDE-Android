package ui.util;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class GerenciadorNDK {

    private static final String TAG = "GerenciadorNDK";

    private static final String PASTA_SYSROOT = "sysroot";
    private static final String PASTA_LIB     = "lib";

    private GerenciadorNDK() { }

    // ==========================================================
    //  API pública
    // ==========================================================

    public static File obterSysroot(Context ctx) {
        File sysroot = new File(ctx.getFilesDir(), PASTA_SYSROOT);

        // ★ Valida arquivos-chave de ARM64 E ARM32
        boolean ok = new File(sysroot, "usr/include/c++/v1").isDirectory()
                && new File(sysroot, "usr/include/jni.h").isFile()
                && new File(sysroot, "usr/include/linux").isDirectory()
                // ARM64
                && new File(sysroot, "usr/lib/aarch64-linux-android/21/libc.so").isFile()
                && new File(sysroot, "usr/include/aarch64-linux-android/asm/unistd.h").isFile()
                // ARM32
                && new File(sysroot, "usr/lib/arm-linux-androideabi/21/libc.so").isFile()
                && new File(sysroot, "usr/include/arm-linux-androideabi/asm/unistd.h").isFile();

        if (ok) {
            return sysroot;
        }

        Log.i(TAG, "Sysroot incompleto. Re-extraindo...");
        apagarRecursivo(sysroot);

        if (!extrairAsset(ctx, PASTA_SYSROOT, sysroot)) {
            Log.e(TAG, "Falha ao extrair sysroot.");
            return null;
        }
        return sysroot;
    }

    public static File obterLibsExtras(Context ctx) {
        File destino = new File(ctx.getFilesDir(), PASTA_LIB);

        // ★ Valida arquivos-chave de ARM64 E ARM32
        boolean ok = new File(destino, "clang-runtime/include/stddef.h").isFile()
                && new File(destino, "libxml2.so.2.9.12").isFile()
                // ARM64
                && new File(destino, "clang-runtime/lib/linux/aarch64/libunwind.a").isFile()
                && new File(destino, "clang-runtime/lib/linux/libclang_rt.builtins-aarch64-android.a").isFile()
                // ARM32
                && new File(destino, "clang-runtime/lib/linux/arm/libunwind.a").isFile()
                && new File(destino, "clang-runtime/lib/linux/libclang_rt.builtins-arm-android.a").isFile();

        if (ok) {
            return destino;
        }

        Log.i(TAG, "Libs extras incompletas. Re-extraindo...");
        apagarRecursivo(destino);

        if (!extrairAsset(ctx, PASTA_LIB, destino)) {
            Log.w(TAG, "Falha ao extrair libs extras.");
            return null;
        }

        aplicarPermissoesExecucao(destino);
        return destino;
    }

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

    public static boolean estaPronta(Context ctx) {
        File clang = obterClang(ctx);
        if (!clang.isFile()) return false;

        File sysroot = new File(ctx.getFilesDir(), PASTA_SYSROOT);
        // Verifica ARM64 e ARM32
        return new File(sysroot, "usr/lib/aarch64-linux-android/21/libc.so").isFile()
                && new File(sysroot, "usr/lib/arm-linux-androideabi/21/libc.so").isFile();
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private static void aplicarPermissoesExecucao(File pasta) {
        if (!pasta.isDirectory()) return;
        File[] filhos = pasta.listFiles();
        if (filhos == null) return;
        for (File f : filhos) {
            if (f.isDirectory()) {
                aplicarPermissoesExecucao(f);
            } else {
                f.setExecutable(true, false);
                f.setReadable(true, false);
            }
        }
    }

    private static boolean extrairAsset(Context ctx, String pastaAsset, File destino) {
        Log.i(TAG, "Extraindo assets/" + pastaAsset + " → " + destino.getAbsolutePath());

        if (!destino.exists() && !destino.mkdirs()) return false;

        try {
            copiarAssetRecursivo(ctx, pastaAsset, destino);
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao extrair " + pastaAsset, e);
            return false;
        }
    }

    private static void copiarAssetRecursivo(Context ctx, String caminhoAsset, File destino)
            throws IOException {

        String[] filhos = ctx.getAssets().list(caminhoAsset);

        if (filhos == null || filhos.length == 0) {
            copiarArquivo(ctx, caminhoAsset, destino);
            return;
        }

        if (!destino.exists() && !destino.mkdirs()) {
            throw new IOException("mkdirs falhou: " + destino);
        }

        for (String filho : filhos) {
            copiarAssetRecursivo(ctx,
                    caminhoAsset + "/" + filho,
                    new File(destino, filho));
        }
    }

    private static void copiarArquivo(Context ctx, String caminhoAsset, File destino)
            throws IOException {

        File pai = destino.getParentFile();
        if (pai != null && !pai.exists() && !pai.mkdirs()) {
            throw new IOException("mkdirs falhou: " + pai);
        }

        try (InputStream in = ctx.getAssets().open(caminhoAsset);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buffer = new byte[8192];
            int lidos;
            while ((lidos = in.read(buffer)) != -1) out.write(buffer, 0, lidos);
        }

        destino.setExecutable(true, false);
        destino.setReadable(true, false);
    }

    private static void apagarRecursivo(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) for (File x : filhos) apagarRecursivo(x);
        }
        f.delete();
    }
}