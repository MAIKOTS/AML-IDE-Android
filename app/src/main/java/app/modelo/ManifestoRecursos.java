package modelo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Lê o manifest.json de recursos.
 */
public class ManifestoRecursos {

    public int versao;
    public String atualizadoEm;
    public List<RecursoRemoto> recursos = new ArrayList<>();

    public static ManifestoRecursos parse(String json) throws Exception {
        JSONObject obj = new JSONObject(json);

        ManifestoRecursos m = new ManifestoRecursos();
        m.versao       = obj.optInt("versao", 1);
        m.atualizadoEm = obj.optString("atualizado_em", "");

        JSONArray arr = obj.optJSONArray("recursos");
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);

                RecursoRemoto r = new RecursoRemoto();
                r.id           = o.getString("id");
                r.nome         = o.optString("nome", r.id);
                r.descricao    = o.optString("descricao", "");
                r.url          = o.getString("url");
                r.tamanhoBytes = o.optLong("tamanho_bytes", 0);
                r.sha256       = o.optString("sha256", "");
                r.destino      = o.optString("destino", r.id);
                r.obrigatorio  = o.optBoolean("obrigatorio", false);

                m.recursos.add(r);
            }
        }

        return m;
    }
}