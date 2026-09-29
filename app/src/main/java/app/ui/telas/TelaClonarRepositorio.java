package ui.telas;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;

import app.R;

public class TelaClonarRepositorio {

    public interface Acoes {
        void aoVoltar();
        void aoClonarComSucesso(File pastaProjeto);
    }

    /** Pasta raiz onde os clones do GitHub ficam. */
    private static final String PASTA_CLONES = "AML_IDE/Projetos_Clonados_Github";

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    private EditText inputUrl;
    private EditText inputBranch;
    private EditText inputToken;
    private TextView console;

    public TelaClonarRepositorio(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    public View construir() {
        View raiz = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_clonar_repositorio, null, false);

        inputUrl    = raiz.findViewById(R.id.inputUrlRepositorio);
        inputBranch = raiz.findViewById(R.id.inputBranch);
        inputToken  = raiz.findViewById(R.id.inputToken);
        console     = raiz.findViewById(R.id.consoleClone);

        raiz.findViewById(R.id.btnClonarVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });

        raiz.findViewById(R.id.btnIniciarClone).setOnClickListener(v -> iniciarClone());

        return raiz;
    }

    private void iniciarClone() {
        String url = inputUrl.getText().toString().trim();
        String branch = inputBranch.getText().toString().trim();
        String token = inputToken.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(ctx, "Informe a URL do repositório.", Toast.LENGTH_SHORT).show();
            return;
        }

        console.setText("Iniciando clone...\n");

        // ★ Pasta raiz dos clones: /storage/emulated/0/AML_IDE/Projetos_Clonados_Github/
        File pastaRaizClones = new File(
                Environment.getExternalStorageDirectory(),
                PASTA_CLONES);

        if (!pastaRaizClones.exists()) {
            if (!pastaRaizClones.mkdirs()) {
                log("✗ Não foi possível criar a pasta:");
                log(pastaRaizClones.getAbsolutePath());
                log("Verifique se tem permissão de armazenamento.");
                return;
            }
        }

        // Roda em background
        new Thread(() -> {
            String nomeRepo = extrairNomeRepo(url);
            if (nomeRepo.isEmpty()) {
                log("✗ Não foi possível extrair o nome do repositório da URL.");
                return;
            }

            File pastaDestino = new File(pastaRaizClones, nomeRepo);

            if (pastaDestino.exists()) {
                log("⚠ Já existe uma pasta com o nome '" + nomeRepo + "'.");
                log("Caminho: " + pastaDestino.getAbsolutePath());
                log("Renomeie ou apague a pasta existente para clonar novamente.");
                return;
            }

            try {
                log("Repositório: " + nomeRepo);
                log("Destino:     " + pastaDestino.getAbsolutePath());
                log("URL:         " + url);
                if (!branch.isEmpty()) log("Branch:      " + branch);
                log("");
                log("Clonando (isso pode demorar alguns segundos)...\n");

                Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(pastaDestino)
                        .setBranch(branch.isEmpty() ? null : branch)
                        .setCredentialsProvider(
                                token.isEmpty() ? null
                                        : new UsernamePasswordCredentialsProvider("token", token))
                        .call();

                log("✓ Clone concluído com sucesso!");
                log("Pasta: " + pastaDestino.getAbsolutePath());

                handlerUI.post(() -> {
                    if (acoes != null) acoes.aoClonarComSucesso(pastaDestino);
                });

            } catch (GitAPIException e) {
                log("\n✗ Erro ao clonar:");
                log(e.getMessage());

                // Diagnóstico simples
                String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (msg.contains("not authorized") || msg.contains("authentication")) {
                    log("");
                    log("Dica: repositórios privados precisam de");
                    log("um token de acesso (Personal Access Token).");
                } else if (msg.contains("not found")) {
                    log("");
                    log("Dica: verifique se a URL está correta e");
                    log("se o repositório existe / é público.");
                } else if (msg.contains("unable to resolve host")
                        || msg.contains("network")) {
                    log("");
                    log("Dica: verifique sua conexão com a internet.");
                }

                e.printStackTrace();
            }
        }, "thread-clone-git").start();
    }

    /**
     * Extrai o nome do repositório a partir de uma URL do GitHub.
     *
     * Exemplos:
     *   https://github.com/user/meu-repo.git  → meu-repo
     *   https://github.com/user/meu-repo      → meu-repo
     *   git@github.com:user/meu-repo.git      → meu-repo
     */
    private String extrairNomeRepo(String url) {
        if (url == null) return "";
        String s = url.trim();

        // Remove barra final
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);

        // Remove .git
        if (s.endsWith(".git")) s = s.substring(0, s.length() - 4);

        // Pega o último segmento após '/' ou ':'
        int idx = Math.max(s.lastIndexOf('/'), s.lastIndexOf(':'));
        if (idx < 0) return s;
        return s.substring(idx + 1);
    }

    private void log(String mensagem) {
        handlerUI.post(() -> {
            if (console != null) {
                console.append(mensagem + "\n");
            }
        });
    }
}