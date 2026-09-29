package ui.editor.sintaxe;

import android.content.Context;
import java.io.File;

public class FabricaDeSintaxe {

    private static GerenciadorSintaxeAssets gerenciador;

    public static void inicializar(Context contexto) {
        if (gerenciador == null) {
            gerenciador = new GerenciadorSintaxeAssets(contexto);
        }
    }

    public static SintaxeJson obterSintaxePorArquivo(File arquivo) {
    if (arquivo == null || gerenciador == null) return null;

    String nome = arquivo.getName();
    if (nome == null || nome.isEmpty()) return null;

    String chave;
    int pontoIndex = nome.lastIndexOf('.');

    if (pontoIndex <= 0 || pontoIndex == nome.length() - 1) {
        // Sem extensão (LICENSE, README, Makefile...)
        chave = nome.toLowerCase();
    } else {
        chave = nome.substring(pontoIndex + 1).toLowerCase();
    }

    return gerenciador.obterSintaxePorExtensao(chave);
}
}