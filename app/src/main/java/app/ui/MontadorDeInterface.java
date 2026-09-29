package ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import ui.estilos.GerenciadorDeEstilos;

public class MontadorDeInterface {

    private final Context contexto;
    private EditText campoEditorCodigo;
    private TextView consoleDeSaida;
    private Button botaoSalvar;
    private Button botaoCompilar;
    private Button botaoLimparConsole;

    public MontadorDeInterface(Context contexto) {
        this.contexto = contexto;
    }

    public View construirLayoutPrincipal() {
        // Container Raiz (Vertical)
        LinearLayout layoutRaiz = new LinearLayout(contexto);
        layoutRaiz.setOrientation(LinearLayout.VERTICAL);
        layoutRaiz.setBackgroundColor(Color.parseColor("#121212"));
        layoutRaiz.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Área Central: Editor de Código
        campoEditorCodigo = new EditText(contexto);
        LinearLayout.LayoutParams parametrosEditor = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        parametrosEditor.setMargins(16, 16, 16, 8);
        campoEditorCodigo.setLayoutParams(parametrosEditor);
        campoEditorCodigo.setBackground(GerenciadorDeEstilos.criarFundoCampoTextual(
                Color.parseColor("#1E1E1E"), Color.parseColor("#2D2D2D"), 2, 16));
        campoEditorCodigo.setTextColor(Color.parseColor("#E0E0E0"));
        campoEditorCodigo.setTypeface(Typeface.MONOSPACE);
        campoEditorCodigo.setGravity(Gravity.TOP | Gravity.START);
        campoEditorCodigo.setPadding(24, 24, 24, 24);
        campoEditorCodigo.setInputType(InputType.TYPE_CLASS_TEXT |
                InputType.TYPE_TEXT_FLAG_MULTI_LINE |
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        campoEditorCodigo.setHint("// Digite o código C/C++ do seu mod AML aqui...");
        campoEditorCodigo.setHintTextColor(Color.parseColor("#555555"));

        // 2. Console de Erros/Saída
        ScrollView containerConsole = new ScrollView(contexto);
        LinearLayout.LayoutParams parametrosConsole = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 280);
        parametrosConsole.setMargins(16, 8, 16, 8);
        containerConsole.setLayoutParams(parametrosConsole);
        containerConsole.setBackground(GerenciadorDeEstilos.criarFundoCampoTextual(
                Color.parseColor("#0A0A0A"), Color.parseColor("#1E1E1E"), 2, 16));

        consoleDeSaida = new TextView(contexto);
        consoleDeSaida.setTextColor(Color.parseColor("#33FF33"));
        consoleDeSaida.setTypeface(Typeface.MONOSPACE);
        consoleDeSaida.setTextSize(12);
        consoleDeSaida.setPadding(20, 20, 20, 20);
        consoleDeSaida.setText("Console pronto.\n");
        
        // Ícone do console nativo
        int resIdConsole = contexto.getResources().getIdentifier("ic_terminal", "drawable", contexto.getPackageName());
        if (resIdConsole != 0) {
            Drawable iconeConsole = contexto.getResources().getDrawable(resIdConsole);
            if (iconeConsole != null) {
                iconeConsole.setBounds(0, 0, 36, 36);
                consoleDeSaida.setCompoundDrawables(iconeConsole, null, null, null);
                consoleDeSaida.setCompoundDrawablePadding(12);
            }
        }
        
        containerConsole.addView(consoleDeSaida);

        // 3. Barra Inferior
        LinearLayout barraInferior = criarBarraInferior();

        layoutRaiz.addView(campoEditorCodigo);
        layoutRaiz.addView(containerConsole);
        layoutRaiz.addView(barraInferior);

        return layoutRaiz;
    }

    private LinearLayout criarBarraInferior() {
        LinearLayout barra = new LinearLayout(contexto);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        barra.setBackgroundColor(Color.parseColor("#181818"));
        barra.setPadding(16, 16, 16, 16);
        barra.setGravity(Gravity.CENTER);

        botaoSalvar = criarBotaoPersonalizado("Salvar", "ic_guardar", "#2D3748", "#4A5568");
        botaoCompilar = criarBotaoPersonalizado("Compilar .SO", "ic_compilar", "#2B6CB0", "#3182CE");
        botaoLimparConsole = criarBotaoPersonalizado("Limpar", "ic_limpar", "#742A2A", "#9B2C2C");

        LinearLayout.LayoutParams parametrosBotoes = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        parametrosBotoes.setMargins(8, 0, 8, 0);

        botaoSalvar.setLayoutParams(parametrosBotoes);
        botaoCompilar.setLayoutParams(parametrosBotoes);
        botaoLimparConsole.setLayoutParams(parametrosBotoes);

        barra.addView(botaoSalvar);
        barra.addView(botaoCompilar);
        barra.addView(botaoLimparConsole);

        return barra;
    }

    private Button criarBotaoPersonalizado(String texto, String nomeIconeDrawable, String corHexNormal, String corHexPressionado) {
        Button botao = new Button(contexto);
        botao.setText(texto);
        botao.setTextColor(Color.WHITE);
        botao.setTextSize(13);
        botao.setTypeface(Typeface.DEFAULT_BOLD);
        
        // Carregamento de drawable do SDK puro do Android
        int resId = contexto.getResources().getIdentifier(nomeIconeDrawable, "drawable", contexto.getPackageName());
        if (resId != 0) {
            Drawable icone = contexto.getResources().getDrawable(resId);
            if (icone != null) {
                icone.setBounds(0, 0, 42, 42);
                botao.setCompoundDrawables(icone, null, null, null);
                botao.setCompoundDrawablePadding(12);
            }
        }

        int corNormal = Color.parseColor(corHexNormal);
        int corPressionado = Color.parseColor(corHexPressionado);

        botao.setBackground(GerenciadorDeEstilos.criarFundoBotao(corNormal, corPressionado, 24));
        return botao;
    }

    public EditText obterCampoEditorCodigo() { return campoEditorCodigo; }
    public TextView obterConsoleDeSaida() { return consoleDeSaida; }
    public Button obterBotaoSalvar() { return botaoSalvar; }
    public Button obterBotaoCompilar() { return botaoCompilar; }
    public Button obterBotaoLimparConsole() { return botaoLimparConsole; }
}