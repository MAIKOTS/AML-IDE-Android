package modelo;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um projeto — carrega dados de Projeto.properties.
 *
 * Todos os campos são públicos para facilitar leitura/escrita.
 *
 * MULTI-ABI: o campo `abis` é uma lista. Ex: ["arm64-v8a", "armeabi-v7a"].
 */
public class Projeto {

    // Caminho da pasta raiz do projeto
    public File pastaRaiz;

    // ---------- Informações ----------
    public String nome      = "MeuMod";
    public String versao    = "1.0.0";
    public String autor     = "Desconhecido";
    public String descricao = "";

    // ---------- Alvo ----------
    // Multi-ABI: lista de ABIs a compilar
    public List<String> abis = new ArrayList<>();
    public int          api  = 21;

    // ---------- Saída ----------
    public String libNome = "libmain";   // sem extensão .so

    // ---------- Compilador ----------
    public String std         = "c++17";
    public String otimizacao  = "O2";

    // ---------- Listas ----------
    public List<String> flags    = new ArrayList<>();
    public List<String> fontes   = new ArrayList<>();
    public List<String> includes = new ArrayList<>();
    public List<String> libs     = new ArrayList<>();

    // ---------- Dependências ----------
    public List<Dependencia> dependencias = new ArrayList<>();

    // ==========================================================
    //  Construtor
    // ==========================================================

    public Projeto() {
        // Fallback: se nenhum .properties definir abi=,
        // assume apenas arm64-v8a (mantém comportamento antigo).
        abis.add("arm64-v8a");
    }

    // ==========================================================
    //  Helpers
    // ==========================================================

    /**
     * Retorna a lista de arquivos-fonte a compilar.
     *
     * - Se `fontes` estiver preenchido no .properties → usa essa lista.
     * - Se estiver vazio → varre `src/` recursivamente.
     */
    public List<File> resolverFontes() {
        List<File> resultado = new ArrayList<>();

        if (!fontes.isEmpty()) {
            for (String caminho : fontes) {
                File f = new File(pastaRaiz, caminho);
                if (f.isFile()) resultado.add(f);
            }
            return resultado;
        }

        File pastaSrc = new File(pastaRaiz, "src");
        if (pastaSrc.isDirectory()) {
            coletarFontes(pastaSrc, resultado);
        }
        return resultado;
    }

    /** Retorna todas as pastas de include (projeto + deps). */
    public List<String> obterPastasIncludes() {
        List<String> pastas = new ArrayList<>();

        // includes do próprio projeto
        for (String inc : includes) {
            pastas.add(new File(pastaRaiz, inc).getAbsolutePath());
        }

        // includes das dependências
        for (Dependencia dep : dependencias) {
            File pastaInc = new File(dep.pastaLocal, "include");
            if (pastaInc.isDirectory()) {
                pastas.add(pastaInc.getAbsolutePath());
            }
        }

        return pastas;
    }

    /** Retorna todas as pastas de libs (deps) para uma ABI específica. */
    public List<String> obterPastasLibs(String abi) {
        List<String> pastas = new ArrayList<>();
        String abiPasta = normalizarAbi(abi);

        for (Dependencia dep : dependencias) {
            File pastaLib = new File(dep.pastaLocal, "lib/" + abiPasta);
            if (pastaLib.isDirectory()) {
                pastas.add(pastaLib.getAbsolutePath());
            }
        }

        return pastas;
    }

    private void coletarFontes(File pasta, List<File> resultado) {
        File[] filhos = pasta.listFiles();
        if (filhos == null) return;

        for (File f : filhos) {
            if (f.isDirectory()) {
                coletarFontes(f, resultado);
            } else {
                String n = f.getName().toLowerCase();
                if (n.endsWith(".cpp") || n.endsWith(".c")
                        || n.endsWith(".cc") || n.endsWith(".cxx")) {
                    resultado.add(f);
                }
            }
        }
    }

    // ==========================================================
    //  Saída (.so)
    // ==========================================================

    /**
     * Caminho final do .so gerado para uma ABI.
     *
     * Estrutura:
     *   pastaRaiz/
     *   └── lib/
     *       ├── arm64-v8a/
     *       │   └── libnome.so
     *       └── armeabi-v7a/
     *           └── libnome.so
     */
    public File obterArquivoSaida(String abi) {
        String abiPasta = normalizarAbi(abi);

        File pastaLib = new File(pastaRaiz, "lib");
        File pastaAbi = new File(pastaLib, abiPasta);

        if (!pastaAbi.exists()) pastaAbi.mkdirs();

        return new File(pastaAbi, libNome + ".so");
    }

    /** Fallback: usa a primeira ABI da lista. */
    public File obterArquivoSaida() {
        String abi = (abis == null || abis.isEmpty()) ? "arm64-v8a" : abis.get(0);
        return obterArquivoSaida(abi);
    }

    // ==========================================================
    //  Utilidades
    // ==========================================================

    /**
     * Normaliza a ABI para o nome de pasta oficial.
     *
     * Aceita variações e converte para o padrão:
     *   arm64, arm64-v8a, aarch64        → arm64-v8a
     *   arm, arm32, armeabi, armeabi-v7a → armeabi-v7a
     *   x86                              → x86
     *   x86_64, x64                      → x86_64
     */
    public static String normalizarAbi(String abi) {
        if (abi == null) return "arm64-v8a";

        String a = abi.toLowerCase().trim();

        if (a.contains("arm64") || a.contains("aarch64")) return "arm64-v8a";
        if (a.contains("armeabi") || a.contains("armv7") || a.equals("arm")) return "armeabi-v7a";
        if (a.contains("x86_64") || a.contains("x64")) return "x86_64";
        if (a.contains("x86")) return "x86";

        return "arm64-v8a"; // fallback
    }
}