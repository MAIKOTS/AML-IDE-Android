package app.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Preferências do usuário (persistidas em SharedPreferences).
 *
 * Uso:
 *   Preferencias.setTamanhoFonte(ctx, 16);
 *   int t = Preferencias.getTamanhoFonte(ctx);
 */
public final class Preferencias {

    private static final String ARQUIVO = "aml_ide_prefs";

    // ---- Chaves ----
    private static final String K_TAMANHO_FONTE      = "tamanho_fonte";
    private static final String K_WORD_WRAP          = "word_wrap";
    private static final String K_NUMEROS_LINHA      = "numeros_linha";
    private static final String K_CONSOLE_AUTO       = "console_auto_expandir";
    private static final String K_SALVAR_AO_SAIR     = "salvar_ao_sair";

    // ---- Padrões ----
    public static final int    PADRAO_TAMANHO_FONTE   = 14;
    public static final boolean PADRAO_WORD_WRAP      = false;
    public static final boolean PADRAO_NUMEROS_LINHA  = true;
    public static final boolean PADRAO_CONSOLE_AUTO   = true;
    public static final boolean PADRAO_SALVAR_AO_SAIR = true;

    private Preferencias() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    // ==========================================================
    //  Editor
    // ==========================================================

    public static int getTamanhoFonte(Context ctx) {
        return prefs(ctx).getInt(K_TAMANHO_FONTE, PADRAO_TAMANHO_FONTE);
    }

    public static void setTamanhoFonte(Context ctx, int sp) {
        prefs(ctx).edit().putInt(K_TAMANHO_FONTE, sp).apply();
    }

    public static boolean isWordWrap(Context ctx) {
        return prefs(ctx).getBoolean(K_WORD_WRAP, PADRAO_WORD_WRAP);
    }

    public static void setWordWrap(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(K_WORD_WRAP, v).apply();
    }

    public static boolean isNumerosLinha(Context ctx) {
        return prefs(ctx).getBoolean(K_NUMEROS_LINHA, PADRAO_NUMEROS_LINHA);
    }

    public static void setNumerosLinha(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(K_NUMEROS_LINHA, v).apply();
    }

    // ==========================================================
    //  Console
    // ==========================================================

    public static boolean isConsoleAutoExpandir(Context ctx) {
        return prefs(ctx).getBoolean(K_CONSOLE_AUTO, PADRAO_CONSOLE_AUTO);
    }

    public static void setConsoleAutoExpandir(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(K_CONSOLE_AUTO, v).apply();
    }

    // ==========================================================
    //  Comportamento
    // ==========================================================

    public static boolean isSalvarAoSair(Context ctx) {
        return prefs(ctx).getBoolean(K_SALVAR_AO_SAIR, PADRAO_SALVAR_AO_SAIR);
    }

    public static void setSalvarAoSair(Context ctx, boolean v) {
        prefs(ctx).edit().putBoolean(K_SALVAR_AO_SAIR, v).apply();
    }
}