package app.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class GerenciadorDeArquivos {

    private static final String TAG = "GerenciadorDeArquivos";

    private GerenciadorDeArquivos() { }

    // ==========================================================
    //  LEITURA
    // ==========================================================

    /**
     * Lê um arquivo como texto UTF-8, preservando as quebras de linha originais.
     *
     * @return conteúdo do arquivo, ou null em caso de erro.
     */
    public static String lerArquivo(File arquivo) {
        if (arquivo == null) {
            Log.w(TAG, "lerArquivo: arquivo nulo");
            return null;
        }
        if (!arquivo.exists()) {
            Log.w(TAG, "lerArquivo: arquivo não existe -> " + arquivo.getAbsolutePath());
            return null;
        }
        if (arquivo.isDirectory()) {
            Log.w(TAG, "lerArquivo: é um diretório -> " + arquivo.getAbsolutePath());
            return null;
        }

        StringBuilder conteudo = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(arquivo),
                        StandardCharsets.UTF_8))) {

            char[] buffer = new char[8192];
            int lidos;
            while ((lidos = br.read(buffer)) != -1) {
                conteudo.append(buffer, 0, lidos);
            }
        } catch (IOException e) {
            Log.e(TAG, "Falha ao ler: " + arquivo.getAbsolutePath(), e);
            return null;
        }

        return conteudo.toString();
    }

    /**
     * Variante segura: retorna "" se algo der errado.
     * Útil quando você não quer lidar com null.
     */
    public static String lerArquivoOuVazio(File arquivo) {
        String s = lerArquivo(arquivo);
        return s != null ? s : "";
    }

    // ==========================================================
    //  ESCRITA
    // ==========================================================

    /**
     * Salva texto UTF-8 no arquivo (sobrescrevendo).
     * A escrita é atômica: grava em um temporário e renomeia,
     * evitando arquivos corrompidos em caso de crash.
     *
     * @return true em caso de sucesso.
     */
    public static boolean salvarArquivo(File arquivo, String conteudo) {
        if (arquivo == null || conteudo == null) {
            Log.w(TAG, "salvarArquivo: parâmetros nulos");
            return false;
        }
        if (arquivo.exists() && arquivo.isDirectory()) {
            Log.w(TAG, "salvarArquivo: é um diretório -> " + arquivo.getAbsolutePath());
            return false;
        }

        // Garante que o diretório-pai existe
        File pai = arquivo.getParentFile();
        if (pai != null && !pai.exists()) {
            if (!pai.mkdirs()) {
                Log.e(TAG, "Não foi possível criar diretório-pai: " + pai.getAbsolutePath());
                return false;
            }
        }

        // Escreve em arquivo temporário, depois renomeia (escrita atômica)
        File temp = new File(arquivo.getParentFile(), arquivo.getName() + ".tmp");

        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(temp),
                        StandardCharsets.UTF_8))) {

            bw.write(conteudo);
            bw.flush();
        } catch (IOException e) {
            Log.e(TAG, "Falha ao escrever temporário: " + temp.getAbsolutePath(), e);
            temp.delete();
            return false;
        }

        // Renomeia (sobrescreve) — atômico no mesmo sistema de arquivos
        if (arquivo.exists() && !arquivo.delete()) {
            Log.w(TAG, "Não foi possível apagar o arquivo antigo, tentando rename mesmo assim");
        }
        if (!temp.renameTo(arquivo)) {
            Log.e(TAG, "Falha ao renomear temp -> destino");
            temp.delete();
            return false;
        }

        return true;
    }

    // ==========================================================
    //  UTILITÁRIOS
    // ==========================================================

    public static boolean existe(File arquivo) {
        return arquivo != null && arquivo.exists();
    }

    public static long tamanho(File arquivo) {
        return (arquivo != null && arquivo.isFile()) ? arquivo.length() : -1;
    }
}