package ui.editor.sintaxe;

import android.text.Editable;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import java.util.List;
import java.util.regex.Matcher;

public class SintaxeJson {
    private final String nome;
    private final List<String> extensoes;
    private final List<RegraSintaxe> regras;

    public SintaxeJson(String nome, List<String> extensoes, List<RegraSintaxe> regras) {
        this.nome = nome;
        this.extensoes = extensoes;
        this.regras = regras;
    }

    public boolean suportaExtensao(String extensao) {
        return extensoes.contains(extensao.toLowerCase());
    }

    public void aplicarSintaxe(Editable editable) {
        String codigo = editable.toString();

        for (RegraSintaxe regra : regras) {
            Matcher matcher = regra.getPadrao().matcher(codigo);
            while (matcher.find()) {
                editable.setSpan(
                        new ForegroundColorSpan(regra.getCor()),
                        matcher.start(),
                        matcher.end(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
        }
    }
}