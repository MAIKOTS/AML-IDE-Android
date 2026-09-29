package ui.dialogos;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DialogoSeletorDePasta {

    public interface RespostaSelecaoPasta {
        void aoSelecionarPasta(File pastaSelecionada);
    }

    // ---------- Raiz permitida (não dá pra subir acima disso) ----------
    private final File raizPermitida;

    private final Context contexto;
    private final RespostaSelecaoPasta resposta;

    private File pastaAtual;

    public DialogoSeletorDePasta(Context contexto, RespostaSelecaoPasta resposta) {
        this.contexto = contexto;
        this.resposta = resposta;

        // Raiz permitida = armazenamento externo principal (/storage/emulated/0)
        this.raizPermitida = Environment.getExternalStorageDirectory();
        this.pastaAtual = raizPermitida;
    }

    public void exibir() {
        AlertDialog.Builder construtor = new AlertDialog.Builder(contexto);
        construtor.setTitle("Selecionar Pasta: " + pastaAtual.getName());

        // ---------- Lista de itens ----------
        List<Item> itens = montarItens(pastaAtual);

        ItemAdapter adapter = new ItemAdapter(contexto, itens);

        final AlertDialog[] dialogoRef = new AlertDialog[1];
        adapter.setOnItemClickListener(item -> {
            if (item.ehPasta) {
                if (dialogoRef[0] != null) dialogoRef[0].dismiss();
                pastaAtual = item.arquivo;
                exibir();
            } else {
                Toast.makeText(contexto,
                        "Isto é um arquivo, não uma pasta.",
                        Toast.LENGTH_SHORT).show();
            }
        });

        construtor.setAdapter(adapter, null);

        construtor.setPositiveButton("Usar esta pasta", (d, which) ->
                resposta.aoSelecionarPasta(pastaAtual));

        construtor.setNegativeButton("Cancelar", null);

        // Só habilita "Subir" se for possível
        AlertDialog dialogo = construtor.create();
        dialogoRef[0] = dialogo;
        dialogo.setOnShowListener(d -> {
            boolean podeSubir = podeSubirDe(pastaAtual);
            dialogo.getButton(AlertDialog.BUTTON_NEUTRAL)
                    .setEnabled(podeSubir);
        });

        // Botão "Subir" só aparece se puder subir
        if (podeSubirDe(pastaAtual)) {
            construtor.setNeutralButton("↑ Subir", (d, which) -> {
                File pai = pastaAtual.getParentFile();
                if (pai != null && estaDentroDaRaiz(pai)) {
                    pastaAtual = pai;
                    exibir();
                } else {
                    Toast.makeText(contexto,
                            "Você já está no topo.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        dialogo.show();
    }

    // ==========================================================
    //  Montagem da lista
    // ==========================================================

    private List<Item> montarItens(File dir) {
        List<Item> itens = new ArrayList<>();

        File[] filhos = dir.listFiles();
        if (filhos == null) return itens;

        // Ordena: pastas primeiro, depois arquivos; alfabético
        Arrays.sort(filhos, (a, b) -> {
            if (a.isDirectory() && !b.isDirectory()) return -1;
            if (!a.isDirectory() && b.isDirectory()) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });

        for (File f : filhos) {
            if (f.getName().startsWith(".")) continue; // ocultos
            itens.add(new Item(f.getName(), f, f.isDirectory()));
        }
        return itens;
    }

    // ==========================================================
    //  Navegação segura
    // ==========================================================

    private boolean podeSubirDe(File pasta) {
        File pai = pasta.getParentFile();
        return pai != null && estaDentroDaRaiz(pai);
    }

    private boolean estaDentroDaRaiz(File destino) {
        try {
            String cDestino = destino.getCanonicalPath();
            String cRaiz    = raizPermitida.getCanonicalPath();
            return cDestino.equals(cRaiz)
                    || cDestino.startsWith(cRaiz + File.separator);
        } catch (IOException e) {
            return false;
        }
    }

    // ==========================================================
    //  Modelo + Adapter
    // ==========================================================

    private static class Item {
        final String nome;
        final File arquivo;
        final boolean ehPasta;
        Item(String nome, File arquivo, boolean ehPasta) {
            this.nome = nome;
            this.arquivo = arquivo;
            this.ehPasta = ehPasta;
        }
    }

    private static class ItemAdapter extends BaseAdapter {
        interface OnClick { void onClick(Item item); }

        private final Context ctx;
        private final List<Item> itens;
        private OnClick listener;

        ItemAdapter(Context ctx, List<Item> itens) {
            this.ctx = ctx;
            this.itens = itens;
        }

        void setOnItemClickListener(OnClick l) { this.listener = l; }

        @Override public int getCount() { return itens.size(); }
        @Override public Object getItem(int i) { return itens.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Item item = itens.get(position);

            LinearLayout linha = new LinearLayout(ctx);
            linha.setOrientation(LinearLayout.HORIZONTAL);
            linha.setGravity(Gravity.CENTER_VERTICAL);
            int pad = dp(14);
            linha.setPadding(pad, pad, pad, pad);

            // Ícone / símbolo
            TextView simbolo = new TextView(ctx);
            simbolo.setText(item.ehPasta ? "📁" : "📄");
            simbolo.setTextSize(18);
            simbolo.setPadding(0, 0, dp(14), 0);
            linha.addView(simbolo);

            // Nome
            TextView nome = new TextView(ctx);
            nome.setText(item.nome);
            nome.setTextSize(14);
            nome.setTypeface(Typeface.DEFAULT,
                    item.ehPasta ? Typeface.BOLD : Typeface.NORMAL);
            nome.setTextColor(item.ehPasta
                    ? Color.parseColor("#F4F4F5")
                    : Color.parseColor("#A1A1AA"));
            linha.addView(nome);

            // Arquivo fica "apagado" e não clicável
            linha.setAlpha(item.ehPasta ? 1f : 0.55f);

            linha.setOnClickListener(v -> {
                if (listener != null) listener.onClick(item);
            });

            return linha;
        }

        private int dp(int v) {
            return (int) (v * ctx.getResources().getDisplayMetrics().density);
        }
    }
}