package app.controladores;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.io.File;

import ui.telas.TelaClonarRepositorio;
import ui.telas.TelaComplementos;
import ui.telas.TelaExploradorFiles;
import ui.telas.TelaGerenciadorProjetos;

/**
 * Abre os dialogs em tela cheia:
 *  - Complementos
 *  - Gerenciador de Projetos
 *  - Clonar Repositório
 *  - Explorador de Files
 */
public class ControladorDialogos {

    public interface Acoes {
        /** Usuário escolheu um projeto (no gerenciador ou após clonar). */
        void aoAbrirProjetoNaEstrutura(File pasta, String rotulo);
    }

    private final Activity atividade;
    private final Acoes acoes;

    public ControladorDialogos(Activity atividade, Acoes acoes) {
        this.atividade = atividade;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Complementos
    // ==========================================================

    public void abrirComplementos() {
        final Dialog[] ref = new Dialog[1];

        TelaComplementos tela = new TelaComplementos(atividade,
                new TelaComplementos.Acoes() {
                    @Override
                    public void aoVoltar() {
                        dismiss(ref);
                    }

                    @Override
                    public void aoAbrirExploradorFiles() {
                        dismiss(ref);
                        abrirExploradorFiles();
                    }
                });

        mostrar(tela.construir(), ref);
    }

    // ==========================================================
    //  Gerenciador de Projetos
    // ==========================================================

    public void abrirGerenciadorProjetos() {
        final Dialog[] ref = new Dialog[1];

        TelaGerenciadorProjetos tela = new TelaGerenciadorProjetos(atividade,
                new TelaGerenciadorProjetos.Acoes() {
                    @Override
                    public void aoVoltar() {
                        dismiss(ref);
                    }

                    @Override
                    public void aoAbrirProjeto(File pasta) {
                        dismiss(ref);
                        if (acoes != null) {
                            acoes.aoAbrirProjetoNaEstrutura(pasta, "Projeto aberto");
                        }
                    }
                });

        mostrar(tela.construir(), ref);
    }

    // ==========================================================
    //  Clonar Repositório
    // ==========================================================

    public void abrirClonarRepositorio() {
        final Dialog[] ref = new Dialog[1];

        TelaClonarRepositorio tela = new TelaClonarRepositorio(atividade,
                new TelaClonarRepositorio.Acoes() {
                    @Override
                    public void aoVoltar() {
                        dismiss(ref);
                    }

                    @Override
                    public void aoClonarComSucesso(File pastaProjeto) {
                        dismiss(ref);
                        if (acoes != null) {
                            acoes.aoAbrirProjetoNaEstrutura(pastaProjeto,
                                    "Repositório clonado");
                        }
                    }
                });

        mostrar(tela.construir(), ref);
    }

    // ==========================================================
    //  Explorador de Files
    // ==========================================================

    public void abrirExploradorFiles() {
        File pastaFiles = atividade.getFilesDir();
        final Dialog[] ref = new Dialog[1];

        TelaExploradorFiles tela = new TelaExploradorFiles(atividade,
                new TelaExploradorFiles.Acoes() {
                    @Override
                    public void aoVoltar() {
                        dismiss(ref);
                    }

                    @Override
                    public void aoRecarregar() {
                        // nada
                    }
                });

        mostrar(tela.construir(pastaFiles), ref);
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    private void mostrar(View view, Dialog[] ref) {
        Dialog dialog = new Dialog(atividade);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(view);
        dialog.setCancelable(true);
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }

        ref[0] = dialog;
    }

    private void dismiss(Dialog[] ref) {
        if (ref[0] != null) ref[0].dismiss();
    }
}