package app.controladores;

import android.app.Activity;

import java.io.File;

import ui.navegacao.NavegadorTelas;
import ui.telas.MontadorDeEditorComAbas;
import ui.telas.MontadorDeEstrutura;
import ui.telas.MontadorDeHome;
import ui.telas.MontadorDeSeletorDeProjetos;

/**
 * Cria e mantém os 4 montadores principais:
 *  - Home
 *  - Seletor de Projetos
 *  - Estrutura
 *  - Editor
 *
 * Os callbacks de cada montador chamam o Host (ControladorApp),
 * que delega para os controladores certos.
 */
public class ControladorMontadores {

    public interface Host {
        // Ações do menu/Home
        void aoAbrirMenuEsquerdo();
        void aoAbrirMenuDireito();
        void aoAbrirGerenciadorProjetos();
        void aoAbrirClonarRepositorio();
        void aoCliqueCriarNovoProjeto();
        void aoImportarProjeto();     // navega para seletor
        void aoAbrirProjetoRecente(File pasta);

        // Ações do editor
        void aoFecharEditor();
        void aoAbrirConfigsProjeto(File pastaProjeto);

        // Ações da estrutura
        void aoSelecionarArquivo(File arquivo);
        void aoVoltarDaEstrutura();

        // Utilitário
        void toast(String msg);
    }

    private final Activity atividade;
    private final NavegadorTelas navegador;
    private final Host host;

    private MontadorDeHome              montadorHome;
    private MontadorDeSeletorDeProjetos montadorSeletor;
    private MontadorDeEstrutura         montadorEstrutura;
    private MontadorDeEditorComAbas     montadorEditor;

    public ControladorMontadores(Activity atividade,
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

        montadorHome = new MontadorDeHome(atividade,
                new MontadorDeHome.AcoesHome() {
                    @Override
                    public void aoCliqueMenuHamburguer() {
                        host.aoAbrirMenuEsquerdo();
                    }

                    @Override
                    public void aoCliqueConfiguracoes() {
                        host.aoAbrirMenuDireito();
                    }

                    @Override
                    public void aoCliqueCriarNovoProjeto() {
                        host.aoCliqueCriarNovoProjeto();   // ✅ CORRETO
                    }

                    @Override
                    public void aoCliqueImportarProjeto() {
                        host.aoImportarProjeto();
                    }

                    @Override
                    public void aoCliqueClonarGithub() {
                        host.aoAbrirClonarRepositorio();
                    }

                    @Override
                    public void aoCliqueProjetoRecente(File pasta) {
                        host.aoAbrirProjetoRecente(pasta);
                    }
                });

        montadorSeletor = new MontadorDeSeletorDeProjetos(atividade,
                new MontadorDeSeletorDeProjetos.AcoesSeletor() {
                    @Override
                    public void aoSelecionarPastaProjeto(File pasta) {
                        host.aoAbrirProjetoRecente(pasta);
                    }

                    @Override
                    public void aoCancelar() {
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_INICIAL);
                    }
                });

        montadorEditor = new MontadorDeEditorComAbas(atividade,
                new MontadorDeEditorComAbas.AcoesEditor() {
                    @Override
                    public void aoFecharEditor() {
                        host.aoFecharEditor();
                    }

                    @Override
                    public void aoAbrirConfigs(File pastaProjeto) {
                        host.aoAbrirConfigsProjeto(pastaProjeto);
                    }
                });

        montadorEstrutura = new MontadorDeEstrutura(atividade,
                new MontadorDeEstrutura.AcoesEstrutura() {
                    @Override
                    public void aoSelecionarArquivo(File arquivo) {
                        host.aoSelecionarArquivo(arquivo);
                    }

                    @Override
                    public void aoVoltar() {
                        host.aoVoltarDaEstrutura();
                    }

                    @Override
                    public void aoAbrirConfigs(File pastaProjeto) {
                        host.aoAbrirConfigsProjeto(pastaProjeto);
                    }
                });
    }

    // ==========================================================
    //  Registro no navegador
    // ==========================================================

    /** Registra as views iniciais no navegador. */
    public void registrarViewsIniciais() {
        navegador.definirViewsTelas(
                montadorHome.construirLayout(),
                montadorSeletor.construirLayout(),
                montadorEstrutura.construirLayout(null, null),
                montadorEditor.construirLayout()
        );
    }

    // ==========================================================
    //  Getters
    // ==========================================================

    public MontadorDeHome home() { return montadorHome; }
    public MontadorDeSeletorDeProjetos seletor() { return montadorSeletor; }
    public MontadorDeEstrutura estrutura() { return montadorEstrutura; }
    public MontadorDeEditorComAbas editor() { return montadorEditor; }
}