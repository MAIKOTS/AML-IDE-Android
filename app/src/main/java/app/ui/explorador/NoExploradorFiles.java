package ui.explorador;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoExploradorFiles {

    private final File arquivo;
    private final int nivel;
    private final NoExploradorFiles pai;

    private final List<NoExploradorFiles> filhos = new ArrayList<>();
    private boolean expandido = false;

    public NoExploradorFiles(File arquivo, int nivel, NoExploradorFiles pai) {
        this.arquivo = arquivo;
        this.nivel = nivel;
        this.pai = pai;
    }

    public File getArquivo()      { return arquivo; }
    public int getNivel()         { return nivel; }
    public NoExploradorFiles getPai() { return pai; }
    public List<NoExploradorFiles> getFilhos() { return filhos; }

    public boolean isPasta()      { return arquivo.isDirectory(); }
    public boolean isExpandido()  { return expandido; }
    public void setExpandido(boolean b) { this.expandido = b; }
    public void alternarExpansao() { this.expandido = !this.expandido; }

    public String getNome()       { return arquivo.getName(); }
    public long getTamanho()      { return arquivo.isFile() ? arquivo.length() : -1; }
    public int getTotalFilhos()   { return filhos.size(); }
}