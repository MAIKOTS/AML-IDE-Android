package ui.telas;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import app.R;
import modelo.GerenciadorProjetoProperties;
import modelo.Projeto;
import ui.dialogos.DialogoApp;
import app.util.GerenciadorDeArquivos;

/**
 * Tela de criação de novo projeto.
 *
 * Formulário completo com todos os campos do Projeto.properties.
 * Ao clicar em CRIAR:
 *   1. Cria a estrutura de pastas
 *   2. Gera o Projeto.properties
 *   3. Cria o main.cpp inicial
 *   4. Chama o callback de sucesso (que navega pra estrutura)
 */
public class TelaNovoProjeto {

    public interface Acoes {
        void aoVoltar();
        /** Chamado depois de criar com sucesso. */
        void aoCriarProjeto(File pastaProjeto);
    }

    private static final String PASTA_PROJETOS = "AML_IDE/Projetos";

    private final Context ctx;
    private final Acoes acoes;
    private final Handler handlerUI = new Handler(Looper.getMainLooper());

    // Views
    private View raizView;
    private EditText campoNome, campoAutor, campoDescricao, campoVersao;
    private EditText campoFlags, campoFontes, campoIncludes, campoLibs, campoDependencias;
    private TextView campoApi, campoStd, campoOtimizacao;
    private TextView chipArm64, chipArm32;
    private TextView btnCriar;
    private TextView status;

    // Estado dos chips
    private boolean abiArm64Ativa = true;
    private boolean abiArm32Ativa = true;

    public TelaNovoProjeto(Context ctx, Acoes acoes) {
        this.ctx = ctx;
        this.acoes = acoes;
    }

    // ==========================================================
    //  Construção
    // ==========================================================

    public View construir() {
        raizView = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_novo_projeto, null, false);

        // Bind
        campoNome         = raizView.findViewById(R.id.novoProjetoNome);
        campoAutor        = raizView.findViewById(R.id.novoProjetoAutor);
        campoDescricao    = raizView.findViewById(R.id.novoProjetoDescricao);
        campoVersao       = raizView.findViewById(R.id.novoProjetoVersao);
        campoApi          = raizView.findViewById(R.id.novoProjetoApi);
        campoStd          = raizView.findViewById(R.id.novoProjetoStd);
        campoOtimizacao   = raizView.findViewById(R.id.novoProjetoOtimizacao);
        campoFlags        = raizView.findViewById(R.id.novoProjetoFlags);
        campoFontes       = raizView.findViewById(R.id.novoProjetoFontes);
        campoIncludes     = raizView.findViewById(R.id.novoProjetoIncludes);
        campoLibs         = raizView.findViewById(R.id.novoProjetoLibs);
        campoDependencias = raizView.findViewById(R.id.novoProjetoDependencias);
        chipArm64         = raizView.findViewById(R.id.novoProjetoAbiArm64);
        chipArm32         = raizView.findViewById(R.id.novoProjetoAbiArm32);
        btnCriar          = raizView.findViewById(R.id.novoProjetoBtnCriar);
        status            = raizView.findViewById(R.id.novoProjetoStatus);

        // Valores padrão
        campoVersao.setText("1.0.0");
        campoFlags.setText("");
        campoIncludes.setText("include");

        atualizarChipsAbi();

        // Listeners
        raizView.findViewById(R.id.novoProjetoBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoVoltar();
        });

        chipArm64.setOnClickListener(v -> {
            abiArm64Ativa = !abiArm64Ativa;
            garantirPeloMenosUmaAbi();
            atualizarChipsAbi();
        });

        chipArm32.setOnClickListener(v -> {
            abiArm32Ativa = !abiArm32Ativa;
            garantirPeloMenosUmaAbi();
            atualizarChipsAbi();
        });

        // Pickers (reaproveitando DialogoApp.selecaoUnica)
        campoStd.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "Padrão C++",
                new String[]{"c++11", "c++14", "c++17", "c++20", "c++23"},
                2, (pos, valor) -> campoStd.setText(valor)));

        campoOtimizacao.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "Otimização",
                new String[]{"O0", "O1", "O2", "O3", "Os", "Oz"},
                2, (pos, valor) -> campoOtimizacao.setText(valor)));

        campoApi.setOnClickListener(v -> DialogoApp.selecaoUnica(ctx,
                "API mínima",
                new String[]{"21  (Android 5.0+)"},
                0, (pos, valor) -> campoApi.setText(valor)));

        btnCriar.setOnClickListener(v -> criarProjeto());

        return raizView;
    }

    // ==========================================================
    //  Chips de ABI
    // ==========================================================

    private void garantirPeloMenosUmaAbi() {
        if (!abiArm64Ativa && !abiArm32Ativa) {
            abiArm64Ativa = true;
        }
    }

    private void atualizarChipsAbi() {
        pintarChip(chipArm64, abiArm64Ativa);
        pintarChip(chipArm32, abiArm32Ativa);
    }

    private void pintarChip(TextView chip, boolean ativo) {
        if (chip == null) return;

        GradientDrawable fundo = new GradientDrawable();
        fundo.setShape(GradientDrawable.RECTANGLE);
        fundo.setCornerRadius(dp(6));

        if (ativo) {
            fundo.setColor(Color.parseColor("#0D2A19"));
            fundo.setStroke(dp(1), Color.parseColor("#00E676"));
            chip.setTextColor(Color.parseColor("#00E676"));
        } else {
            fundo.setColor(Color.parseColor("#0F0F12"));
            fundo.setStroke(dp(1), Color.parseColor("#27272A"));
            chip.setTextColor(Color.parseColor("#52525B"));
        }
        chip.setBackground(fundo);
    }

    // ==========================================================
    //  Criação
    // ==========================================================

    private void criarProjeto() {
        // ---------- Validações ----------
        String nome = campoNome.getText().toString().trim();
        if (nome.isEmpty()) {
            erro("Digite um nome para o projeto.");
            campoNome.requestFocus();
            return;
        }

        nome = sanitizarNome(nome);
        if (nome.isEmpty()) {
            erro("Nome inválido. Use apenas letras, números, '-' e '_'.");
            campoNome.requestFocus();
            return;
        }

        String autor = campoAutor.getText().toString().trim();
        if (autor.isEmpty()) autor = "Desconhecido";

        String descricao = campoDescricao.getText().toString().trim();
        String versao = campoVersao.getText().toString().trim();
        if (versao.isEmpty()) versao = "1.0.0";

        // ---------- Destino ----------
        File pastaProjetos = new File(
                Environment.getExternalStorageDirectory(), PASTA_PROJETOS);
        if (!pastaProjetos.exists() && !pastaProjetos.mkdirs()) {
            erro("Não foi possível criar a pasta de projetos.");
            return;
        }

        File pastaProjeto = new File(pastaProjetos, nome);
        if (pastaProjeto.exists()) {
            erro("Já existe um projeto com esse nome.");
            campoNome.requestFocus();
            return;
        }

        // ---------- Monta Projeto ----------
        Projeto p = new Projeto();
        p.pastaRaiz = pastaProjeto;
        p.nome      = nome;
        p.versao    = versao;
        p.autor     = autor;
        p.descricao = descricao;
        p.libNome   = "lib" + nome.toLowerCase().replaceAll("[^a-z0-9_]", "_");

        // ABIs
        p.abis = new ArrayList<>();
        if (abiArm64Ativa) p.abis.add("arm64-v8a");
        if (abiArm32Ativa) p.abis.add("armeabi-v7a");

        // API (por enquanto só 21)
        p.api = 21;

        // Compilador
        p.std        = campoStd.getText().toString().trim();
        p.otimizacao = campoOtimizacao.getText().toString().trim();

        // Listas
        p.flags    = parseLista(campoFlags.getText().toString());
        p.fontes   = parseLista(campoFontes.getText().toString());
        p.includes = parseLista(campoIncludes.getText().toString());
        p.libs     = parseLista(campoLibs.getText().toString());
        p.dependencias = parseDependencias(
                campoDependencias.getText().toString(), pastaProjeto);

        // ---------- Cria estrutura ----------
        btnCriar.setEnabled(false);
        btnCriar.setAlpha(0.5f);
        status.setText("Criando...");

        final Projeto projetoFinal = p;
        final String  nomeFinal    = nome;

        new Thread(() -> {
            boolean ok = construirEstrutura(projetoFinal);

            handlerUI.post(() -> {
                btnCriar.setEnabled(true);
                btnCriar.setAlpha(1.0f);

                if (ok) {
                    status.setText("Projeto criado!");
                    Toast.makeText(ctx,
                            "Projeto \"" + nomeFinal + "\" criado!",
                            Toast.LENGTH_SHORT).show();

                    if (acoes != null) {
                        acoes.aoCriarProjeto(projetoFinal.pastaRaiz);
                    }
                } else {
                    status.setText("");
                    erro("Falha ao criar o projeto. Verifique as permissões.");
                }
            });
        }, "criar-projeto").start();
    }

    /**
     * Cria a estrutura em disco:
     *   Projeto/
     *   ├── Projeto.properties
     *   ├── src/main.cpp
     *   ├── include/
     *   ├── deps/
     *   └── lib/<abi>/
     */
    private boolean construirEstrutura(Projeto p) {
        try {
            File raiz = p.pastaRaiz;
            if (!raiz.mkdirs() && !raiz.isDirectory()) return false;

            File src     = new File(raiz, "src");
            File include = new File(raiz, "include");
            File deps    = new File(raiz, "deps");
            File lib     = new File(raiz, "lib");

            src.mkdirs();
            include.mkdirs();
            deps.mkdirs();
            lib.mkdirs();

            // Subpastas de ABI
            for (String abi : p.abis) {
                String abiPasta = Projeto.normalizarAbi(abi);
                new File(lib, abiPasta).mkdirs();
            }

            // Projeto.properties
            if (!GerenciadorProjetoProperties.salvar(p)) return false;

            // main.cpp inicial
            File mainCpp = new File(src, "main.cpp");
            String codigo =
                    "// " + p.nome + "\n" +
                    "// Mod AML para GTA San Andreas Android\n" +
                    "// Criado com a AML IDE\n\n" +
                    "#include <cstdio>\n" +
                    "#include <jni.h>\n\n" +
                    "extern \"C\" {\n" +
                    "    __attribute__((visibility(\"default\")))\n" +
                    "    void AMLMain() {\n" +
                    "        printf(\"Mod " + p.nome + " carregado!\\n\");\n" +
                    "    }\n" +
                    "}\n";

            GerenciadorDeArquivos.salvarArquivo(mainCpp, codigo);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==========================================================
    //  Parsers
    // ==========================================================

    private List<String> parseLista(String texto) {
        List<String> out = new ArrayList<>();
        if (texto == null) return out;
        for (String s : texto.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private List<modelo.Dependencia> parseDependencias(String texto, File pastaProjeto) {
        List<modelo.Dependencia> out = new ArrayList<>();
        if (texto == null || texto.trim().isEmpty()) return out;

        for (String item : texto.split(",")) {
            String s = item.trim();
            if (s.isEmpty()) continue;

            modelo.Dependencia d = new modelo.Dependencia();

            int idx = s.lastIndexOf('@');
            if (idx > 0 && idx < s.length() - 1) {
                d.url = s.substring(0, idx);
                d.branch = s.substring(idx + 1);
            } else {
                d.url = s;
                d.branch = null;
            }

            String url = d.url;
            while (url.endsWith("/")) url = url.substring(0, url.length() - 1);
            if (url.endsWith(".git")) url = url.substring(0, url.length() - 4);
            int slash = url.lastIndexOf('/');
            d.nomeRepo = (slash >= 0) ? url.substring(slash + 1) : url;
            d.pastaLocal = new File(pastaProjeto, "deps/" + d.nomeRepo);

            out.add(d);
        }
        return out;
    }

    private String sanitizarNome(String nome) {
        if (nome == null) return "";
        return nome.trim()
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "_");
    }

    private void erro(String msg) {
        status.setText(msg);
        status.setTextColor(Color.parseColor("#FF5252"));
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}