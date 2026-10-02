package ui.dialogos;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import app.util.ModoImersivo;

/**
 * Sistema de diálogos do app — versão compacta.
 *
 * Substitui os AlertDialog nativos (que quebram o modo imersivo)
 * por dialogs customizados com a paleta do app.
 */
public final class DialogoApp {

    // ==========================================================
    //  Paleta
    // ==========================================================
    private static final int COR_TITULO    = 0xFFF4F4F5;
    private static final int COR_TEXTO     = 0xFFE4E4E7;
    private static final int COR_SECUNDARIO= 0xFFA1A1AA;
    private static final int COR_HINT      = 0xFF52525B;
    private static final int COR_VERDE     = 0xFF00E676;
    private static final int COR_VERMELHO  = 0xFFFF5252;
    private static final int COR_BORDA     = 0xFF27272A;

    private DialogoApp() { }

    // ==========================================================
    //  Interfaces de callback
    // ==========================================================
    public interface OnConfirmar { void onConfirmar(); }
    public interface OnInput     { void onInput(String valor); }
    public interface OnEscolha   { void onEscolha(int posicao, String valor); }

    // ==========================================================
    //  1. Confirmação (OK / Cancelar)
    // ==========================================================
    public static Dialog confirmar(Context ctx,
                                   String titulo,
                                   String mensagem,
                                   String textoPositivo,
                                   String textoNegativo,
                                   final OnConfirmar onConfirmar) {
        return confirmar(ctx, titulo, mensagem, textoPositivo,
                textoNegativo, false, onConfirmar);
    }

    public static Dialog confirmar(Context ctx,
                                   String titulo,
                                   String mensagem,
                                   String textoPositivo,
                                   String textoNegativo,
                                   boolean destrutivo,
                                   final OnConfirmar onConfirmar) {

        final Dialog dialog = novoDialog(ctx);
        LinearLayout card = criarCard(ctx, true);

        if (titulo != null && !titulo.isEmpty()) {
            card.addView(criarTitulo(ctx, titulo));
        }

        if (mensagem != null && !mensagem.isEmpty()) {
            card.addView(criarMensagem(ctx, mensagem));
        }

        card.addView(criarBotoes(ctx, dialog,
                textoPositivo, destrutivo ? COR_VERMELHO : COR_VERDE,
                textoNegativo, COR_SECUNDARIO,
                () -> { if (onConfirmar != null) onConfirmar.onConfirmar(); },
                null));

        return montarDialog(dialog, ctx, card);
    }

    // ==========================================================
    //  2. Lista de ações (com ícone)
    // ==========================================================
    public static Dialog listaAcoes(Context ctx,
                                    String titulo,
                                    final List<ItemAcao> itens) {

        final Dialog dialog = novoDialog(ctx);
        LinearLayout card = criarCard(ctx, false);

        if (titulo != null && !titulo.isEmpty()) {
            LinearLayout header = new LinearLayout(ctx);
            header.setOrientation(LinearLayout.VERTICAL);
            header.setPadding(dp(ctx, 18), dp(ctx, 14), dp(ctx, 18), dp(ctx, 8));
            header.addView(criarTitulo(ctx, titulo));
            card.addView(header);
        }

        ScrollView scroll = new ScrollView(ctx);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout lista = new LinearLayout(ctx);
        lista.setOrientation(LinearLayout.VERTICAL);
        lista.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (final ItemAcao item : itens) {
            lista.addView(criarLinhaAcao(ctx, dialog, item));
        }
        scroll.addView(lista);
        card.addView(scroll);

        // Espaço inferior sutil
        View espaco = new View(ctx);
        espaco.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 6)));
        card.addView(espaco);

        return montarDialog(dialog, ctx, card);
    }

    // ==========================================================
    //  3. Input de texto
    // ==========================================================
    public static Dialog input(Context ctx,
                               String titulo,
                               String hint,
                               String valorInicial,
                               final OnInput onInput) {

        final Dialog dialog = novoDialog(ctx);
        LinearLayout card = criarCard(ctx, true);

        card.addView(criarTitulo(ctx, titulo));

        final EditText campo = new EditText(ctx);
        campo.setHint(hint);
        campo.setText(valorInicial != null ? valorInicial : "");
        if (valorInicial != null) campo.setSelection(valorInicial.length());
        campo.setSingleLine(true);
        campo.setTypeface(Typeface.MONOSPACE);
        campo.setTextColor(COR_TEXTO);
        campo.setHintTextColor(COR_HINT);
        campo.setTextSize(12);
        campo.setBackgroundResource(
                ctx.getResources().getIdentifier(
                        "bg_dialogo_input", "drawable", ctx.getPackageName()));
        campo.setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8));

        LinearLayout.LayoutParams lpCampo = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lpCampo.topMargin = dp(ctx, 8);
        lpCampo.bottomMargin = dp(ctx, 6);
        campo.setLayoutParams(lpCampo);

        card.addView(campo);

        card.addView(criarBotoes(ctx, dialog,
                "OK", COR_VERDE,
                "Cancelar", COR_SECUNDARIO,
                () -> {
                    String v = campo.getText().toString().trim();
                    if (onInput != null) onInput.onInput(v);
                },
                null));

        final Dialog d = montarDialog(dialog, ctx, card);
        campo.requestFocus();
        if (d.getWindow() != null) {
            d.getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
        return d;
    }

    // ==========================================================
    //  4. Seleção única
    // ==========================================================
    public static Dialog selecaoUnica(Context ctx,
                                      String titulo,
                                      final String[] opcoes,
                                      int idxAtual,
                                      final OnEscolha onEscolha) {

        final Dialog dialog = novoDialog(ctx);
        LinearLayout card = criarCard(ctx, false);

        LinearLayout header = new LinearLayout(ctx);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(ctx, 18), dp(ctx, 14), dp(ctx, 18), dp(ctx, 8));
        header.addView(criarTitulo(ctx, titulo));
        card.addView(header);

        LinearLayout lista = new LinearLayout(ctx);
        lista.setOrientation(LinearLayout.VERTICAL);
        lista.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < opcoes.length; i++) {
            final int pos = i;
            final String opcao = opcoes[i];
            final boolean selecionado = (i == idxAtual);

            LinearLayout linha = new LinearLayout(ctx);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.CENTER_VERTICAL);
            aplicarRipple(ctx, linha);
            linha.setClickable(true);
            linha.setFocusable(true);
            linha.setPadding(dp(ctx, 18), dp(ctx, 11), dp(ctx, 18), dp(ctx, 11));

            // Bolinha de seleção
            TextView check = new TextView(ctx);
            check.setGravity(Gravity.CENTER);
            check.setTextSize(13);
            check.setText(selecionado ? "●" : "○");
            check.setTextColor(selecionado ? COR_VERDE : COR_BORDA);
            LinearLayout.LayoutParams lpCheck = new LinearLayout.LayoutParams(
                    dp(ctx, 18), dp(ctx, 18));
            lpCheck.rightMargin = dp(ctx, 12);
            check.setLayoutParams(lpCheck);
            linha.addView(check);

            TextView tv = new TextView(ctx);
            tv.setText(opcao);
            tv.setTextSize(13);
            tv.setTextColor(selecionado ? COR_VERDE : COR_TEXTO);
            tv.setTypeface(Typeface.MONOSPACE,
                    selecionado ? Typeface.BOLD : Typeface.NORMAL);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            linha.addView(tv);

            linha.setOnClickListener(v -> {
                dialog.dismiss();
                if (onEscolha != null) onEscolha.onEscolha(pos, opcao);
            });

            lista.addView(linha);
        }

        card.addView(lista);

        View espaco = new View(ctx);
        espaco.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 6)));
        card.addView(espaco);

        return montarDialog(dialog, ctx, card);
    }

    // ==========================================================
    //  5. Mensagem simples (só OK)
    // ==========================================================
    public static Dialog mensagem(Context ctx, String titulo, String mensagem) {
        return confirmar(ctx, titulo, mensagem, "OK", null, null);
    }

    // ==========================================================
    //  Internos — construção de Views
    // ==========================================================

    private static Dialog novoDialog(Context ctx) {
        Dialog d = new Dialog(ctx);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    /**
     * Cria o card do diálogo.
     *
     * @param comPaddingInterno true  → padding em todos os lados (confirmação, input)
     *                          false → padding lateral 0 (listas, pra ripple encostar)
     */
    private static LinearLayout criarCard(Context ctx, boolean comPaddingInterno) {
        LinearLayout card = new LinearLayout(ctx);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(
                ctx.getResources().getIdentifier(
                        "bg_dialogo_card", "drawable", ctx.getPackageName()));

        if (comPaddingInterno) {
            card.setPadding(dp(ctx, 18), dp(ctx, 16), dp(ctx, 18), dp(ctx, 8));
        } else {
            card.setPadding(0, 0, 0, 0);
        }

        card.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return card;
    }

    private static TextView criarTitulo(Context ctx, String texto) {
        TextView tv = new TextView(ctx);
        tv.setText(texto);
        tv.setTextColor(COR_TITULO);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return tv;
    }

    private static TextView criarMensagem(Context ctx, String texto) {
        TextView tv = new TextView(ctx);
        tv.setText(texto);
        tv.setTextColor(COR_SECUNDARIO);
        tv.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        tv.setLineSpacing(dp(ctx, 2), 1f);
        return tv;
    }

    private static View criarLinhaAcao(final Context ctx,
                                        final Dialog dialog,
                                        final ItemAcao item) {
        LinearLayout linha = new LinearLayout(ctx);
        linha.setOrientation(LinearLayout.HORIZONTAL);
        linha.setGravity(Gravity.CENTER_VERTICAL);
        aplicarRipple(ctx, linha);
        linha.setClickable(true);
        linha.setFocusable(true);
        linha.setPadding(dp(ctx, 18), dp(ctx, 12), dp(ctx, 18), dp(ctx, 12));

        if (item.iconeRes != 0) {
            ImageView icone = new ImageView(ctx);
            icone.setImageResource(item.iconeRes);
            int cor = item.cor != 0 ? item.cor : COR_SECUNDARIO;
            icone.setColorFilter(cor);
            LinearLayout.LayoutParams lpIcone = new LinearLayout.LayoutParams(
                    dp(ctx, 18), dp(ctx, 18));
            lpIcone.rightMargin = dp(ctx, 14);
            icone.setLayoutParams(lpIcone);
            linha.addView(icone);
        }

        TextView tv = new TextView(ctx);
        tv.setText(item.texto);
        tv.setTextSize(13);
        tv.setTextColor(item.cor != 0 ? item.cor : COR_TEXTO);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        linha.addView(tv);

        linha.setOnClickListener(v -> {
            dialog.dismiss();
            if (item.acao != null) item.acao.run();
        });

        return linha;
    }

    /** Barra de botões (cancelar + positivo). */
    private static View criarBotoes(final Context ctx,
                                    final Dialog dialog,
                                    String textoPositivo, int corPositivo,
                                    String textoNegativo, int corNegativo,
                                    final Runnable onPositivo,
                                    final Runnable onCancelarExtra) {

        LinearLayout barra = new LinearLayout(ctx);
        barra.setOrientation(LinearLayout.HORIZONTAL);
        barra.setGravity(Gravity.END);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 12);
        barra.setLayoutParams(lp);

        if (textoNegativo != null && !textoNegativo.isEmpty()) {
            barra.addView(criarBotaoTexto(ctx, textoNegativo, corNegativo, () -> {
                dialog.dismiss();
                if (onCancelarExtra != null) onCancelarExtra.run();
            }));
        }

        if (textoPositivo != null && !textoPositivo.isEmpty()) {
            barra.addView(criarBotaoTexto(ctx, textoPositivo, corPositivo, () -> {
                dialog.dismiss();
                if (onPositivo != null) onPositivo.run();
            }));
        }

        return barra;
    }

    private static TextView criarBotaoTexto(Context ctx,
                                            String texto,
                                            int cor,
                                            final Runnable onClick) {
        TextView b = new TextView(ctx);
        b.setText(texto);
        b.setTextColor(cor);
        b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        b.setPadding(dp(ctx, 12), dp(ctx, 8), dp(ctx, 12), dp(ctx, 8));
        b.setBackgroundResource(
                ctx.getResources().getIdentifier(
                        "bg_dialogo_botao", "drawable", ctx.getPackageName()));
        b.setClickable(true);
        b.setFocusable(true);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 2);
        b.setLayoutParams(lp);

        b.setOnClickListener(v -> {
            if (onClick != null) onClick.run();
        });
        return b;
    }

    // ==========================================================
    //  Ripple / attr
    // ==========================================================

    /** Aplica um fundo de toque discreto via selector (sem ripple). */
    private static void aplicarRipple(Context ctx, View v) {
        v.setBackgroundResource(
                ctx.getResources().getIdentifier(
                        "bg_dialogo_item", "drawable", ctx.getPackageName()));
    }

    // ==========================================================
    //  Montagem final do Dialog
    // ==========================================================

    private static Dialog montarDialog(final Dialog dialog,
                                       final Context ctx,
                                       View conteudo) {

        dialog.setContentView(conteudo);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);

        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            WindowManager.LayoutParams lp = w.getAttributes();
            lp.dimAmount = 0.6f;
            w.setAttributes(lp);

            aplicarImersivo(w);
        }

        dialog.setOnDismissListener(d -> {
            if (ctx instanceof Activity) {
                ModoImersivo.aplicar((Activity) ctx);
            }
        });

        dialog.show();

        // Largura do card: 85% da tela, cap 360dp (bem mais compacto)
        if (w != null) {
            int larguraTela = ctx.getResources().getDisplayMetrics().widthPixels;
            int largura = (int) (larguraTela * 0.85f);
            int capDp = dp(ctx, 360);
            if (largura > capDp) largura = capDp;
            w.setLayout(largura, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        return dialog;
    }

    private static void aplicarImersivo(Window w) {
        w.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    private static int dp(Context ctx, int v) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, v,
                ctx.getResources().getDisplayMetrics());
    }
}