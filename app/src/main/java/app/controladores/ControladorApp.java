package app.controladores;

import android.app.Activity;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import app.util.ModoImersivo;
import modelo.Projeto;
import ui.componentes.MenuLateralApp;
import ui.navegacao.NavegadorTelas;
import ui.telas.MontadorDeEditorComAbas;
import ui.telas.MontadorDeEstrutura;
import ui.telas.MontadorDeHome;
import ui.telas.MontadorDeSeletorDeProjetos;
import ui.telas.TelaConfigProjeto;

/**
 * Controlador principal do app.
 *
 * Responsável por:
 *  - Criar o NavegadorTelas e os montadores
 *  - Orquestrar a navegação entre telas
 *  - Delegar dialogs para o ControladorDialogos
 *
 * A MainActivity só cria este controlador e delega os eventos
 * do ciclo de vida (back, focus).
 */
public class ControladorApp {

    private final Activity atividade;
    private final NavegadorTelas navegador;
    private final ControladorDialogos dialogos;

    private MenuLateralApp painelComMenu;

    private MontadorDeHome montadorHome;
    private MontadorDeSeletorDeProjetos montadorSeletor;
    private MontadorDeEstrutura montadorEstrutura;
    private MontadorDeEditorComAbas montadorEditor;

    public ControladorApp(Activity atividade) {
        this.atividade = atividade;
        this.navegador = new NavegadorTelas(atividade);
        this.dialogos = new ControladorDialogos(atividade,
                new ControladorDialogos.Acoes() {
                    @Override
                    public void aoAbrirProjetoNaEstrutura(File pasta, String rotulo) {
                        abrirProjetoNaEstrutura(pasta, rotulo);
                    }
                });

        inicializarMontadores();
        criarPainelComMenu();
    }

    /** Retorna a view-raiz pra colocar no setContentView(). */
    public View construirViewPrincipal() {
        return painelComMenu;
    }

    // ==========================================================
    //  Montadores e views
    // ==========================================================

    private void inicializarMontadores() {

        // ---- Home ----
        montadorHome = new MontadorDeHome(atividade,
                new MontadorDeHome.AcoesHome() {
                    @Override
                    public void aoCliqueMenuHamburguer() {
                        painelComMenu.abrirMenu();
                    }

                    @Override
                    public void aoCliqueConfiguracoes() {
                        ModoImersivo.aplicar(atividade);
                    }

                    @Override
                    public void aoCliqueCriarNovoProjeto() {
                        dialogos.abrirGerenciadorProjetos();
                    }

                    @Override
                    public void aoCliqueImportarProjeto() {
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_SELETOR_PROJETO);
                    }

                    @Override
                    public void aoCliqueClonarGithub() {
                        dialogos.abrirClonarRepositorio();
                    }

                    @Override
                    public void aoCliqueProjetoRecente(File pasta) {
                        if (pasta == null || !pasta.exists()) {
                            toast("Projeto não encontrado.");
                            return;
                        }
                        abrirProjetoNaEstrutura(pasta, "Projeto carregado");
                    }
                });

        // ---- Seletor ----
        montadorSeletor = new MontadorDeSeletorDeProjetos(atividade,
                new MontadorDeSeletorDeProjetos.AcoesSeletor() {
                    @Override
                    public void aoSelecionarPastaProjeto(File pasta) {
                        abrirProjetoNaEstrutura(pasta, "Projeto carregado");
                    }

                    @Override
                    public void aoCancelar() {
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_INICIAL);
                    }
                });

        // ---- Editor ----
        montadorEditor = new MontadorDeEditorComAbas(atividade,
                new MontadorDeEditorComAbas.AcoesEditor() {
                    @Override
                    public void aoFecharEditor() {
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO);
                        ModoImersivo.aplicar(atividade);
                    }

                    @Override
                    public void aoAbrirConfigs(File pastaProjeto) {
                        abrirConfigProjeto(pastaProjeto);
                    }
                });

        // ---- Estrutura ----
        montadorEstrutura = new MontadorDeEstrutura(atividade,
                new MontadorDeEstrutura.AcoesEstrutura() {
                    @Override
                    public void aoSelecionarArquivo(File arquivo) {
                        montadorEditor.abrirArquivo(arquivo);
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_EDITOR);
                        ModoImersivo.aplicar(atividade);
                    }

                    @Override
                    public void aoVoltar() {
                        if (!navegador.voltarTelaAnterior()) {
                            navegador.navegarPara(
                                    NavegadorTelas.EstadoTela.TELA_INICIAL);
                        }
                        ModoImersivo.aplicar(atividade);
                    }
                });

        // Registra views no navegador
        navegador.definirViewsTelas(
                montadorHome.construirLayout(),
                montadorSeletor.construirLayout(),
                montadorEstrutura.construirLayout(null, null),
                montadorEditor.construirLayout()
        );
    }

    // ==========================================================
    //  Menu lateral
    // ==========================================================

    private void criarPainelComMenu() {
        painelComMenu = new MenuLateralApp(
                atividade,
                navegador.obterConteinerPrincipal(),
                navegador,
                new MenuLateralApp.AcoesMenu() {
                    @Override
                    public void aoNavegar(NavegadorTelas.EstadoTela tela) {
                        if (tela == NavegadorTelas.EstadoTela.TELA_INICIAL
                                && montadorHome != null) {
                            montadorHome.atualizarRecentes();
                        }
                        navegador.navegarPara(tela);
                        painelComMenu.fecharMenu();
                    }

                    @Override
                    public void aoAbrirComplementos() {
                        painelComMenu.fecharMenu();
                        dialogos.abrirComplementos();
                    }
                });

        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
    }

    // ==========================================================
    //  Ações de navegação
    // ==========================================================

    private void abrirProjetoNaEstrutura(File pasta, String rotulo) {
        if (pasta == null || !pasta.exists()) {
            toast("Projeto não encontrado.");
            return;
        }

        View layoutEstrutura = montadorEstrutura.construirLayout(
                pasta, pasta.getName());

        navegador.definirViewsTelas(
                montadorHome.construirLayout(),
                montadorSeletor.construirLayout(),
                layoutEstrutura,
                montadorEditor.construirLayout()
        );

        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO);
        ModoImersivo.aplicar(atividade);
        toast(rotulo + ": " + pasta.getName());
    }

    private void abrirConfigProjeto(File pastaProjeto) {
        if (pastaProjeto == null || !pastaProjeto.exists()) {
            toast("Projeto não encontrado.");
            return;
        }

        TelaConfigProjeto tela = new TelaConfigProjeto(atividade, pastaProjeto,
                new TelaConfigProjeto.Acoes() {
                    @Override
                    public void aoSalvar(Projeto projeto) {
                        toast("Configurações salvas!");
                        if (!navegador.voltarTelaAnterior()) {
                            navegador.navegarPara(
                                    NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO);
                        }
                        ModoImersivo.aplicar(atividade);
                    }

                    @Override
                    public void aoCancelar() {
                        if (!navegador.voltarTelaAnterior()) {
                            navegador.navegarPara(
                                    NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO);
                        }
                        ModoImersivo.aplicar(atividade);
                    }
                });

        navegador.definirViewConfig(tela.construir());
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_CONFIG_PROJETO);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Ciclo de vida (chamado pela MainActivity)
    // ==========================================================

    /**
     * Trata o botão "voltar" do sistema.
     *
     * @return true se consumiu o evento; false se o app pode fechar.
     */
    public boolean aoVoltar() {
        // Menu lateral aberto? Fecha primeiro.
        if (painelComMenu != null && painelComMenu.isMenuAberto()) {
            painelComMenu.fecharMenu();
            ModoImersivo.aplicar(atividade);
            return true;
        }

        // Ainda tem pra onde voltar?
        if (navegador.voltarTelaAnterior()) {
            if (navegador.obterTelaAtual() == NavegadorTelas.EstadoTela.TELA_INICIAL
                    && montadorHome != null) {
                montadorHome.atualizarRecentes();
            }
            ModoImersivo.aplicar(atividade);
            return true;
        }

        // Na Home: deixa o sistema fechar o app
        return false;
    }

    public void aoGanharFoco() {
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Utilitário
    // ==========================================================

    private void toast(String msg) {
        Toast.makeText(atividade, msg, Toast.LENGTH_SHORT).show();
    }
}