package app.util;

import android.app.Activity;
import android.widget.Toast;

import globalclass.util.PermissionFragment;

/**
 * Solicita as permissões necessárias para o app.
 */
public final class Permissoes {

    private Permissoes() { }

    /** Pede permissão de armazenamento; avisa se negada. */
    public static void solicitarArmazenamento(Activity atividade) {
        if (atividade == null) return;

        PermissionFragment.solicitar(atividade, concedida -> {
            if (!concedida) {
                Toast.makeText(atividade,
                        "Sem permissão de armazenamento o app não pode criar projetos.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}