package ui.navegacao;

import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.Stack;

public class NavegadorTelas {

    private static final String TAG = "NavegadorTelas";

    public enum EstadoTela {
        TELA_INICIAL,
        TELA_SELETOR_PROJETO,
        TELA_ESTRUTURA_PROJETO,
        TELA_EDITOR,
        TELA_CONFIG_PROJETO,
        TELA_EXPLORADOR,
        TELA_FERRAMENTAS_IDE,
        TELA_RECURSOS,
        TELA_GERENCIADOR_PROJETOS,
        TELA_CLONAR_REPOSITORIO,
        TELA_NOVO_PROJETO
    }

    /** ★ NOVO — notifica cada troca de tela. */
    public interface OnTelaMudouListener {
        void aoMudar(EstadoTela novaTela);
    }

    private final Activity atividade;
    private final FrameLayout conteinerPrincipal;
    private final Stack<EstadoTela> historicoNavegacao;

    private View viewHome;
    private View viewSeletor;
    private View viewEstrutura;
    private View viewEditor;
    private View viewConfig;
    private View viewExplorador;
    private View viewFerramentasIde;
    private View viewRecursos;
    private View viewGerenciadorProjetos;
    private View viewClonarRepositorio;
    private View viewNovoProjeto;

    private OnTelaMudouListener onTelaMudouListener;   // ← NOVO

    public NavegadorTelas(Activity atividade) {
        this.atividade = atividade;
        this.conteinerPrincipal = new FrameLayout(atividade);
        this.conteinerPrincipal.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        this.historicoNavegacao = new Stack<>();
    }

    public FrameLayout obterConteinerPrincipal() {
        return conteinerPrincipal;
    }

    /** ★ NOVO */
    public void setOnTelaMudouListener(OnTelaMudouListener l) {
        this.onTelaMudouListener = l;
    }

    public void definirViewsTelas(View home, View seletor, View estrutura, View editor) {
        this.viewHome = home;
        this.viewSeletor = seletor;
        this.viewEstrutura = estrutura;
        this.viewEditor = editor;
    }

    public void definirViewConfig(View config) {
        this.viewConfig = config;
    }

    public void definirViewExplorador(View explorador) {
        this.viewExplorador = explorador;
    }

    public void definirViewFerramentasIde(View v) {
        this.viewFerramentasIde = v;
    }

    public void definirViewRecursos(View v) {
        this.viewRecursos = v;
    }

    public void definirViewGerenciadorProjetos(View v) {
        this.viewGerenciadorProjetos = v;
    }

    public void definirViewClonarRepositorio(View v) {
        this.viewClonarRepositorio = v;
    }
    
    public void definirViewNovoProjeto(View v) {   // ← NOVO
        this.viewNovoProjeto = v;
    }

    // ==========================================================
    //  Navegação
    // ==========================================================

    public void navegarPara(EstadoTela novaTela) {
        if (novaTela == null) return;

        if (novaTela == EstadoTela.TELA_INICIAL) {
            if (historicoNavegacao.size() == 1
                    && historicoNavegacao.peek() == EstadoTela.TELA_INICIAL) {
                return;
            }
            historicoNavegacao.clear();
            historicoNavegacao.push(EstadoTela.TELA_INICIAL);
            Log.i(TAG, "→ HOME (reset)  prof=" + historicoNavegacao.size());
            exibirTelaAtual();
            return;
        }

        if (!historicoNavegacao.isEmpty()
                && historicoNavegacao.peek() == novaTela) {
            return;
        }

        if (historicoNavegacao.contains(novaTela)) {
            while (!historicoNavegacao.isEmpty()
                    && historicoNavegacao.peek() != novaTela) {
                historicoNavegacao.pop();
            }
            Log.i(TAG, "→ " + novaTela + " (pop-to)  prof="
                    + historicoNavegacao.size());
            exibirTelaAtual();
            return;
        }

        historicoNavegacao.push(novaTela);
        Log.i(TAG, "→ " + novaTela + "  prof=" + historicoNavegacao.size());
        exibirTelaAtual();
    }

    public boolean voltarTelaAnterior() {
        if (historicoNavegacao.size() > 1) {
            historicoNavegacao.pop();
            Log.i(TAG, "← " + historicoNavegacao.peek()
                    + "  prof=" + historicoNavegacao.size());
            exibirTelaAtual();
            return true;
        }
        Log.i(TAG, "← já está na raiz");
        return false;
    }

    public void voltarPara(EstadoTela telaAlvo) {
        if (telaAlvo == null) return;

        while (!historicoNavegacao.isEmpty()
                && historicoNavegacao.peek() != telaAlvo) {
            historicoNavegacao.pop();
        }

        if (historicoNavegacao.isEmpty()) {
            historicoNavegacao.push(telaAlvo);
        }

        Log.i(TAG, "←← " + telaAlvo + "  prof=" + historicoNavegacao.size());
        exibirTelaAtual();
    }

    public void irParaHome() {
        historicoNavegacao.clear();
        historicoNavegacao.push(EstadoTela.TELA_INICIAL);
        Log.i(TAG, "←← HOME (reset)");
        exibirTelaAtual();
    }

    public EstadoTela obterTelaAtual() {
        return historicoNavegacao.isEmpty()
                ? EstadoTela.TELA_INICIAL
                : historicoNavegacao.peek();
    }

    public boolean podeVoltar() {
        return historicoNavegacao.size() > 1;
    }

    public int profundidadeHistorico() {
        return historicoNavegacao.size();
    }

    // ==========================================================
    //  Renderização
    // ==========================================================

    private void exibirTelaAtual() {
        conteinerPrincipal.removeAllViews();

        if (historicoNavegacao.isEmpty()) {
            historicoNavegacao.push(EstadoTela.TELA_INICIAL);
        }

        EstadoTela telaAtual = historicoNavegacao.peek();
        View alvo = obterViewDaTela(telaAtual);

        if (alvo != null) {
            conteinerPrincipal.addView(alvo);
        } else {
            Log.w(TAG, "⚠ View nula para " + telaAtual);
        }

        // ★ Notifica o listener
        if (onTelaMudouListener != null) {
            onTelaMudouListener.aoMudar(telaAtual);
        }
    }

    private View obterViewDaTela(EstadoTela tela) {
        switch (tela) {
            case TELA_INICIAL:              return viewHome;
            case TELA_SELETOR_PROJETO:      return viewSeletor;
            case TELA_ESTRUTURA_PROJETO:    return viewEstrutura;
            case TELA_EDITOR:               return viewEditor;
            case TELA_CONFIG_PROJETO:       return viewConfig;
            case TELA_EXPLORADOR:           return viewExplorador;
            case TELA_FERRAMENTAS_IDE:      return viewFerramentasIde;
            case TELA_RECURSOS:             return viewRecursos;
            case TELA_GERENCIADOR_PROJETOS: return viewGerenciadorProjetos;
            case TELA_CLONAR_REPOSITORIO:   return viewClonarRepositorio;
            case TELA_NOVO_PROJETO:         return viewNovoProjeto;
            default:                        return null;
        }
    }
}