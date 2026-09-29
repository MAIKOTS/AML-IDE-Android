package modelo;

import java.io.File;

public class Dependencia {
    public String url;            // https://github.com/user/repo.git
    public String branch;         // main, v1.0, ou null
    public String nomeRepo;       // "repo" (extraído da URL)
    public File   pastaLocal;     // <projeto>/deps/<nomeRepo>/
    public boolean ehHeaderOnly;  // true se não precisa linkar

    public boolean jaBaixada() {
        return pastaLocal != null && pastaLocal.isDirectory()
                && new File(pastaLocal, ".git").exists();
    }
}