package app.controladores;

import android.app.Activity;
import android.view.View;

import ui.componentes.MenuLateralApp;
import ui.componentes.MenuLateralConfiguracoes;
import ui.navegacao.NavegadorTelas;

/**
 * Cria e gerencia os 2 drawers laterais:
 *  - ESQUERDO: menu de navegação (sempre disponível)
 *  - DIREITO : configurações (só na Home)
 */
public class ControladorPaineis {

    public interface Host {
        // Navegação do menu esquerdo
        void aoNavegarPeloMenu(NavegadorTelas.EstadoTela tela);
        void aoAbrirFerramentasIdePeloMenu();

        // Config (só na Home)
        void aoAlterarTamanhoFonte(int novoTamanho);
        void aoAlternarWordWrap(boolean ativo);
        void aoAlternarNumerosLinha(boolean ativo);
        void aoAlternarConsoleAuto(boolean ativo);
        void aoAlternarSalvarAoSair(boolean ativo);
        void aoClicarSobre();
        void aoClicarGithub();
        void aoClicarReportar();
    }

    private final Activity atividade;
    private final NavegadorTelas navegador;
    private final Host host;

    private MenuLateralApp painelComMenu;
    private MenuLateralConfiguracoes painelConfig;

    public ControladorPaineis(Activity atividade,
                              NavegadorTelas navegador,
                              Host host) {
        this.atividade = atividade;
        this.navegador = navegador;
        this.host = host;
    }

    // ==========================================================
    //  Inicialização
    // ==========================================================

    public void inicializar() {

        // ---------- Drawer DIREITO (config) ----------
        painelConfig = new MenuLateralConfiguracoes(
                atividade,
                navegador.obterConteinerPrincipal(),
                new MenuLateralConfiguracoes.AcoesConfig() {
                    @Override
                    public void aoFechar() {
                        painelConfig.fecharMenu();
                    }

                    @Override
                    public void aoAlterarTamanhoFonte(int n) {
                        host.aoAlterarTamanhoFonte(n);
                    }

                    @Override
                    public void aoAlternarWordWrap(boolean a) {
                        host.aoAlternarWordWrap(a);
                    }

                    @Override
                    public void aoAlternarNumerosLinha(boolean a) {
                        host.aoAlternarNumerosLinha(a);
                    }

                    @Override
                    public void aoAlternarConsoleAuto(boolean a) {
                        host.aoAlternarConsoleAuto(a);
                    }

                    @Override
                    public void aoAlternarSalvarAoSair(boolean a) {
                        host.aoAlternarSalvarAoSair(a);
                    }

                    @Override
                    public void aoClicarSobre() {
                        host.aoClicarSobre();
                    }

                    @Override
                    public void aoClicarGithub() {
                        host.aoClicarGithub();
                    }

                    @Override
                    public void aoClicarReportar() {
                        host.aoClicarReportar();
                    }
                });

        // Config começa habilitado (estamos na Home)
        painelConfig.setHabilitado(true);

        // ---------- Drawer ESQUERDO (menu principal) ----------
        painelComMenu = new MenuLateralApp(
                atividade,
                painelConfig,
                navegador,
                new MenuLateralApp.AcoesMenu() {
                    @Override
                    public void aoNavegar(NavegadorTelas.EstadoTela tela) {
                        host.aoNavegarPeloMenu(tela);
                    }

                    @Override
                    public void aoAbrirFerramentasIde() {
                        painelComMenu.fecharMenu();
                        host.aoAbrirFerramentasIdePeloMenu();
                    }
                });
    }

    // ==========================================================
    //  Acesso
    // ==========================================================

    public View obterPainelRaiz() {
        return painelComMenu;
    }

    // ==========================================================
    //  Abrir/fechar
    // ==========================================================

    public void abrirEsquerdo() {
        if (painelConfig != null && painelConfig.isMenuAberto()) {
            painelConfig.fecharMenu();
        }
        if (painelComMenu != null) painelComMenu.abrirMenu();
    }

    public void abrirDireito() {
        if (painelComMenu != null && painelComMenu.isMenuAberto()) {
            painelComMenu.fecharMenu();
        }
        if (painelConfig != null) painelConfig.abrirMenu();
    }

    public boolean isEsquerdoAberto() {
        return painelComMenu != null && painelComMenu.isMenuAberto();
    }

    public boolean isDireitoAberto() {
        return painelConfig != null && painelConfig.isMenuAberto();
    }

    public void fecharEsquerdo() {
        if (painelComMenu != null) painelComMenu.fecharMenu();
    }

    public void fecharDireito() {
        if (painelConfig != null) painelConfig.fecharMenu();
    }

    /** Habilita/desabilita o menu direito (só funciona na Home). */
    public void setHabilitadoDireito(boolean habilitado) {
        if (painelConfig != null) {
            painelConfig.setHabilitado(habilitado);
        }
    }
}