package ui.componentes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import ui.navegacao.NavegadorTelas;
import app.R;

public class MenuLateralApp extends PainelLateralDeslizante {

    public interface AcoesMenu {
        void aoNavegar(NavegadorTelas.EstadoTela tela);
        /** Nova ação: usuário quer abrir a tela de complementos. */
        void aoAbrirComplementos();
    }

    public MenuLateralApp(Context contexto,
                          View conteudoPrincipal,
                          NavegadorTelas navegador,
                          AcoesMenu acoes) {
        super(contexto,
              conteudoPrincipal,
              inflarConteudo(contexto, acoes));
    }

    private static LinearLayout inflarConteudo(Context ctx, AcoesMenu acoes) {
        LinearLayout menu = (LinearLayout) LayoutInflater
                .from(ctx)
                .inflate(R.layout.view_menu_lateral, null, false);

        menu.findViewById(R.id.menu_item_home).setOnClickListener(
                v -> acoes.aoNavegar(NavegadorTelas.EstadoTela.TELA_INICIAL));

        menu.findViewById(R.id.menu_item_seletor).setOnClickListener(
                v -> acoes.aoNavegar(NavegadorTelas.EstadoTela.TELA_SELETOR_PROJETO));

        menu.findViewById(R.id.menu_item_estrutura).setOnClickListener(
                v -> acoes.aoNavegar(NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO));

        menu.findViewById(R.id.menu_item_editor).setOnClickListener(
                v -> acoes.aoNavegar(NavegadorTelas.EstadoTela.TELA_EDITOR));

        menu.findViewById(R.id.menu_item_complementos).setOnClickListener(
                v -> acoes.aoAbrirComplementos());

        return menu;
    }
}