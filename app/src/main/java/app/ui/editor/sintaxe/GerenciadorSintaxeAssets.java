package ui.editor.sintaxe;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GerenciadorSintaxeAssets {

    private final List<SintaxeJson> sintaxesCarregadas = new ArrayList<>();

    public GerenciadorSintaxeAssets(Context contexto) {
        carregarSintaxes(contexto);
    }

    private void carregarSintaxes(Context contexto) {
        try {
            String[] arquivos = contexto.getAssets().list("sintaxes");
            if (arquivos == null) return;

            for (String arquivo : arquivos) {
                if (arquivo.endsWith(".json")) {
                    InputStream is = contexto.getAssets().open("sintaxes/" + arquivo);
                    int size = is.available();
                    byte[] buffer = new byte[size];
                    is.read(buffer);
                    is.close();

                    String jsonStr = new String(buffer, StandardCharsets.UTF_8);
                    JSONObject json = new JSONObject(jsonStr);

                    String nome = json.getString("nome");
                    
                    // Extensões
                    JSONArray arrExt = json.getJSONArray("extensoes");
                    List<String> extensoes = new ArrayList<>();
                    for (int i = 0; i < arrExt.length(); i++) {
                        extensoes.add(arrExt.getString(i));
                    }

                    // Regras
                    JSONArray arrRegras = json.getJSONArray("regras");
                    List<RegraSintaxe> regras = new ArrayList<>();
                    for (int i = 0; i < arrRegras.length(); i++) {
                        JSONObject objRegra = arrRegras.getJSONObject(i);
                        regras.add(new RegraSintaxe(
                                objRegra.getString("nome"),
                                objRegra.getString("regex"),
                                objRegra.getString("cor")
                        ));
                    }

                    sintaxesCarregadas.add(new SintaxeJson(nome, extensoes, regras));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SintaxeJson obterSintaxePorExtensao(String extensao) {
        for (SintaxeJson sintaxe : sintaxesCarregadas) {
            if (sintaxe.suportaExtensao(extensao)) {
                return sintaxe;
            }
        }
        return null;
    }
}