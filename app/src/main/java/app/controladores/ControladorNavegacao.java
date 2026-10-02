package app.controladores;

import android.app.Activity;
import android.view.View;

import java.io.File;

import app.util.ModoImersivo;
import modelo.Projeto;
import ui.navegacao.NavegadorTelas;
import ui.telas.FerramentasDaIDE;
import ui.telas.MontadorDeEditorComAbas;
import ui.telas.MontadorDeEstrutura;
import ui.telas.MontadorDeHome;
import ui.telas.MontadorDeSeletorDeProjetos;
import ui.telas.TelaClonarRepositorio;
import ui.telas.TelaConfigProjeto;
import ui.telas.TelaExploradorFiles;
import ui.telas.TelaGerenciadorProjetos;
import ui.telas.TelaNovoProjeto;
import ui.telas.TelaRecursos;

/**
 * Abre TODAS as telas navegáveis (não-dialogs) e gerencia a
 * navegação entre elas.
 *
 * Não abre drawers — isso é responsabilidade do ControladorPaineis.
 */
public class ControladorNavegacao {

    public interface Host {
        MontadorDeHome      montadorHome();
        MontadorDeSeletorDeProjetos montadorSeletor();
        MontadorDeEstrutura montadorEstrutura();
        MontadorDeEditorComAbas montadorEditor();
        void toast(String msg);
    }

    private final Activity     atividade;
    private final NavegadorTelas navegador;
    private final Host         host;

    public ControladorNavegacao(Activity atividade,
                                NavegadorTelas navegador,
                                Host host) {
        this.atividade = atividade;
        this.navegador = navegador;
        this.host = host;
    }

    // ==========================================================
    //  Helper interno
    // ==========================================================

    private void voltarOuHome() {
        if (!navegador.voltarTelaAnterior()) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        }
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Novo Projeto
    // ==========================================================

    public void abrirNovoProjeto() {
        TelaNovoProjeto tela = new TelaNovoProjeto(atividade,
                new TelaNovoProjeto.Acoes() {
                    @Override
                    public void aoVoltar() {
                        voltarOuHome();
                    }

                    @Override
                    public void aoCriarProjeto(File pastaProjeto) {
                        // Navega direto pra estrutura do projeto novo
                        abrirProjetoNaEstrutura(pastaProjeto, "Projeto criado");
                    }
                });

        navegador.definirViewNovoProjeto(tela.construir());
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_NOVO_PROJETO);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Explorador de Files
    // ==========================================================

    public void abrirExploradorFiles() {
        File pastaFiles = atividade.getFilesDir();

        TelaExploradorFiles tela = new TelaExploradorFiles(atividade,
                new TelaExploradorFiles.Acoes() {
                    @Override
                    public void aoVoltar() { voltarOuHome(); }

                    @Override
                    public void aoRecarregar() { }

                    @Override
                    public void aoEditarArquivo(File arquivo) {
                        host.montadorEditor().abrirArquivo(arquivo);
                        navegador.navegarPara(
                                NavegadorTelas.EstadoTela.TELA_EDITOR);
                        ModoImersivo.aplicar(atividade);
                    }
                });

        navegador.definirViewExplorador(tela.construir(pastaFiles));
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_EXPLORADOR);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Ferramentas da IDE
    // ==========================================================

    public void abrirFerramentasIde() {
        FerramentasDaIDE tela = new FerramentasDaIDE(atividade,
                new FerramentasDaIDE.Acoes() {
                    @Override
                    public void aoVoltar() { voltarOuHome(); }

                    @Override
                    public void aoAbrirExploradorFiles() {
                        abrirExploradorFiles();
                    }

                    @Override
                    public void aoAbrirRecursos() {
                        abrirRecursos();
                    }
                });

        navegador.definirViewFerramentasIde(tela.construir());
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_FERRAMENTAS_IDE);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Recursos
    // ==========================================================

    public void abrirRecursos() {
        TelaRecursos tela = new TelaRecursos(atividade,
                new TelaRecursos.Acoes() {
                    @Override
                    public void aoVoltar() { voltarOuHome(); }
                });

        navegador.definirViewRecursos(tela.construir());
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_RECURSOS);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Gerenciador de Projetos
    // ==========================================================

    public void abrirGerenciadorProjetos() {
        TelaGerenciadorProjetos tela = new TelaGerenciadorProjetos(atividade,
                new TelaGerenciadorProjetos.Acoes() {
                    @Override
                    public void aoVoltar() { voltarOuHome(); }

                    @Override
                    public void aoAbrirProjeto(File pasta) {
                        abrirProjetoNaEstrutura(pasta, "Projeto aberto");
                    }
                });

        navegador.definirViewGerenciadorProjetos(tela.construir());
        navegador.navegarPara(
                NavegadorTelas.EstadoTela.TELA_GERENCIADOR_PROJETOS);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Clonar Repositório
    // ==========================================================

    public void abrirClonarRepositorio() {
        TelaClonarRepositorio tela = new TelaClonarRepositorio(atividade,
                new TelaClonarRepositorio.Acoes() {
                    @Override
                    public void aoVoltar() { voltarOuHome(); }

                    @Override
                    public void aoClonarComSucesso(File pastaProjeto) {
                        abrirProjetoNaEstrutura(pastaProjeto,
                                "Repositório clonado");
                    }
                });

        navegador.definirViewClonarRepositorio(tela.construir());
        navegador.navegarPara(
                NavegadorTelas.EstadoTela.TELA_CLONAR_REPOSITORIO);
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Estrutura de Projeto
    // ==========================================================

    public void abrirProjetoNaEstrutura(File pasta, String rotulo) {
        if (pasta == null || !pasta.exists()) {
            host.toast("Projeto não encontrado.");
            return;
        }

        View layoutEstrutura = host.montadorEstrutura()
                .construirLayout(pasta, pasta.getName());

        navegador.definirViewsTelas(
                host.montadorHome().construirLayout(),
                host.montadorSeletor().construirLayout(),
                layoutEstrutura,
                host.montadorEditor().construirLayout()
        );

        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_ESTRUTURA_PROJETO);
        ModoImersivo.aplicar(atividade);
        host.toast(rotulo + ": " + pasta.getName());
    }

    // ==========================================================
    //  Configuração de Projeto
    // ==========================================================

    public void abrirConfigProjeto(File pastaProjeto) {
        if (pastaProjeto == null || !pastaProjeto.exists()) {
            host.toast("Projeto não encontrado.");
            return;
        }

        TelaConfigProjeto tela = new TelaConfigProjeto(atividade, pastaProjeto,
                new TelaConfigProjeto.Acoes() {
                    @Override
                    public void aoSalvar(Projeto projeto) {
                        host.toast("Configurações salvas!");
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
}