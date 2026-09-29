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
        TELA_CONFIG_PROJETO
    }

    private final Activity atividade;
    private final FrameLayout conteinerPrincipal;
    private final Stack<EstadoTela> historicoNavegacao;

    private View viewHome;
    private View viewSeletor;
    private View viewEstrutura;
    private View viewEditor;
    private View viewConfig;

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

    /** Define as 4 views principais. */
    public void definirViewsTelas(View home, View seletor, View estrutura, View editor) {
        this.viewHome = home;
        this.viewSeletor = seletor;
        this.viewEstrutura = estrutura;
        this.viewEditor = editor;
    }

    /** Define a view de configs (usada sob demanda). */
    public void definirViewConfig(View config) {
        this.viewConfig = config;
    }

    // ==========================================================
    //  Navegação
    // ==========================================================

    /**
     * Navega para uma tela.
     *
     * Regras:
     *  - TELA_INICIAL → reseta a pilha (back na Home fecha o app)
     *  - Mesma tela atual → ignora
     *  - Tela já existente mais abaixo na pilha → volta pra ela
     *    (evita A → B → A → B → A ... e mantém back coerente)
     *  - Caso contrário → empilha
     */
    public void navegarPara(EstadoTela novaTela) {
        if (novaTela == null) return;

        // ---- HOME: reset total ----
        if (novaTela == EstadoTela.TELA_INICIAL) {
            if (historicoNavegacao.size() == 1
                    && historicoNavegacao.peek() == EstadoTela.TELA_INICIAL) {
                return; // já está na Home
            }
            historicoNavegacao.clear();
            historicoNavegacao.push(EstadoTela.TELA_INICIAL);
            Log.i(TAG, "→ HOME (reset)  prof=" + historicoNavegacao.size());
            exibirTelaAtual();
            return;
        }

        // ---- mesma tela atual: no-op ----
        if (!historicoNavegacao.isEmpty()
                && historicoNavegacao.peek() == novaTela) {
            return;
        }

        // ---- tela já existe abaixo na pilha: volta pra ela ----
        // Ex: [HOME, ESTRUTURA, EDITOR] + navegarPara(ESTRUTURA)
        //     → popa EDITOR → [HOME, ESTRUTURA]
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

        // ---- empilha normal ----
        historicoNavegacao.push(novaTela);
        Log.i(TAG, "→ " + novaTela + "  prof=" + historicoNavegacao.size());
        exibirTelaAtual();
    }

    /**
     * Volta pra tela anterior do histórico.
     *
     * @return true se conseguiu voltar; false se já está na raiz (Home).
     *         Quando retorna false, o chamador deve fechar o app.
     */
    public boolean voltarTelaAnterior() {
        if (historicoNavegacao.size() > 1) {
            historicoNavegacao.pop();
            Log.i(TAG, "← " + historicoNavegacao.peek()
                    + "  prof=" + historicoNavegacao.size());
            exibirTelaAtual();
            return true;
        }
        Log.i(TAG, "← já está na raiz — back vai fechar o app");
        return false;
    }

    /**
     * Volta direto pra uma tela específica, removendo tudo no caminho.
     *
     * Uso típico:
     *   Salvar configs → voltarPara(TELA_ESTRUTURA_PROJETO)
     *
     * Se a tela não existir na pilha, ela vira a raiz (limpando tudo).
     */
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

    /** Limpa tudo e volta pra Home. */
    public void irParaHome() {
        historicoNavegacao.clear();
        historicoNavegacao.push(EstadoTela.TELA_INICIAL);
        Log.i(TAG, "←← HOME (reset)");
        exibirTelaAtual();
    }

    // ==========================================================
    //  Consultas
    // ==========================================================

    public EstadoTela obterTelaAtual() {
        return historicoNavegacao.isEmpty()
                ? EstadoTela.TELA_INICIAL
                : historicoNavegacao.peek();
    }

    /** true se tem pra onde voltar (não está na Home). */
    public boolean podeVoltar() {
        return historicoNavegacao.size() > 1;
    }

    /** Nº de telas na pilha (pra debug). */
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
    }

    private View obterViewDaTela(EstadoTela tela) {
        switch (tela) {
            case TELA_INICIAL:           return viewHome;
            case TELA_SELETOR_PROJETO:   return viewSeletor;
            case TELA_ESTRUTURA_PROJETO: return viewEstrutura;
            case TELA_EDITOR:            return viewEditor;
            case TELA_CONFIG_PROJETO:    return viewConfig;
            default:                     return null;
        }
    }
}