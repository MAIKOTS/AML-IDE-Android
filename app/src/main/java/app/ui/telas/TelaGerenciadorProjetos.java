package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import app.R;
import modelo.GerenciadorProjetoProperties;
import modelo.Projeto;
import app.util.GerenciadorDeArquivos;

/**
 * Tela de gerenciamento de projetos.
 *
 * Fica fixa em /storage/emulated/0/AML_IDE/Projetos/
 *
 * Operações:
 *  - Criar novo projeto (com Projeto.properties e main.cpp inicial)
 *  - Abrir projeto (click simples)
 *  - Renomear, copiar, recortar, colar, deletar (menu 3 pontinhos)
 */
public class TelaGerenciadorProjetos {

    public interface Acoes {
        void aoVoltar();
        void aoAbrirProjeto(File pasta);
    }

    // ==========================================================
    //  Clipboard ESTÁTICO (persiste entre aberturas da tela)
    // ==========================================================
    private static File clipboardArquivo = null;
    private static boolean clipboardEhRecorte = false;

    private final Context ctx;
    private final Acoes acoes;
    private final File pastaProjetos;

    private LinearLayout lista;
    private LinearLayout emptyState;
    private LinearLayout barraClipboard;
    private TextView clipboardTexto;
    private TextView caminhoTexto;

    public TelaGerenciadorProjetos(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
        this.pastaProjetos = new File(
                Environment.getExternalStorageDirectory(),
                "AML_IDE/Projetos");

        // Garante que a pasta existe
        if (!pastaProjetos.exists()) {
            pastaProjetos.mkdirs();
        }
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construir() {
        View raiz = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_gerenciador_projetos, null, false);

        lista           = raiz.findViewById(R.id.projLista);
        emptyState      = raiz.findViewById(R.id.projEmptyState);
        barraClipboard  = raiz.findViewById(R.id.projBarraClipboard);
        clipboardTexto  = raiz.findViewById(R.id.projClipboardTexto);
        caminhoTexto    = raiz.findViewById(R.id.projCaminho);

        caminhoTexto.setText(pastaProjetos.getAbsolutePath());

        // Voltar
        raiz.findViewById(R.id.projBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });

        // Novo projeto
        raiz.findViewById(R.id.projBtnNovo).setOnClickListener(v -> dialogoNovoProjeto());
        raiz.findViewById(R.id.projBtnCriarPrimeiro).setOnClickListener(v -> dialogoNovoProjeto());

        // Colar
        raiz.findViewById(R.id.projBtnColar).setOnClickListener(v -> colarClipboard());

        // Cancelar clipboard
        raiz.findViewById(R.id.projBtnCancelarClipboard).setOnClickListener(v -> {
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
        });

        atualizarBarraClipboard();
        recarregarLista();
        return raiz;
    }

    // ==========================================================
    //  Lista de projetos
    // ==========================================================

    private void recarregarLista() {
        lista.removeAllViews();

        File[] projetos = pastaProjetos.listFiles();
        if (projetos == null) projetos = new File[0];

        // Filtra só pastas e ignora ocultos
        List<File> validos = new ArrayList<>();
        for (File f : projetos) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                validos.add(f);
            }
        }

        // Ordena alfabético
        validos.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        // Empty state
        if (validos.isEmpty()) {
            lista.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        lista.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);

        LayoutInflater inflater = LayoutInflater.from(ctx);
        for (File projeto : validos) {
            lista.addView(criarItemProjeto(inflater, projeto));
        }
    }

    private View criarItemProjeto(LayoutInflater inflater, File projeto) {
        View item = inflater.inflate(R.layout.item_projeto, lista, false);

        TextView nome = item.findViewById(R.id.itemProjetoNome);
        TextView detalhe = item.findViewById(R.id.itemProjetoDetalhe);
        TextView menu = item.findViewById(R.id.itemProjetoMenu);

        nome.setText(projeto.getName());
        detalhe.setText(descreverProjeto(projeto));

        // Click simples → abrir projeto
        item.setOnClickListener(v -> {
            if (acoes != null) acoes.aoAbrirProjeto(projeto);
        });

        // Long press → menu também
        item.setOnLongClickListener(v -> {
            menuContexto(projeto);
            return true;
        });

        // Click no ⋮ → menu
        menu.setOnClickListener(v -> menuContexto(projeto));

        return item;
    }

    /**
     * Descreve o projeto lendo o Projeto.properties se existir.
     *
     * Formato:
     *   "v1.0.0 • arm64-v8a • libmeumod • 3 fontes"
     * ou
     *   "3 fontes" (sem properties)
     * ou
     *   "vazio"
     */
    private String descreverProjeto(File pasta) {
    StringBuilder sb = new StringBuilder();

    File propFile = new File(pasta, GerenciadorProjetoProperties.NOME_ARQUIVO);
    if (propFile.isFile()) {
        try {
            Projeto p = GerenciadorProjetoProperties.carregar(pasta);
            if (p.versao != null && !p.versao.isEmpty()) {
                sb.append("v").append(p.versao);
            }
            if (p.abis != null && !p.abis.isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(String.join(",", p.abis));   // <- aqui
            }
            if (p.libNome != null && !p.libNome.isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(p.libNome);
            }
            if (p.dependencias != null && !p.dependencias.isEmpty()) {
                if (sb.length() > 0) sb.append(" • ");
                sb.append(p.dependencias.size()).append(" dep");
                if (p.dependencias.size() != 1) sb.append("s");
            }
        } catch (Exception ignored) { }
    }

    int fontes = contarFontes(pasta);
    if (fontes > 0) {
        if (sb.length() > 0) sb.append(" • ");
        sb.append(fontes).append(" fonte").append(fontes != 1 ? "s" : "");
    }

    if (sb.length() == 0) return "vazio";
    return sb.toString();
}

    private int contarFontes(File pasta) {
        File src = new File(pasta, "src");
        if (!src.isDirectory()) return 0;

        File[] filhos = src.listFiles();
        if (filhos == null) return 0;

        int total = 0;
        for (File f : filhos) {
            String n = f.getName().toLowerCase();
            if (n.endsWith(".cpp") || n.endsWith(".c")
                    || n.endsWith(".cc") || n.endsWith(".cxx")) {
                total++;
            }
        }
        return total;
    }

    // ==========================================================
    //  Menu de contexto
    // ==========================================================

    private void menuContexto(File projeto) {
        String[] opcoes = {
                "✏  Renomear",
                "📋  Copiar",
                "✂  Recortar",
                "📦  Baixar dependências",
                "🗑  Deletar"
        };

        new AlertDialog.Builder(ctx)
                .setTitle(projeto.getName())
                .setItems(opcoes, (d, w) -> {
                    switch (w) {
                        case 0: dialogoRenomear(projeto); break;
                        case 1: copiarParaClipboard(projeto, false); break;
                        case 2: copiarParaClipboard(projeto, true); break;
                        case 3: baixarDependencias(projeto); break;
                        case 4: confirmarDeletar(projeto); break;
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================================
    //  Novo projeto
    // ==========================================================

    private void dialogoNovoProjeto() {
        EditText campo = new EditText(ctx);
        campo.setHint("nome-do-projeto");
        campo.setPadding(dp(20), dp(20), dp(20), dp(20));
        campo.setSingleLine(true);

        new AlertDialog.Builder(ctx)
                .setTitle("Novo Projeto")
                .setMessage("Criar em: " + pastaProjetos.getAbsolutePath())
                .setView(campo)
                .setPositiveButton("Criar", (d, w) -> {
                    String nome = campo.getText().toString().trim();
                    if (nome.isEmpty()) return;

                    // Sanitiza o nome (evita caracteres problemáticos)
                    nome = sanitizarNome(nome);
                    if (nome.isEmpty()) {
                        Toast.makeText(ctx, "Nome inválido.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    File novo = new File(pastaProjetos, nome);
                    if (novo.exists()) {
                        Toast.makeText(ctx, "Já existe um projeto com esse nome.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (criarEstruturaProjeto(novo, nome)) {
                        Toast.makeText(ctx, "Projeto \"" + nome + "\" criado!",
                                Toast.LENGTH_SHORT).show();
                        recarregarLista();
                    } else {
                        Toast.makeText(ctx, "Falha ao criar projeto.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    /**
     * Cria a estrutura completa do projeto:
     *
     *   Projeto/
     *   ├── Projeto.properties
     *   ├── src/
     *   │   └── main.cpp
     *   ├── include/
     *   └── lib/
     *       └── arm64-v8a/      ← onde os .so vão ficar
     */
    private boolean criarEstruturaProjeto(File pastaProjeto, String nome) {
    if (!pastaProjeto.mkdirs()) return false;

    File pastaSrc     = new File(pastaProjeto, "src");
    File pastaInclude = new File(pastaProjeto, "include");
    File pastaDeps    = new File(pastaProjeto, "deps");
    File pastaLib     = new File(pastaProjeto, "lib");

    pastaSrc.mkdirs();
    pastaInclude.mkdirs();
    pastaDeps.mkdirs();
    pastaLib.mkdirs();

    GerenciadorProjetoProperties.criarPadrao(pastaProjeto, nome);

    Projeto p = GerenciadorProjetoProperties.carregar(pastaProjeto);
    if (p.abis == null || p.abis.isEmpty()) {
        p.abis = new ArrayList<>();
        p.abis.add("arm64-v8a");
        p.abis.add("armeabi-v7a");
    }

    // Cria subpasta para CADA ABI
    for (String abi : p.abis) {
        String abiPasta = Projeto.normalizarAbi(abi);   // <- aqui
        new File(pastaLib, abiPasta).mkdirs();
    }

    File mainCpp = new File(pastaSrc, "main.cpp");
    String codigo =
            "// " + nome + "\n" +
            "// Mod AML para GTA San Andreas Android\n" +
            "// Gerado automaticamente pela AML IDE\n\n" +
            "#include <cstdio>\n" +
            "#include <jni.h>\n\n" +
            "extern \"C\" {\n" +
            "    __attribute__((visibility(\"default\")))\n" +
            "    void AMLMain() {\n" +
            "        printf(\"Mod " + nome + " carregado!\\n\");\n" +
            "    }\n" +
            "}\n";

    GerenciadorDeArquivos.salvarArquivo(mainCpp, codigo);

    return true;
}

    private String sanitizarNome(String nome) {
        if (nome == null) return "";
        return nome.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")   // remove inválidos
                .replaceAll("\\s+", "_");            // espaços → _
    }

    // ==========================================================
    //  Renomear
    // ==========================================================

    private void dialogoRenomear(File projeto) {
        EditText campo = new EditText(ctx);
        campo.setText(projeto.getName());
        campo.setSelection(projeto.getName().length());
        campo.setPadding(dp(20), dp(20), dp(20), dp(20));
        campo.setSingleLine(true);

        new AlertDialog.Builder(ctx)
                .setTitle("Renomear")
                .setView(campo)
                .setPositiveButton("Renomear", (d, w) -> {
                    String novoNome = campo.getText().toString().trim();
                    if (novoNome.isEmpty() || novoNome.equals(projeto.getName())) return;

                    novoNome = sanitizarNome(novoNome);
                    if (novoNome.isEmpty()) return;

                    File novo = new File(projeto.getParentFile(), novoNome);
                    if (novo.exists()) {
                        Toast.makeText(ctx, "Já existe.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (projeto.renameTo(novo)) {
                        Toast.makeText(ctx, "Renomeado.", Toast.LENGTH_SHORT).show();
                        recarregarLista();
                    } else {
                        Toast.makeText(ctx, "Falha ao renomear.",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ==========================================================
    //  Dependências
    // ==========================================================

    /**
     * Baixa as dependências do projeto via Git.
     * Lê a lista de `Projeto.properties`.
     */
    private void baixarDependencias(File pastaProjeto) {
        Projeto p = GerenciadorProjetoProperties.carregar(pastaProjeto);

        if (p.dependencias == null || p.dependencias.isEmpty()) {
            Toast.makeText(ctx,
                    "Nenhuma dependência configurada.\n" +
                    "Adicione em Projeto.properties (dependencias=url1,url2)",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(ctx,
                "Baixando " + p.dependencias.size() + " dependência(s)...",
                Toast.LENGTH_SHORT).show();

        modelo.GerenciadorDependencias.baixar(p, null, new modelo.GerenciadorDependencias.Callback() {
            @Override
            public void aoProgresso(int indice, int total, String nome) {
                // (opcional) mostrar progresso
            }

            @Override
            public void aoFinalizar(boolean sucesso, String erro) {
                if (sucesso) {
                    Toast.makeText(ctx,
                            "Dependências baixadas com sucesso!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ctx,
                            "Falha: " + erro,
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    // ==========================================================
    //  Deletar
    // ==========================================================

    private void confirmarDeletar(File projeto) {
        new AlertDialog.Builder(ctx)
                .setTitle("Deletar projeto?")
                .setMessage("\"" + projeto.getName() + "\"\n\n" +
                        "Todos os arquivos dentro serão apagados permanentemente.")
                .setPositiveButton("Deletar", (d, w) -> {
                    boolean ok = deletarRecursivo(projeto);
                    Toast.makeText(ctx,
                            ok ? "Deletado." : "Falha ao deletar.",
                            Toast.LENGTH_SHORT).show();
                    recarregarLista();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private boolean deletarRecursivo(File f) {
        if (f == null || !f.exists()) return false;
        if (f.isDirectory()) {
            File[] filhos = f.listFiles();
            if (filhos != null) {
                for (File x : filhos) deletarRecursivo(x);
            }
        }
        return f.delete();
    }

    // ==========================================================
    //  Clipboard
    // ==========================================================

    private void copiarParaClipboard(File projeto, boolean recortar) {
        clipboardArquivo = projeto;
        clipboardEhRecorte = recortar;
        atualizarBarraClipboard();
        Toast.makeText(ctx,
                (recortar ? "Recortado: " : "Copiado: ") + projeto.getName(),
                Toast.LENGTH_SHORT).show();
    }

    private void atualizarBarraClipboard() {
        if (clipboardArquivo == null) {
            barraClipboard.setVisibility(View.GONE);
        } else {
            barraClipboard.setVisibility(View.VISIBLE);
            clipboardTexto.setText(
                    (clipboardEhRecorte ? "✂ " : "📋 ") + clipboardArquivo.getName());
        }
    }

    private void colarClipboard() {
        if (clipboardArquivo == null) return;

        File destino = new File(pastaProjetos, clipboardArquivo.getName());

        if (destino.exists()) {
            String base = clipboardArquivo.getName();
            int i = 1;
            while (destino.exists()) {
                destino = new File(pastaProjetos, base + "_copia" + i);
                i++;
            }
        }

        boolean ok;
        if (clipboardEhRecorte) {
            ok = clipboardArquivo.renameTo(destino);
        } else {
            ok = copiarRecursivo(clipboardArquivo, destino);
        }

        if (ok) {
            Toast.makeText(ctx,
                    (clipboardEhRecorte ? "Movido: " : "Colado: ") + destino.getName(),
                    Toast.LENGTH_SHORT).show();
            clipboardArquivo = null;
            clipboardEhRecorte = false;
            atualizarBarraClipboard();
            recarregarLista();
        } else {
            Toast.makeText(ctx, "Falha na operação.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean copiarRecursivo(File origem, File destino) {
        try {
            if (origem.isDirectory()) {
                if (!destino.exists() && !destino.mkdirs()) return false;
                File[] filhos = origem.listFiles();
                if (filhos != null) {
                    for (File f : filhos) {
                        if (!copiarRecursivo(f, new File(destino, f.getName()))) return false;
                    }
                }
                return true;
            } else {
                return copiarArquivo(origem, destino);
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean copiarArquivo(File origem, File destino) {
        try (InputStream in = new FileInputStream(origem);
             OutputStream out = new FileOutputStream(destino)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    // ==========================================================
    //  Utils
    // ==========================================================

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}