package modelo;

import android.os.Environment;
import android.util.Log;

import java.io.File;

public class GerenciadorDeDiretorioApp {

    private static final String TAG = "GerenciadorDeDiretorioApp";
    private static final String NOME_PASTA_APP = "AML_IDE";

    private GerenciadorDeDiretorioApp() { }

    /**
     * Obtém ou cria o diretório principal de projetos na raiz do armazenamento interno.
     * Idempotente: se já existe, retorna sem erro.
     */
    public static File obterOuCriarDiretorioProjetos() {
        File raiz = Environment.getExternalStorageDirectory();
        if (raiz == null) {
            Log.e(TAG, "Armazenamento externo indisponível");
            return null;
        }

        File pastaDoApp = new File(raiz, NOME_PASTA_APP);

        if (pastaDoApp.exists()) {
            return pastaDoApp.isDirectory() ? pastaDoApp : null;
        }

        if (!pastaDoApp.mkdirs()) {
            Log.e(TAG, "Falha ao criar: " + pastaDoApp.getAbsolutePath());
            return null;
        }
        return pastaDoApp;
    }
}