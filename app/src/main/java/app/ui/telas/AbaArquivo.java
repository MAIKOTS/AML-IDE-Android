package ui.telas;

import java.io.File;
import java.util.Objects;

public class AbaArquivo {
    private final File arquivo;
    private String conteudoEmMemoria;
    private boolean modificado;

    public AbaArquivo(File arquivo, String conteudo) {
        this.arquivo = Objects.requireNonNull(arquivo, "arquivo nulo");
        this.conteudoEmMemoria = conteudo != null ? conteudo : "";
        this.modificado = false;
    }

    public File getArquivo() { return arquivo; }

    public String getConteudoEmMemoria() { return conteudoEmMemoria; }

    public void setConteudoEmMemoria(String conteudo) {
        this.conteudoEmMemoria = conteudo != null ? conteudo : "";
    }

    public boolean isModificado() { return modificado; }
    public void setModificado(boolean modificado) { this.modificado = modificado; }

    /** Nome exibido na aba. */
    public String getNome() { return arquivo.getName(); }
}