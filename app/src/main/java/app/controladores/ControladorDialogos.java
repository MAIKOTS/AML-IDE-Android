package app.controladores;

import java.io.File;

/**
 * Notificador de ações que antes eram dialogs.
 *
 * Depois da refatoração, gerenciador de projetos, clonar repositório,
 * ferramentas da IDE e explorador são TELAS navegáveis.
 *
 * Esta classe apenas repassa as intenções do usuário pro ControladorApp.
 */
public class ControladorDialogos {

    public interface Acoes {
        void aoAbrirProjetoNaEstrutura(File pasta, String rotulo);
        void aoEditarArquivoNoEditor(File arquivo);
        void aoAbrirExploradorFiles();
        void aoAbrirFerramentasIde();
        void aoAbrirGerenciadorProjetos();   // ← NOVO
        void aoAbrirClonarRepositorio();     // ← NOVO
    }

    private final Acoes acoes;

    public ControladorDialogos(Acoes acoes) {
        this.acoes = acoes;
    }

    public void abrirGerenciadorProjetos() {
        if (acoes != null) acoes.aoAbrirGerenciadorProjetos();
    }

    public void abrirClonarRepositorio() {
        if (acoes != null) acoes.aoAbrirClonarRepositorio();
    }

    public void abrirFerramentasIde() {
        if (acoes != null) acoes.aoAbrirFerramentasIde();
    }
}