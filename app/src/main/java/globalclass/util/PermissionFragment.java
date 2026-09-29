package globalclass.util;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import app.R;
import globalclass.settings.LogApp;

@SuppressWarnings("deprecation")
public class PermissionFragment extends Fragment {

    private static final String TAG = "PermissionFragment";
    private static final int CODIGO_REQUISICAO_PERMISSAO         = 1001;
    private static final int CODIGO_REQUISICAO_GERENCIAR_STORAGE = 1002;

    public interface CallbackPermissao {
        void onResultado(boolean concedida);
    }

    private CallbackPermissao callback;
    private boolean jaMostrouDialogoNestaSessao = false;

    public PermissionFragment() { }

    // ==========================================================
    //  API pública
    // ==========================================================

    public static void solicitar(Activity activity, CallbackPermissao callback) {
        if (activity == null) {
            if (callback != null) callback.onResultado(false);
            return;
        }

        // Já tem permissão? Retorna na hora.
        if (temPermissao(activity)) {
            LogApp.i("Permissão de armazenamento: OK (já concedida)");
            if (callback != null) callback.onResultado(true);
            return;
        }

        // Sem permissão — mostra o diálogo explicativo
        FragmentManager fm = activity.getFragmentManager();
        PermissionFragment fragment = (PermissionFragment) fm.findFragmentByTag(TAG);

        if (fragment == null) {
            fragment = new PermissionFragment();
            fragment.callback = callback;
            fm.beginTransaction().add(fragment, TAG).commitAllowingStateLoss();
        } else {
            fragment.callback = callback;
        }
    }

    /** Verifica se a permissão de armazenamento está concedida. */
    public static boolean temPermissao(Activity activity) {
        if (activity == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — permissão especial "All files access"
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 a 10
            int leitura = activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
            int escrita = activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return leitura == PackageManager.PERMISSION_GRANTED
                && escrita == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Android 5.1 e inferiores
    }

    // ==========================================================
    //  Ciclo de vida
    // ==========================================================

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        // Evita mostrar o diálogo múltiplas vezes se a Activity recriar
        if (!jaMostrouDialogoNestaSessao) {
            jaMostrouDialogoNestaSessao = true;
            exibirDialogoExplicativo();
        }
    }

    // ==========================================================
    //  Diálogo customizado
    // ==========================================================

    private void exibirDialogoExplicativo() {
        Activity activity = getActivity();
        if (activity == null) return;

        // Infla o layout customizado
        View layout = LayoutInflater.from(activity)
                .inflate(R.layout.dialogo_permissao, null, false);

        TextView btnContinuar = layout.findViewById(R.id.btnPermissaoContinuar);
        TextView btnCancelar  = layout.findViewById(R.id.btnPermissaoCancelar);

        final AlertDialog[] dialogoRef = new AlertDialog[1];

        btnContinuar.setOnClickListener(v -> {
            if (dialogoRef[0] != null) dialogoRef[0].dismiss();
            iniciarSolicitacao();
        });

        btnCancelar.setOnClickListener(v -> {
            if (dialogoRef[0] != null) dialogoRef[0].dismiss();
            notificarERemover(false);
        });

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(layout)
                .setCancelable(false)
                .create();

        // Aplica fundo arredondado
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_dialog_arredondado);
        }

        dialogoRef[0] = dialog;
        dialog.show();
    }

    // ==========================================================
    //  Solicitação
    // ==========================================================

    private void iniciarSolicitacao() {
        Activity activity = getActivity();
        if (activity == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ — abre a tela de "Acesso a todos os arquivos"
            LogApp.i("Abrindo tela de 'Todos os arquivos' (Android 11+)");
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                startActivityForResult(intent, CODIGO_REQUISICAO_GERENCIAR_STORAGE);
            } catch (Exception e) {
                // Fallback para a tela geral
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivityForResult(intent, CODIGO_REQUISICAO_GERENCIAR_STORAGE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 a 10 — diálogo de permissão nativo
            LogApp.i("Solicitando READ/WRITE_EXTERNAL_STORAGE (Android 6-10)");
            requestPermissions(
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    CODIGO_REQUISICAO_PERMISSAO
            );
        } else {
            // Android 5.1 e inferiores — concedida na instalação
            notificarERemover(true);
        }
    }

    // ==========================================================
    //  Resultados
    // ==========================================================

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != CODIGO_REQUISICAO_PERMISSAO) return;

        boolean concedida = temPermissao(getActivity());

        if (concedida) {
            LogApp.i("Permissão concedida pelo usuário (Android 6-10)");
            notificarERemover(true);
        } else {
            // Usuário negou — verifica se marcou "Nunca perguntar novamente"
            LogApp.w("Permissão negada pelo usuário");
            verificarSeNegouPermanentemente();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != CODIGO_REQUISICAO_GERENCIAR_STORAGE) return;

        boolean concedida = temPermissao(getActivity());

        if (concedida) {
            LogApp.i("Permissão concedida pelo usuário (Android 11+)");
            notificarERemover(true);
        } else {
            LogApp.w("Permissão negada pelo usuário (Android 11+)");
            notificarERemover(false);
        }
    }

    // ==========================================================
    //  Negação permanente (Android 6-10)
    // ==========================================================

    private void verificarSeNegouPermanentemente() {
        Activity activity = getActivity();
        if (activity == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean deveExplicar = shouldShowRequestPermissionRationale(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (deveExplicar) {
                // Ainda pode pedir de novo — só nega e segue
                notificarERemover(false);
            } else {
                // Negou permanentemente → manda para Configurações
                exibirDialogoAbrirConfiguracoes();
            }
        } else {
            notificarERemover(false);
        }
    }

    private void exibirDialogoAbrirConfiguracoes() {
        Activity activity = getActivity();
        if (activity == null) return;

        new AlertDialog.Builder(activity)
                .setTitle("Permissão necessária")
                .setMessage("Para a IDE funcionar, o acesso ao armazenamento é obrigatório.\n\n" +
                        "Toque em \"Abrir configurações\" e ative manualmente.")
                .setCancelable(false)
                .setPositiveButton("Abrir configurações", (d, w) -> {
                    abrirConfiguracoesDoApp();
                })
                .setNegativeButton("Cancelar", (d, w) -> {
                    notificarERemover(false);
                })
                .show();
    }

    private void abrirConfiguracoesDoApp() {
        Activity activity = getActivity();
        if (activity == null) return;

        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            startActivity(intent);
            Toast.makeText(activity,
                    "Ative as permissões e volte ao app.",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(activity,
                    "Não foi possível abrir as configurações.",
                    Toast.LENGTH_SHORT).show();
        }

        // Notifica como negado — a MainActivity vai decidir o que fazer
        notificarERemover(false);
    }

    // ==========================================================
    //  Cleanup
    // ==========================================================

    private void notificarERemover(boolean concedida) {
        if (callback != null) {
            callback.onResultado(concedida);
        }
        if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
        }
    }
}