package ui.componentes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import ui.dialogos.DialogoApp;
import app.R;
import app.util.Preferencias;

/**
 * Segundo menu lateral — CONFIGURAÇÕES.
 *
 * Abre pela DIREITA. Estende PainelLateralDireita.
 */
public class MenuLateralConfiguracoes extends PainelLateralDireita {

    public interface AcoesConfig {
        void aoFechar();
        void aoAlterarTamanhoFonte(int novoTamanho);
        void aoAlternarWordWrap(boolean ativo);
        void aoAlternarNumerosLinha(boolean ativo);
        void aoAlternarConsoleAuto(boolean ativo);
        void aoAlternarSalvarAoSair(boolean ativo);
        void aoClicarSobre();
        void aoClicarGithub();
        void aoClicarReportar();
    }

    private static final int[] TAMANHOS_FONTE = { 12, 14, 16, 18, 20 };

    private final TextView valorTamanhoFonte;

    public MenuLateralConfiguracoes(Context ctx,
                                    View conteudoPrincipal,
                                    final AcoesConfig acoes) {
        super(ctx, conteudoPrincipal, inflarMenu(ctx));

        LinearLayout menu = obterMenuLateral();

        // ---- Bind ----
        ImageButton btnFechar            = menu.findViewById(R.id.configBtnFechar);
        valorTamanhoFonte                = menu.findViewById(R.id.configValorTamanhoFonte);
        Switch swWordWrap                = menu.findViewById(R.id.configSwitchWordWrap);
        Switch swNumerosLinha            = menu.findViewById(R.id.configSwitchNumerosLinha);
        Switch swConsoleAuto             = menu.findViewById(R.id.configSwitchConsoleAuto);
        Switch swSalvarAoSair            = menu.findViewById(R.id.configSwitchSalvarAoSair);

        View itemTamanhoFonte = menu.findViewById(R.id.configItemTamanhoFonte);
        View itemSobre        = menu.findViewById(R.id.configItemSobre);
        View itemGithub       = menu.findViewById(R.id.configItemGithub);
        View itemReportar     = menu.findViewById(R.id.configItemReportar);

        // ---- Estado inicial ----
        valorTamanhoFonte.setText(Preferencias.getTamanhoFonte(ctx) + "sp");
        swWordWrap.setChecked(Preferencias.isWordWrap(ctx));
        swNumerosLinha.setChecked(Preferencias.isNumerosLinha(ctx));
        swConsoleAuto.setChecked(Preferencias.isConsoleAutoExpandir(ctx));
        swSalvarAoSair.setChecked(Preferencias.isSalvarAoSair(ctx));

        // ---- Listeners ----
        btnFechar.setOnClickListener(v -> {
            if (acoes != null) acoes.aoFechar();
        });

        itemTamanhoFonte.setOnClickListener(v -> dialogoTamanhoFonte(ctx, acoes));

        swWordWrap.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setWordWrap(ctx, checked);
            if (acoes != null) acoes.aoAlternarWordWrap(checked);
        });

        swNumerosLinha.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setNumerosLinha(ctx, checked);
            if (acoes != null) acoes.aoAlternarNumerosLinha(checked);
        });

        swConsoleAuto.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setConsoleAutoExpandir(ctx, checked);
            if (acoes != null) acoes.aoAlternarConsoleAuto(checked);
        });

        swSalvarAoSair.setOnCheckedChangeListener((b, checked) -> {
            Preferencias.setSalvarAoSair(ctx, checked);
            if (acoes != null) acoes.aoAlternarSalvarAoSair(checked);
        });

        itemSobre.setOnClickListener(v -> {
            if (acoes != null) acoes.aoClicarSobre();
        });

        itemGithub.setOnClickListener(v -> {
            if (acoes != null) acoes.aoClicarGithub();
        });

        itemReportar.setOnClickListener(v -> {
            if (acoes != null) acoes.aoClicarReportar();
        });
    }

    // ==========================================================
    //  Diálogo de tamanho de fonte
    // ==========================================================

    private void dialogoTamanhoFonte(Context ctx, final AcoesConfig acoes) {
    final String[] rotulos = new String[TAMANHOS_FONTE.length];
    int atual = Preferencias.getTamanhoFonte(ctx);
    int idxAtual = 0;

    for (int i = 0; i < TAMANHOS_FONTE.length; i++) {
        rotulos[i] = TAMANHOS_FONTE[i] + "sp"
                + (TAMANHOS_FONTE[i] == 14 ? "  (padrão)" : "");
        if (TAMANHOS_FONTE[i] == atual) idxAtual = i;
    }

    DialogoApp.selecaoUnica(ctx, "Tamanho da fonte", rotulos, idxAtual,
            (posicao, valor) -> {
                int novo = TAMANHOS_FONTE[posicao];
                Preferencias.setTamanhoFonte(ctx, novo);
                valorTamanhoFonte.setText(novo + "sp");
                if (acoes != null) acoes.aoAlterarTamanhoFonte(novo);
            });
}

    // ==========================================================
    //  Helper
    // ==========================================================

    private static LinearLayout inflarMenu(Context ctx) {
        return (LinearLayout) LayoutInflater.from(ctx)
                .inflate(R.layout.view_configuracoes, null, false);
    }
}