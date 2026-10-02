package ui.dialogos;

/**
 * Item de uma lista de ações de diálogo.
 *  iconeRes = 0 → sem ícone
 *  cor      = 0 → cor padrão (cinza)
 */
public class ItemAcao {

    public final int      iconeRes;
    public final String   texto;
    public final int      cor;
    public final Runnable acao;

    public ItemAcao(int iconeRes, String texto, Runnable acao) {
        this(iconeRes, texto, 0, acao);
    }

    public ItemAcao(int iconeRes, String texto, int cor, Runnable acao) {
        this.iconeRes = iconeRes;
        this.texto    = texto;
        this.cor      = cor;
        this.acao     = acao;
    }
}