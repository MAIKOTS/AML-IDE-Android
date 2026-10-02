package ui.componentes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

/**
 * Espelho do PainelLateralDeslizante, mas abre pela DIREITA.
 *
 * Uso:
 *   new PainelLateralDireita(ctx, conteudo, menuLateral);
 *
 * - Swipe da borda direita pra ESQUERDA abre
 * - Swipe pra DIREITA fecha
 * - Botão "fechar" interno também fecha
 *
 * ★ Pode ser habilitado/desabilitado em runtime via setHabilitado().
 *   Quando desabilitado, bloqueia gestos e fecha automaticamente.
 */
public class PainelLateralDireita extends FrameLayout {

    private final LinearLayout menuLateral;
    private final View sombraFundo;
    private boolean menuAberto = false;
    private boolean habilitado = true;
    private float pontoToqueInicialX = 0;

    private final int larguraMenu;
    private final float margemBordaPx;

    /** Largura do menu: 50% da tela. */
    private static final float FRACAO_LARGURA = 0.50f;

    public PainelLateralDireita(Context contexto,
                                View conteudoPrincipal,
                                LinearLayout menuLateral) {
        super(contexto);
        this.menuLateral = menuLateral;

        margemBordaPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 30,
                contexto.getResources().getDisplayMetrics());

        int larguraTela = contexto.getResources().getDisplayMetrics().widthPixels;
        this.larguraMenu = (int) (larguraTela * FRACAO_LARGURA);

        setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        // 1. Conteúdo principal
        addView(conteudoPrincipal);

        // 2. Sombra / overlay
        sombraFundo = new View(contexto);
        sombraFundo.setBackgroundColor(Color.parseColor("#99000000"));
        sombraFundo.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        sombraFundo.setVisibility(View.GONE);
        sombraFundo.setOnTouchListener((v, event) -> {
            if (menuAberto && event.getAction() == MotionEvent.ACTION_UP) {
                fecharMenu();
            }
            return true;
        });
        addView(sombraFundo);

        // 3. Estilo do menu
        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.parseColor("#18181B"));

        float raio = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 4,
                contexto.getResources().getDisplayMetrics());

        fundo.setCornerRadii(new float[]{
                raio, raio,
                raio, raio,
                raio, raio,
                raio, raio
        });

        int bordaPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 1,
                contexto.getResources().getDisplayMetrics());
        fundo.setStroke(bordaPx, Color.parseColor("#27272A"));
        menuLateral.setBackground(fundo);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                larguraMenu,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END);
        menuLateral.setLayoutParams(params);
        menuLateral.setTranslationX(larguraMenu);

        menuLateral.setClickable(true);
        menuLateral.setFocusable(true);

        addView(menuLateral);
    }

    // ==========================================================
    //  Habilitação (só funciona onde o ControladorApp permitir)
    // ==========================================================

    /**
     * Habilita/desabilita o painel.
     * Quando desabilitado, bloqueia gestos e fecha o menu se aberto.
     */
    public void setHabilitado(boolean h) {
        this.habilitado = h;
        if (!h && menuAberto) {
            fecharMenu();
        }
    }

    public boolean isHabilitado() {
        return habilitado;
    }

    // ==========================================================
    //  Toques / gestos
    // ==========================================================

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!habilitado) return false;

        float x = ev.getX();
        float larguraView = getWidth();

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pontoToqueInicialX = x;
                if (menuAberto || x >= larguraView - margemBordaPx) {
                    return false;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                float deltaX = x - pontoToqueInicialX;
                if (!menuAberto
                        && pontoToqueInicialX >= larguraView - margemBordaPx
                        && deltaX < -10) {
                    return true;
                }
                if (menuAberto && deltaX > 10) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        if (!habilitado) return false;

        float x = evento.getX();
        float larguraView = getWidth();

        switch (evento.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pontoToqueInicialX = x;
                return menuAberto || pontoToqueInicialX >= larguraView - margemBordaPx;

            case MotionEvent.ACTION_MOVE:
                if (!menuAberto && pontoToqueInicialX < larguraView - margemBordaPx) {
                    return false;
                }

                float deslocamentoX = x - pontoToqueInicialX;
                if (menuAberto) {
                    if (deslocamentoX > 0) {
                        float novaPos = Math.min(larguraMenu, deslocamentoX);
                        menuLateral.setTranslationX(novaPos);
                        float progresso = (larguraMenu - novaPos) / (float) larguraMenu;
                        sombraFundo.setAlpha(progresso);
                    }
                } else {
                    float novaPos = Math.max(0, larguraMenu + deslocamentoX);
                    menuLateral.setTranslationX(novaPos);
                    sombraFundo.setVisibility(View.VISIBLE);
                    float progresso = (larguraMenu - novaPos) / (float) larguraMenu;
                    sombraFundo.setAlpha(progresso);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!menuAberto && pontoToqueInicialX < larguraView - margemBordaPx) {
                    return false;
                }

                float posAtual = menuLateral.getTranslationX();
                if (posAtual < ((float) larguraMenu / 2)) {
                    abrirMenu();
                } else {
                    fecharMenu();
                }
                return true;
        }
        return super.onTouchEvent(evento);
    }

    // ==========================================================
    //  API pública
    // ==========================================================

    public void abrirMenu() {
        if (!habilitado) return;

        sombraFundo.setVisibility(View.VISIBLE);
        sombraFundo.animate().alpha(1.0f).setDuration(200).start();
        menuLateral.animate().translationX(0).setDuration(200).start();
        menuAberto = true;
    }

    public void fecharMenu() {
        sombraFundo.animate().alpha(0.0f).setDuration(200).withEndAction(() -> {
            sombraFundo.setVisibility(View.GONE);
        }).start();
        menuLateral.animate().translationX(larguraMenu).setDuration(200).start();
        menuAberto = false;
    }

    public boolean isMenuAberto() {
        return menuAberto;
    }

    public LinearLayout obterMenuLateral() {
        return menuLateral;
    }
}