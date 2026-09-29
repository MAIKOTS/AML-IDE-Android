package app.util;

import android.util.Log;

/**
 * Carrega as bibliotecas nativas necessárias para o compilador.
 * O loadLibrary força o Android a extrair as libs para
 * nativeLibraryDir/ antes de qualquer tentativa de execução.
 */
public final class LibsNativas {

    private static final String TAG = "LibsNativas";

    private LibsNativas() { }

    public static void carregar() {
        carregar("c++_shared");
        carregar("ldreal");
        carregar("ldwrapper");
    }

    private static void carregar(String nome) {
        try {
            System.loadLibrary(nome);
        } catch (Throwable t) {
            Log.w(TAG, nome + ": " + t.getMessage());
        }
    }
}