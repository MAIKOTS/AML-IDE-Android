package app.util;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import modelo.ManifestoRecursos;
import modelo.RecursoRemoto;

/**
 * Gerencia o ciclo de vida dos recursos essenciais da IDE:
 *
 *   1. Lê o manifest.json (remoto com fallback local)
 *   2. Verifica o que já está instalado
 *   3. Baixa só o que falta
 *   4. Verifica SHA256
 *   5. Extrai para files/<destino>/
 *
 * Uso:
 *   GerenciadorRecursos g = new GerenciadorRecursos(ctx);
 *   g.baixarManifesto(cb);
 *   g.instalar(recurso, cbInstalacao);
 */
public final class GerenciadorRecursos {

    private static final String TAG = "GerenciadorRecursos";

    // ★★★ MUDE AQUI pro seu repositório ★★★
    public static final String URL_MANIFESTO =
            "https://raw.githubusercontent.com/MAIKOTS/aml-ide-recursos/main/manifest.json";

    private static final String PASTA_RECURSOS = "recursos";

    private final Context ctx;

    public GerenciadorRecursos(Context ctx) {
        this.ctx = ctx;
    }

    // ==========================================================
    //  Caminhos
    // ==========================================================

    /** files/recursos/ — onde ficam manifest e zips baixados. */
    public File obterPastaRecursos() {
        File f = new File(ctx.getFilesDir(), PASTA_RECURSOS);
        if (!f.exists()) f.mkdirs();
        return f;
    }

    public File obterArquivoManifesto() {
        return new File(obterPastaRecursos(), "manifest.json");
    }

    public File obterArquivoZip(RecursoRemoto r) {
        return new File(obterPastaRecursos(), r.id + ".zip");
    }

    public File obterPastaDestino(RecursoRemoto r) {
        if (r.destino == null || r.destino.isEmpty()) {
            return ctx.getFilesDir();
        }
        return new File(ctx.getFilesDir(), r.destino);
    }

    // ==========================================================
    //  Manifesto
    // ==========================================================

    public interface CallbackManifesto {
        void aoConcluir(ManifestoRecursos manifesto);
        void aoErro(String mensagem);
    }

    /**
     * Baixa o manifest remoto. Se falhar, tenta o local.
     * Se não houver nenhum, retorna erro.
     */
    public void baixarManifesto(final CallbackManifesto cb) {
        DownloadHelper.baixar(URL_MANIFESTO, obterArquivoManifesto(),
                new DownloadHelper.Callback() {

                    @Override public void aoProgresso(long b, long t, int p) { }
                    @Override public boolean continuar() { return true; }

                    @Override
                    public void aoConcluir(File arquivo) {
                        try {
                            ManifestoRecursos m = ManifestoRecursos.parse(
                                    lerTexto(arquivo));
                            if (cb != null) cb.aoConcluir(m);
                        } catch (Exception e) {
                            // JSON corrompido — tenta local
                            ManifestoRecursos local = lerManifestoLocal();
                            if (local != null) {
                                if (cb != null) cb.aoConcluir(local);
                            } else if (cb != null) {
                                cb.aoErro("Manifesto inválido: " + e.getMessage());
                            }
                        }
                    }

                    @Override
                    public void aoErro(String msg) {
                        ManifestoRecursos local = lerManifestoLocal();
                        if (local != null) {
                            if (cb != null) cb.aoConcluir(local);
                        } else if (cb != null) {
                            cb.aoErro(msg);
                        }
                    }
                });
    }

    /** Lê o manifest do disco ou dos assets. */
    public ManifestoRecursos lerManifestoLocal() {
        // 1. Baixado anteriormente
        File local = obterArquivoManifesto();
        if (local.isFile()) {
            try {
                return ManifestoRecursos.parse(lerTexto(local));
            } catch (Exception ignored) { }
        }

        // 2. Embutido no APK (assets/recursos/manifest.json)
        try (InputStream in = ctx.getAssets().open("recursos/manifest.json")) {
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[8192];
            int lidos;
            while ((lidos = in.read(buf)) != -1) {
                sb.append(new String(buf, 0, lidos, "UTF-8"));
            }
            return ManifestoRecursos.parse(sb.toString());
        } catch (Exception ignored) { }

        return null;
    }

    // ==========================================================
    //  Instalação
    // ==========================================================

    public interface CallbackInstalacao {
        /**
         * etapa = "Baixando", "Verificando", "Extraindo" (com pct 0..100),
         * ou texto livre com pct = -1.
         */
        void aoProgresso(String etapa, int pct);
        void aoConcluir();
        void aoErro(String mensagem);
        boolean cancelado();
    }

    /** Já está instalado? */
    public boolean estaInstalado(RecursoRemoto r) {
        if (r == null) return false;

        // Caso especial: destino vazio = checa assinaturas específicas
        if (r.destino == null || r.destino.isEmpty()) {
            if ("sysroot".equals(r.id)) {
                return new File(ctx.getFilesDir(),
                        "sysroot/usr/lib/aarch64-linux-android/21/libc.so").isFile();
            }
            if ("lib".equals(r.id)) {
                return new File(ctx.getFilesDir(),
                        "lib/clang-runtime/include/stddef.h").isFile();
            }
            return false;
        }

        File destino = obterPastaDestino(r);
        if (!destino.isDirectory()) return false;

        File[] filhos = destino.listFiles();
        return filhos != null && filhos.length > 0;
    }

    /** Remove o recurso (apaga a pasta destino + zip baixado). */
    public void remover(RecursoRemoto r) {
        if (r == null) return;
        apagarRecursivo(obterPastaDestino(r));
        obterArquivoZip(r).delete();
    }

    /**
     * Baixa, verifica e extrai o recurso.
     * Executa tudo em background; chama os callbacks conforme avança.
     */
    /**
 * Baixa, verifica e extrai o recurso.
 * Executa tudo em background; chama os callbacks conforme avança.
 *
 * ★ Após extrair com sucesso, o .zip é apagado automaticamente
 *   pra economizar espaço.
 */
public void instalar(final RecursoRemoto r, final CallbackInstalacao cb) {
    if (r == null) {
        if (cb != null) cb.aoErro("Recurso nulo");
        return;
    }

    new Thread(() -> {
        try {
            File zip = obterArquivoZip(r);

            // ---------- 1. Precisa baixar? ----------
            boolean precisaBaixar = !zip.isFile();
            if (!precisaBaixar && r.sha256 != null && !r.sha256.isEmpty()) {
                String sha = DownloadHelper.calcularSha256(zip);
                if (!r.sha256.equalsIgnoreCase(sha)) {
                    zip.delete();
                    precisaBaixar = true;
                }
            }

            if (precisaBaixar) {
                if (!baixarComEspera(r, zip, cb)) return;
            }

            // ---------- 2. Verifica SHA256 ----------
            if (r.sha256 != null && !r.sha256.isEmpty()) {
                if (cb != null) cb.aoProgresso("Verificando integridade", -1);
                String sha = DownloadHelper.calcularSha256(zip);
                if (!r.sha256.equalsIgnoreCase(sha)) {
                    zip.delete();
                    if (cb != null) cb.aoErro("SHA256 inválido — arquivo corrompido");
                    return;
                }
            }

            // ---------- 3. Extrai ----------
            if (cb != null) cb.aoProgresso("Extraindo " + r.nome, 0);

            File destino = obterPastaDestino(r);
            if (!destino.exists()) destino.mkdirs();

            extrairZip(zip, destino, cb);

            // ---------- 4. ★ Limpa o .zip ----------
            if (zip.exists() && !zip.delete()) {
                Log.w(TAG, "Não foi possível apagar o zip: " + zip.getAbsolutePath());
            } else {
                Log.i(TAG, "Zip apagado: " + zip.getName());
            }

            if (cb != null) cb.aoConcluir();

        } catch (Exception e) {
            Log.e(TAG, "Falha instalando " + r.id, e);
            if (cb != null) cb.aoErro(e.getMessage() != null
                    ? e.getMessage() : "Erro desconhecido");
        }
    }, "instalar-recurso").start();
}

    // ==========================================================
    //  Helpers internos
    // ==========================================================

    private boolean baixarComEspera(final RecursoRemoto r,
                                     final File zip,
                                     final CallbackInstalacao cb) throws Exception {

        if (cb != null) cb.aoProgresso("Baixando " + r.nome, 0);

        final Object lock = new Object();
        final boolean[] fim = { false };
        final String[] erro = { null };

        DownloadHelper.baixar(r.url, zip, new DownloadHelper.Callback() {
            @Override public void aoProgresso(long b, long t, int p) {
                if (cb != null) cb.aoProgresso("Baixando " + r.nome, p);
            }
            @Override public boolean continuar() {
                return cb == null || !cb.cancelado();
            }
            @Override public void aoConcluir(File arquivo) {
                synchronized (lock) { fim[0] = true; lock.notifyAll(); }
            }
            @Override public void aoErro(String msg) {
                synchronized (lock) { erro[0] = msg; fim[0] = true; lock.notifyAll(); }
            }
        });

        synchronized (lock) {
            while (!fim[0]) lock.wait();
        }

        if (erro[0] != null) {
            if (cb != null) cb.aoErro("Falha no download: " + erro[0]);
            return false;
        }
        return true;
    }

    private void extrairZip(File zip, File destino, CallbackInstalacao cb) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry;
            byte[] buf = new byte[65536];
            int total = 0;

            while ((entry = zis.getNextEntry()) != null) {
                if (cb != null && cb.cancelado()) return;

                File out = new File(destino, entry.getName());

                // Segurança: path traversal
                String canonDest = destino.getCanonicalPath();
                String canonOut  = out.getCanonicalPath();
                if (!canonOut.equals(canonDest)
                        && !canonOut.startsWith(canonDest + File.separator)) {
                    throw new Exception("Zip inválido: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    out.mkdirs();
                    continue;
                }

                File pai = out.getParentFile();
                if (pai != null && !pai.exists()) pai.mkdirs();

                try (OutputStream os = new FileOutputStream(out)) {
                    int lidos;
                    while ((lidos = zis.read(buf)) != -1) os.write(buf, 0, lidos);
                }

                total++;
                if (cb != null && total % 20 == 0) {
                    cb.aoProgresso("Extraindo (" + total + " arquivos)", -1);
                }
            }
        }
    }

    private String lerTexto(File f) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (InputStream in = new FileInputStream(f)) {
            byte[] buf = new byte[8192];
            int lidos;
            while ((lidos = in.read(buf)) != -1) {
                sb.append(new String(buf, 0, lidos, "UTF-8"));
            }
        }
        return sb.toString();
    }

    public static void apagarRecursivo(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) for (File x : filhos) apagarRecursivo(x);
        }
        f.delete();
    }

    // ==========================================================
    //  Espaço em disco
    // ==========================================================

    public long obterEspacoLivre() {
        return ctx.getFilesDir().getUsableSpace();
    }

    public static String formatarTamanho(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}