package ui.editor.sintaxe;

import android.graphics.Color;
import java.util.regex.Pattern;

public class RegraSintaxe {
    private final String nome;
    private final Pattern padrao;
    private final int cor;

    public RegraSintaxe(String nome, String regex, String hexCor) {
        this.nome = nome;
        this.padrao = Pattern.compile(regex, Pattern.MULTILINE);
        this.cor = Color.parseColor(hexCor);
    }

    public Pattern getPadrao() { return padrao; }
    public int getCor() { return cor; }
}