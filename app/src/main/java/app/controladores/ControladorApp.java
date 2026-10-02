package app.controladores;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import java.io.File;

import app.util.ModoImersivo;
import ui.navegacao.NavegadorTelas;
import ui.telas.MontadorDeEditorComAbas;
import ui.telas.MontadorDeEstrutura;
import ui.telas.MontadorDeHome;
import ui.telas.MontadorDeSeletorDeProjetos;

/**
 * Orquestrador principal do app.
 *
 * Delega responsabilidades para:
 *  - ControladorNavegacao  → abrir telas navegáveis
 *  - ControladorMontadores → criar/gerenciar montadores
 *  - ControladorPaineis    → criar/gerenciar drawers
 *  - ControladorDialogos   → notificações pontuais
 *
 * Aqui só fica:
 *  - Ciclo de vida (onBackPressed, onWindowFocusChanged)
 *  - Roteamento dos callbacks dos 3 sub-controladores
 *  - Utilitários (toast, abrir URL, mostrar Sobre)
 */
public class ControladorApp
        implements ControladorNavegacao.Host,
                   ControladorMontadores.Host,
                   ControladorPaineis.Host {

    private static final String URL_GITHUB = "https://github.com/MaikoTS";

    private final Activity atividade;
    private final NavegadorTelas navegador;

    private final ControladorDialogos   dialogos;
    private final ControladorMontadores montadores;
    private final ControladorNavegacao  navegacao;
    private final ControladorPaineis    paineis;

    // ==========================================================
    //  Construtor
    // ==========================================================

    public ControladorApp(Activity atividade) {
        this.atividade = atividade;
        this.navegador = new NavegadorTelas(atividade);

        // 1. Sub-controladores
        this.montadores = new ControladorMontadores(atividade, navegador, this);
        this.navegacao  = new ControladorNavegacao (atividade, navegador, this);
        this.paineis    = new ControladorPaineis    (atividade, navegador, this);

        // 2. Dialogos (só notificam)
        this.dialogos = new ControladorDialogos(new ControladorDialogos.Acoes() {
            @Override
            public void aoAbrirProjetoNaEstrutura(File pasta, String rotulo) {
                navegacao.abrirProjetoNaEstrutura(pasta, rotulo);
            }

            @Override
            public void aoEditarArquivoNoEditor(File arquivo) {
                montadores.editor().abrirArquivo(arquivo);
                navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_EDITOR);
                ModoImersivo.aplicar(atividade);
            }

            @Override
            public void aoAbrirExploradorFiles() {
                navegacao.abrirExploradorFiles();
            }

            @Override
            public void aoAbrirFerramentasIde() {
                navegacao.abrirFerramentasIde();
            }

            @Override
            public void aoAbrirGerenciadorProjetos() {
                navegacao.abrirGerenciadorProjetos();
            }

            @Override
            public void aoAbrirClonarRepositorio() {
                navegacao.abrirClonarRepositorio();
            }
        });

        // 3. Ordem de inicialização
        montadores.inicializar();
        paineis.inicializar();
        montadores.registrarViewsIniciais();

        // 4. Listener de troca de tela (pra habilitar/desabilitar menu direito)
        navegador.setOnTelaMudouListener(this::aoMudarTela);

        // 5. Tela inicial
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
    }

    public View construirViewPrincipal() {
        return paineis.obterPainelRaiz();
    }

    // ==========================================================
    //  Ciclo de vida
    // ==========================================================

    public boolean aoVoltar() {
        // 1. Drawer direito (config) aberto? Fecha.
        if (paineis.isDireitoAberto()) {
            paineis.fecharDireito();
            ModoImersivo.aplicar(atividade);
            return true;
        }

        // 2. Drawer esquerdo (menu) aberto? Fecha.
        if (paineis.isEsquerdoAberto()) {
            paineis.fecharEsquerdo();
            ModoImersivo.aplicar(atividade);
            return true;
        }

        // 3. Está na Home? Deixa o Android fechar o app.
        if (navegador.obterTelaAtual() == NavegadorTelas.EstadoTela.TELA_INICIAL) {
            return false;
        }

        // 4. Não é Home: volta uma tela.
        navegador.voltarTelaAnterior();

        if (navegador.obterTelaAtual() == NavegadorTelas.EstadoTela.TELA_INICIAL) {
            montadores.home().atualizarRecentes();
        }
        ModoImersivo.aplicar(atividade);
        return true;
    }

    public void aoGanharFoco() {
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Reação a mudanças de tela
    // ==========================================================

    private void aoMudarTela(NavegadorTelas.EstadoTela novaTela) {
        boolean ehHome = (novaTela == NavegadorTelas.EstadoTela.TELA_INICIAL);
        paineis.setHabilitadoDireito(ehHome);
    }

    // ==========================================================
    //  Host: ControladorMontadores
    // ==========================================================

    @Override
    public void aoAbrirMenuEsquerdo() {
        paineis.abrirEsquerdo();
    }

    @Override
    public void aoAbrirMenuDireito() {
        // Só abre se estiver na Home
        if (navegador.obterTelaAtual() != NavegadorTelas.EstadoTela.TELA_INICIAL) {
            return;
        }
        paineis.abrirDireito();
    }

    /** ★ NOVO — abre a tela de formulário "Novo Projeto" */
    @Override
    public void aoCliqueCriarNovoProjeto() {
        navegacao.abrirNovoProjeto();
    }

    @Override
    public void aoAbrirGerenciadorProjetos() {
        navegacao.abrirGerenciadorProjetos();
    }

    @Override
    public void aoAbrirClonarRepositorio() {
        navegacao.abrirClonarRepositorio();
    }

    @Override
    public void aoImportarProjeto() {
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_SELETOR_PROJETO);
    }

    @Override
    public void aoAbrirProjetoRecente(File pasta) {
        if (pasta == null || !pasta.exists()) {
            toast("Projeto não encontrado.");
            return;
        }
        navegacao.abrirProjetoNaEstrutura(pasta, "Projeto carregado");
    }

    @Override
    public void aoFecharEditor() {
        // Respeita a pilha:
        //  - Veio da ESTRUTURA → volta pra ela
        //  - Veio do menu → volta pra HOME
        //  - Sem pilha → HOME por padrão
        if (!navegador.voltarTelaAnterior()) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        }
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoAbrirConfigsProjeto(File pastaProjeto) {
        navegacao.abrirConfigProjeto(pastaProjeto);
    }

    @Override
    public void aoSelecionarArquivo(File arquivo) {
        montadores.editor().abrirArquivo(arquivo);
        navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_EDITOR);
        ModoImersivo.aplicar(atividade);
    }

    @Override
    public void aoVoltarDaEstrutura() {
        if (!navegador.voltarTelaAnterior()) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        }
        ModoImersivo.aplicar(atividade);
    }

    // ==========================================================
    //  Host: ControladorPaineis
    // ==========================================================

    @Override
    public void aoNavegarPeloMenu(NavegadorTelas.EstadoTela tela) {
        // Se não é Home, reseta a pilha antes (volta pra HOME "limpa")
        // Assim o botão Voltar da nova tela sempre volta pra Home.
        if (tela != NavegadorTelas.EstadoTela.TELA_INICIAL) {
            navegador.navegarPara(NavegadorTelas.EstadoTela.TELA_INICIAL);
        } else {
            montadores.home().atualizarRecentes();
        }
        navegador.navegarPara(tela);
        paineis.fecharEsquerdo();
    }

    @Override
    public void aoAbrirFerramentasIdePeloMenu() {
        navegacao.abrirFerramentasIde();
    }

    @Override
    public void aoAlterarTamanhoFonte(int novoTamanho) {
        toast("Fonte: " + novoTamanho + "sp");
    }

    @Override public void aoAlternarWordWrap(boolean ativo)         { }
    @Override public void aoAlternarNumerosLinha(boolean ativo)     { }
    @Override public void aoAlternarConsoleAuto(boolean ativo)      { }
    @Override public void aoAlternarSalvarAoSair(boolean ativo)     { }

    @Override
    public void aoClicarSobre() {
        mostrarDialogoSobre();
    }

    @Override
    public void aoClicarGithub() {
        abrirUrl(URL_GITHUB);
    }

    @Override
    public void aoClicarReportar() {
        abrirUrl(URL_GITHUB + "/issues");
    }

    // ==========================================================
    //  Host: ControladorNavegacao
    // ==========================================================

    @Override
    public MontadorDeHome montadorHome() {
        return montadores.home();
    }

    @Override
    public MontadorDeSeletorDeProjetos montadorSeletor() {
        return montadores.seletor();
    }

    @Override
    public MontadorDeEstrutura montadorEstrutura() {
        return montadores.estrutura();
    }

    @Override
    public MontadorDeEditorComAbas montadorEditor() {
        return montadores.editor();
    }

    @Override
    public void toast(String msg) {
        Toast.makeText(atividade, msg, Toast.LENGTH_SHORT).show();
    }

    // ==========================================================
    //  Utilitários
    // ==========================================================

    private void mostrarDialogoSobre() {
        ui.dialogos.DialogoApp.confirmar(atividade,
                "AML IDE",
                "Versão: 1.00\n\n" +
                "IDE mobile para criação de mods AML\n" +
                "(AndroidModLoader) para GTA San Andreas.\n\n" +
                "Compilador Clang 14 • NDK r24\n" +
                "Suporte multi-ABI (ARM64 + ARM32)\n\n" +
                "Desenvolvido por MaikoTS",
                "OK", null, null);
    }

    private void abrirUrl(String url) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            atividade.startActivity(i);
        } catch (Exception e) {
            toast("Não foi possível abrir o navegador.");
        }
    }
}