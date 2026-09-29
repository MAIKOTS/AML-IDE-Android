package ui.telas;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

/**
 * Tela visual para editar o Projeto.properties.
 *
 * Apresentada como Dialog em tela cheia (pelo botão Configs do editor).
 * Ao salvar, sobrescreve o arquivo Projeto.properties no disco.
 */
public class TelaConfigProjeto {

    public interface Acoes {
        /** Chamado depois que salvou com sucesso. */
        void aoSalvar(Projeto projeto);
        /** Chamado quando o usuário cancela (Voltar). */
        void aoCancelar();
    }

    private final Context ctx;
    private final File pastaProjeto;
    private final Acoes acoes;
    private final Projeto projeto;

    // Views
    private EditText campoNome, campoVersao, campoAutor, campoDescricao;
    private EditText campoApi, campoLibNome, campoFlags, campoFontes;
    private EditText campoIncludes, campoLibs, campoDependencias;
    private TextView campoStd, campoOtimizacao;
    private TextView chipArm64, chipArm32;

    private boolean abiArm64Ativa;
    private boolean abiArm32Ativa;

    public TelaConfigProjeto(Context ctx, File pastaProjeto, Acoes acoes) {
        this.ctx = ctx;
        this.pastaProjeto = pastaProjeto;
        this.acoes = acoes;
        this.projeto = GerenciadorProjetoProperties.carregar(pastaProjeto);
    }

    public View construir() {
        View raiz = LayoutInflater.from(ctx)
                .inflate(R.layout.tela_config_projeto, null, false);

        // -------- Bind --------
        campoNome         = raiz.findViewById(R.id.configNome);
        campoVersao       = raiz.findViewById(R.id.configVersao);
        campoAutor        = raiz.findViewById(R.id.configAutor);
        campoDescricao    = raiz.findViewById(R.id.configDescricao);
        campoApi          = raiz.findViewById(R.id.configApi);
        campoLibNome      = raiz.findViewById(R.id.configLibNome);
        campoFlags        = raiz.findViewById(R.id.configFlags);
        campoFontes       = raiz.findViewById(R.id.configFontes);
        campoIncludes     = raiz.findViewById(R.id.configIncludes);
        campoLibs         = raiz.findViewById(R.id.configLibs);
        campoDependencias = raiz.findViewById(R.id.configDependencias);
        campoStd          = raiz.findViewById(R.id.configStd);
        campoOtimizacao   = raiz.findViewById(R.id.configOtimizacao);
        chipArm64         = raiz.findViewById(R.id.configAbiArm64);
        chipArm32         = raiz.findViewById(R.id.configAbiArm32);

        // Título
        TextView titulo = raiz.findViewById(R.id.configTitulo);
        titulo.setText(pastaProjeto.getName());

        // -------- Preenche com os dados atuais --------
        campoNome.setText(projeto.nome);
        campoVersao.setText(projeto.versao);
        campoAutor.setText(projeto.autor);
        campoDescricao.setText(projeto.descricao);
        campoApi.setText(String.valueOf(projeto.api));
        campoLibNome.setText(projeto.libNome);
        campoFlags.setText(join(projeto.flags));
        campoFontes.setText(join(projeto.fontes));
        campoIncludes.setText(join(projeto.includes));
        campoLibs.setText(join(projeto.libs));
        campoDependencias.setText(serializarDeps(projeto.dependencias));
        campoStd.setText(projeto.std);
        campoOtimizacao.setText(projeto.otimizacao);

        // ABIs
        abiArm64Ativa = projeto.abis.contains("arm64-v8a");
        abiArm32Ativa = projeto.abis.contains("armeabi-v7a");
        if (!abiArm64Ativa && !abiArm32Ativa) abiArm64Ativa = true; // fallback
        atualizarChipsAbi();

        // -------- Listeners --------
        raiz.findViewById(R.id.configBtnVoltar).setOnClickListener(v -> {
            if (acoes != null) acoes.aoCancelar();
        });

        raiz.findViewById(R.id.configBtnSalvar).setOnClickListener(v -> salvar());

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

        campoStd.setOnClickListener(v -> mostrarSeletor("Padrão C++",
                new String[]{"c++11", "c++14", "c++17", "c++20", "c++23"},
                campoStd));

        campoOtimizacao.setOnClickListener(v -> mostrarSeletor("Otimização",
                new String[]{"O0", "O1", "O2", "O3", "Os", "Oz"},
                campoOtimizacao));

        return raiz;
    }

    // ==========================================================
    //  Chips de ABI
    // ==========================================================

    private void garantirPeloMenosUmaAbi() {
        if (!abiArm64Ativa && !abiArm32Ativa) {
            // Reverte a última ação: mantém pelo menos uma
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
            fundo.setColor(Color.parseColor("#0D2A19"));      // verde escuro
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
    //  Seletor de opção (std, otimização)
    // ==========================================================

    private void mostrarSeletor(String titulo, String[] opcoes, TextView alvo) {
        String atual = alvo.getText().toString();

        new AlertDialog.Builder(ctx)
                .setTitle(titulo)
                .setSingleChoiceItems(opcoes, indexOf(opcoes, atual), (d, w) -> {
                    alvo.setText(opcoes[w]);
                    d.dismiss();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int indexOf(String[] arr, String valor) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(valor)) return i;
        }
        return -1;
    }

    // ==========================================================
    //  Salvar
    // ==========================================================

    private void salvar() {
        // Nome e lib_nome não podem ficar vazios
        String nome = campoNome.getText().toString().trim();
        if (nome.isEmpty()) {
            toast("O nome não pode ficar vazio.");
            campoNome.requestFocus();
            return;
        }

        String libNome = campoLibNome.getText().toString().trim();
        if (libNome.isEmpty()) {
            toast("O nome da biblioteca não pode ficar vazio.");
            campoLibNome.requestFocus();
            return;
        }

        // API (numérica)
        int api = 21;
        try {
            api = Integer.parseInt(campoApi.getText().toString().trim());
            if (api < 1 || api > 99) throw new NumberFormatException();
        } catch (Exception e) {
            toast("API inválida. Use um número entre 1 e 99.");
            campoApi.requestFocus();
            return;
        }

        // Aplica tudo no Projeto
        projeto.nome      = nome;
        projeto.versao    = campoVersao.getText().toString().trim();
        projeto.autor     = campoAutor.getText().toString().trim();
        projeto.descricao = campoDescricao.getText().toString().trim();
        projeto.api       = api;
        projeto.libNome   = libNome;

        projeto.std        = campoStd.getText().toString().trim();
        projeto.otimizacao = campoOtimizacao.getText().toString().trim();

        projeto.flags    = parseLista(campoFlags.getText().toString());
        projeto.fontes   = parseLista(campoFontes.getText().toString());
        projeto.includes = parseLista(campoIncludes.getText().toString());
        projeto.libs     = parseLista(campoLibs.getText().toString());

        // ABIs
        projeto.abis.clear();
        if (abiArm64Ativa) projeto.abis.add("arm64-v8a");
        if (abiArm32Ativa) projeto.abis.add("armeabi-v7a");

        // Dependências
        projeto.dependencias = parseDependencias(campoDependencias.getText().toString());

        // Salva
        if (GerenciadorProjetoProperties.salvar(projeto)) {
            toast("Configurações salvas!");
            if (acoes != null) acoes.aoSalvar(projeto);
        } else {
            toast("Erro ao salvar. Verifique as permissões.");
        }
    }

    // ==========================================================
    //  Parsers
    // ==========================================================

    private String join(List<String> lista) {
        if (lista == null || lista.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lista.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(lista.get(i));
        }
        return sb.toString();
    }

    private List<String> parseLista(String texto) {
        List<String> out = new ArrayList<>();
        if (texto == null) return out;
        for (String s : texto.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private String serializarDeps(List<modelo.Dependencia> deps) {
        if (deps == null || deps.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < deps.size(); i++) {
            if (i > 0) sb.append(",");
            modelo.Dependencia d = deps.get(i);
            sb.append(d.url);
            if (d.branch != null && !d.branch.isEmpty()) {
                sb.append("@").append(d.branch);
            }
        }
        return sb.toString();
    }

    private List<modelo.Dependencia> parseDependencias(String texto) {
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

    private void toast(String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
    }

    private int dp(int v) {
        return (int) (v * ctx.getResources().getDisplayMetrics().density);
    }
}