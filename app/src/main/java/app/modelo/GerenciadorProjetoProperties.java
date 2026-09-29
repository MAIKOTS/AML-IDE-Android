package modelo;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Carrega e salva o arquivo Projeto.properties.
 */
public final class GerenciadorProjetoProperties {

    private static final String TAG = "ProjetoProperties";
    public  static final String NOME_ARQUIVO = "Projeto.properties";

    private GerenciadorProjetoProperties() { }

    // ==========================================================
    //  Carregar
    // ==========================================================

    public static Projeto carregar(File pastaProjeto) {
        Projeto p = new Projeto();
        p.pastaRaiz = pastaProjeto;

        File arquivo = new File(pastaProjeto, NOME_ARQUIVO);
        if (!arquivo.isFile()) {
            Log.w(TAG, "Projeto.properties não encontrado. Usando padrões.");
            return p;
        }

        Properties props = new Properties();
        try (InputStreamReader isr = new InputStreamReader(
                new FileInputStream(arquivo), StandardCharsets.UTF_8)) {
            props.load(isr);
        } catch (IOException e) {
            Log.e(TAG, "Erro ao ler Projeto.properties", e);
            return p;
        }

        // ---------- Strings ----------
        p.nome      = props.getProperty("nome", p.nome);
        p.versao    = props.getProperty("versao", p.versao);
        p.autor     = props.getProperty("autor", p.autor);
        p.descricao = props.getProperty("descricao", p.descricao);

        // ---------- Alvo (multi-ABI) ----------
        String abiRaw = props.getProperty("abi", "");
        p.abis.clear();
        if (abiRaw == null || abiRaw.trim().isEmpty()) {
            // Se não veio abi=, mantém o default do construtor
            if (p.abis.isEmpty()) p.abis.add("arm64-v8a");
        } else {
            for (String a : abiRaw.split(",")) {
                String abi = Projeto.normalizarAbi(a);
                if (!p.abis.contains(abi)) p.abis.add(abi);
            }
            if (p.abis.isEmpty()) p.abis.add("arm64-v8a");
        }

        p.api = parseInt(props.getProperty("api"), p.api);

        // ---------- Saída ----------
        p.libNome = props.getProperty("lib_nome", p.libNome);

        // ---------- Compilador ----------
        p.std        = props.getProperty("std", p.std);
        p.otimizacao = props.getProperty("otimizacao", p.otimizacao);

        // ---------- Listas ----------
        p.flags    = split(props.getProperty("flags", ""));
        p.fontes   = split(props.getProperty("fontes", ""));
        p.includes = split(props.getProperty("includes", ""));
        p.libs     = split(props.getProperty("libs", ""));

        // ---------- Dependências ----------
        p.dependencias = new ArrayList<>();
        String depsRaw = props.getProperty("dependencias", "");
        if (!depsRaw.isEmpty()) {
            for (String item : depsRaw.split(",")) {
                String s = item.trim();
                if (s.isEmpty()) continue;

                Dependencia d = new Dependencia();

                // Separa URL do branch: "url@branch"
                int idx = s.lastIndexOf('@');
                if (idx > 0 && idx < s.length() - 1) {
                    d.url = s.substring(0, idx);
                    d.branch = s.substring(idx + 1);
                } else {
                    d.url = s;
                    d.branch = null;
                }

                // Extrai o nome do repositório
                String url = d.url;
                while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
                if (url.endsWith(".git")) url = url.substring(0, url.length() - 4);
                int slash = url.lastIndexOf('/');
                d.nomeRepo = (slash >= 0) ? url.substring(slash + 1) : url;

                // Pasta local
                d.pastaLocal = new File(p.pastaRaiz, "deps/" + d.nomeRepo);

                p.dependencias.add(d);
            }
        }

        return p;
    }

    // ==========================================================
    //  Salvar
    // ==========================================================

    public static boolean salvar(Projeto p) {
        if (p == null || p.pastaRaiz == null) return false;

        StringBuilder sb = new StringBuilder();
        sb.append("# ============================================================\n");
        sb.append("#  Projeto.properties\n");
        sb.append("#  Configuração do projeto.\n");
        sb.append("# ============================================================\n\n");

        sb.append("# ---------- Informações ----------\n");
        sb.append("nome=").append(p.nome).append("\n");
        sb.append("versao=").append(p.versao).append("\n");
        sb.append("autor=").append(p.autor).append("\n");
        sb.append("descricao=").append(p.descricao).append("\n\n");

        sb.append("# ---------- Alvo ----------\n");
        sb.append("# abi: arm64-v8a, armeabi-v7a (separadas por vírgula)\n");
        sb.append("# api: 21, 24, 26, 30...\n");
        sb.append("abi=").append(joinAbis(p.abis)).append("\n");
        sb.append("api=").append(p.api).append("\n\n");

        sb.append("# ---------- Saída ----------\n");
        sb.append("# Nome do arquivo .so (sem extensão)\n");
        sb.append("lib_nome=").append(p.libNome).append("\n\n");

        sb.append("# ---------- Compilador ----------\n");
        sb.append("std=").append(p.std).append("\n");
        sb.append("otimizacao=").append(p.otimizacao).append("\n\n");

        sb.append("# ---------- Flags extras ----------\n");
        sb.append("# Separe por vírgula. Ex: -DDEBUG,-DVERBOSE\n");
        sb.append("flags=").append(join(p.flags)).append("\n\n");

        sb.append("# ---------- Fontes ----------\n");
        sb.append("# Deixe vazio para compilar TODOS os .cpp/.c em src/\n");
        sb.append("# Ou especifique: src/main.cpp,src/helper.cpp\n");
        sb.append("fontes=").append(join(p.fontes)).append("\n\n");

        sb.append("# ---------- Includes ----------\n");
        sb.append("# Pastas com headers adicionais\n");
        sb.append("includes=").append(join(p.includes)).append("\n\n");

        sb.append("# ---------- Bibliotecas ----------\n");
        sb.append("# Ex: -llog,-landroid,-lGLESv2\n");
        sb.append("libs=").append(join(p.libs)).append("\n\n");

        // ---------- Dependências ----------
        sb.append("# ---------- Dependências ----------\n");
        sb.append("# Repositórios Git para baixar em deps/\n");
        sb.append("# Formato: URL@branch (branch é opcional)\n");
        sb.append("# Ex: https://github.com/user/lib.git@v1.0\n");
        sb.append("dependencias=").append(joinDependencias(p.dependencias)).append("\n");

        File arquivo = new File(p.pastaRaiz, NOME_ARQUIVO);
        try (OutputStreamWriter osw = new OutputStreamWriter(
                new FileOutputStream(arquivo), StandardCharsets.UTF_8)) {
            osw.write(sb.toString());
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Erro ao salvar Projeto.properties", e);
            return false;
        }
    }

    // ==========================================================
    //  Criar template padrão
    // ==========================================================

    public static void criarPadrao(File pastaProjeto, String nomeProjeto) {
        Projeto p = new Projeto();
        p.pastaRaiz = pastaProjeto;
        p.nome = nomeProjeto;
        p.libNome = "lib" + sanitizarNome(nomeProjeto);

        // Multi-ABI por padrão em projetos novos
        p.abis.clear();
        p.abis.add("arm64-v8a");
        p.abis.add("armeabi-v7a");

        salvar(p);
    }

    private static String sanitizarNome(String s) {
        if (s == null) return "main";
        return s.toLowerCase()
                .replaceAll("[^a-z0-9_]", "_");
    }

    // ==========================================================
    //  Utilitários
    // ==========================================================

    private static List<String> split(String valor) {
        List<String> lista = new ArrayList<>();
        if (valor == null || valor.trim().isEmpty()) return lista;
        for (String s : valor.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) lista.add(t);
        }
        return lista;
    }

    private static String join(List<String> lista) {
        if (lista == null || lista.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(lista.get(i));
        }
        return sb.toString();
    }

    /** Serializa ABIs para o formato "arm64-v8a,armeabi-v7a". */
    private static String joinAbis(List<String> abis) {
        if (abis == null || abis.isEmpty()) return "arm64-v8a";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < abis.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(abis.get(i));
        }
        return sb.toString();
    }

    /** Serializa dependências de volta para o formato "url@branch,url@branch". */
    private static String joinDependencias(List<Dependencia> deps) {
        if (deps == null || deps.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < deps.size(); i++) {
            if (i > 0) sb.append(",");
            Dependencia d = deps.get(i);
            sb.append(d.url);
            if (d.branch != null && !d.branch.isEmpty()) {
                sb.append("@").append(d.branch);
            }
        }
        return sb.toString();
    }

    private static int parseInt(String s, int padrao) {
        if (s == null) return padrao;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return padrao; }
    }
}