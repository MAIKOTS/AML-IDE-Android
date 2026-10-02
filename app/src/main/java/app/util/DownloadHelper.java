package app.util;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;

/**
 * Helper de download HTTP com:
 *  - Segue redirect (GitHub Releases redireciona)
 *  - Reporta progresso
 *  - Aceita cancelamento
 *  - Calcula SHA256
 *
 * Uso:
 *   DownloadHelper.baixar(url, arquivo, new DownloadHelper.Callback() {
 *       @Override public void aoProgresso(long b, long t, int p) { ... }
 *       @Override public void aoConcluir(File f) { ... }
 *       @Override public void aoErro(String m) { ... }
 *       @Override public boolean continuar() { return true; }
 *   });
 */
public final class DownloadHelper {

    private static final String TAG = "DownloadHelper";
    private static final int TIMEOUT_CONEXAO = 30000;
    private static final int TIMEOUT_LEITURA = 60000;
    private static final int MAX_REDIRECTS   = 5;

    private DownloadHelper() { }

    // ==========================================================
    //  Callback
    // ==========================================================

    public interface Callback {
        void aoProgresso(long bytesBaixados, long totalBytes, int percentual);
        void aoConcluir(File arquivo);
        void aoErro(String mensagem);
        /** Retorne false para cancelar o download. */
        boolean continuar();
    }

    // ==========================================================
    //  Download
    // ==========================================================

    /**
     * Baixa a URL para o arquivo de destino numa thread separada.
     * O arquivo é gravado primeiro em .part e renomeado no fim
     * (evita arquivo corrompido se o app crashar no meio).
     */
    public static void baixar(final String url, final File destino, final Callback cb) {
        new Thread(() -> {
            File temp = new File(destino.getAbsolutePath() + ".part");
            try {
                HttpURLConnection conn = abrirComRedirect(url);

                long total = conn.getContentLengthLong();

                File pai = destino.getParentFile();
                if (pai != null && !pai.exists()) pai.mkdirs();

                try (InputStream in  = new BufferedInputStream(conn.getInputStream(), 65536);
                     FileOutputStream out = new FileOutputStream(temp)) {

                    byte[] buf = new byte[65536];
                    long baixados = 0;
                    int ultimoPct = -1;
                    int lidos;

                    while ((lidos = in.read(buf)) != -1) {
                        if (cb != null && !cb.continuar()) {
                            temp.delete();
                            return;
                        }

                        out.write(buf, 0, lidos);
                        baixados += lidos;

                        if (cb != null && total > 0) {
                            int pct = (int) (baixados * 100 / total);
                            if (pct != ultimoPct) {
                                ultimoPct = pct;
                                cb.aoProgresso(baixados, total, pct);
                            }
                        }
                    }
                    out.flush();
                }

                // Renomeia .part → destino
                if (destino.exists()) destino.delete();
                if (!temp.renameTo(destino)) {
                    throw new Exception("Falha ao renomear temp → destino");
                }

                if (cb != null) cb.aoConcluir(destino);

            } catch (Exception e) {
                Log.e(TAG, "Download falhou", e);
                temp.delete();
                if (cb != null) cb.aoErro(e.getMessage());
            }
        }, "download-recurso").start();
    }

    // ==========================================================
    //  HTTP
    // ==========================================================

    private static HttpURLConnection abrirComRedirect(String urlString) throws Exception {
        String atual = urlString;

        for (int i = 0; i < MAX_REDIRECTS; i++) {
            URL url = new URL(atual);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(TIMEOUT_CONEXAO);
            conn.setReadTimeout(TIMEOUT_LEITURA);
            conn.setRequestProperty("User-Agent", "AML-IDE/1.0");

            int codigo = conn.getResponseCode();

            if (codigo == HttpURLConnection.HTTP_OK) return conn;

            if (codigo == HttpURLConnection.HTTP_MOVED_TEMP
                    || codigo == HttpURLConnection.HTTP_MOVED_PERM
                    || codigo == 307
                    || codigo == 308) {
                String nova = conn.getHeaderField("Location");
                conn.disconnect();
                if (nova == null || nova.isEmpty()) {
                    throw new Exception("Redirect sem Location");
                }
                atual = nova;
                continue;
            }

            conn.disconnect();
            throw new Exception("HTTP " + codigo);
        }
        throw new Exception("Muitos redirects");
    }

    // ==========================================================
    //  SHA256
    // ==========================================================

    public static String calcularSha256(File arquivo) {
        if (arquivo == null || !arquivo.isFile()) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (InputStream in = new FileInputStream(arquivo)) {
                byte[] buf = new byte[8192];
                int lidos;
                while ((lidos = in.read(buf)) != -1) {
                    md.update(buf, 0, lidos);
                }
            }
            byte[] hash = md.digest();
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Erro SHA256", e);
            return null;
        }
    }
}