package modelo;

import android.os.Handler;
import android.os.Looper;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class GerenciadorDependencias {

    public interface Callback {
        void aoProgresso(int indice, int total, String nome);
        void aoFinalizar(boolean sucesso, String erro);
    }

    private GerenciadorDependencias() { }

    /**
     * Baixa todas as dependências que ainda não foram baixadas.
     * Roda em background thread.
     */
    public static void baixar(Projeto projeto, String token, Callback cb) {
    new Thread(() -> {
        File pastaDeps = new File(projeto.pastaRaiz, "deps");
        if (!pastaDeps.exists()) pastaDeps.mkdirs();

        List<Dependencia> pendentes = new ArrayList<>();
        for (Dependencia d : projeto.dependencias) {
            if (!d.jaBaixada()) pendentes.add(d);
        }

        if (pendentes.isEmpty()) {
            if (cb != null) cb.aoFinalizar(true, null);
            return;
        }

        int total = pendentes.size();
        int i = 0;

        for (Dependencia dep : pendentes) {
            i++;
            if (cb != null) cb.aoProgresso(i, total, dep.nomeRepo);

            try {
                // ---------- CORREÇÃO 1: Remove a barra final da URL ----------
                String urlLimpa = dep.url;
                while (urlLimpa.endsWith("/")) {
                    urlLimpa = urlLimpa.substring(0, urlLimpa.length() - 1);
                }

                File destino = new File(pastaDeps, dep.nomeRepo);

                Git.cloneRepository()
                        .setURI(urlLimpa)
                        .setDirectory(destino)
                        .setBranch(dep.branch)
                        .setCloneAllBranches(dep.branch == null)
                        // ---------- CORREÇÃO 2: Ativa o clone de submódulos ----------
                        .setCloneSubmodules(true)
                        .setCredentialsProvider(
                                (token == null || token.isEmpty())
                                        ? null
                                        : new UsernamePasswordCredentialsProvider("token", token))
                        .call();

            } catch (GitAPIException e) {
                if (cb != null) cb.aoFinalizar(false,
                        "Falha em " + dep.nomeRepo + ": " + e.getMessage());
                return;
            }
        }

        if (cb != null) cb.aoFinalizar(true, null);
    }, "baixar-deps").start();
}
}