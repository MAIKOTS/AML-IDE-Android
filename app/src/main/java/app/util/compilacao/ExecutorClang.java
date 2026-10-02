package app.util.compilacao;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import ui.util.GerenciadorLogsConsole;

/**
 * Executa o binário do clang via ProcessBuilder e devolve o resultado.
 * É o ÚNICO lugar que chama Process.
 */
public final class ExecutorClang {

    public static class Resultado {
        public int     codigoSaida;
        public String  saida = "";
        public long    duracaoMs;
    }

    private ExecutorClang() { }

    public static Resultado executar(List<String> cmd,
                                     String nativeDir,
                                     File libsExtras,
                                     GerenciadorLogsConsole log) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);

        // PATH
        String pathAtual = pb.environment().get("PATH");
        pb.environment().put("PATH",
                nativeDir + ":" + (pathAtual != null ? pathAtual : "/system/bin"));

        // LD_LIBRARY_PATH
        String ldPath = nativeDir;
        if (libsExtras != null) {
            ldPath = libsExtras.getAbsolutePath() + ":" + ldPath;
        }
        pb.environment().put("LD_LIBRARY_PATH", ldPath);

        if (log != null) {
            log.debug("Env", "PATH            = " + pb.environment().get("PATH"));
            log.debug("Env", "LD_LIBRARY_PATH = " + pb.environment().get("LD_LIBRARY_PATH"));
        }

        // Executa
        if (log != null) log.info("Exec", "Executando clang...");
        long inicio = System.currentTimeMillis();

        Process processo = pb.start();

        StringBuilder saida = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(processo.getInputStream()))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                saida.append(linha).append("\n");
            }
        }

        int codigo = processo.waitFor();
        long duracao = System.currentTimeMillis() - inicio;

        // Resultado
        Resultado r = new Resultado();
        r.codigoSaida = codigo;
        r.saida       = saida.toString();
        r.duracaoMs   = duracao;

        if (log != null) {
            log.info("Exec", "Exit code: " + codigo
                    + "  •  Duração: " + GerenciadorLogsConsole.formatarDuracao(duracao));
            if (r.saida.length() > 0) {
                log.info("Saída", "--- saída do clang/ld ("
                        + r.saida.length() + " chars) ---");
                log.saidaCrua(r.saida);
                log.info("Saída", "--- fim da saída ---");
            } else {
                log.debug("Saída", "(vazia)");
            }
        }

        return r;
    }
}