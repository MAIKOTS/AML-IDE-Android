package modelo;

/**
 * Representa um recurso baixável (sysroot, clang-runtime, etc).
 * Vem do manifest.json hospedado no GitHub.
 */
public class RecursoRemoto {

    public String  id;            // "sysroot", "lib"
    public String  nome;          // "Sysroot"
    public String  descricao;     // descrição curta
    public String  url;           // URL completa do .zip
    public long    tamanhoBytes;  // tamanho do .zip
    public String  sha256;        // hash esperado
    public String  destino;       // pasta de destino em files/
    public boolean obrigatorio;   // essencial?
}