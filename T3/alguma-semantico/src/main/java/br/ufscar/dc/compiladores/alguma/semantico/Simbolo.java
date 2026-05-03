package br.ufscar.dc.compiladores.alguma.semantico;

public class Simbolo {
    public enum Categoria { VARIAVEL, CONSTANTE, TIPO, PROCEDIMENTO, FUNCAO }

    private final String nome;
    private final String tipo;
    private final Categoria categoria;

    public Simbolo(String nome, String tipo, Categoria categoria) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
    }

    public String getNome()         { return nome; }
    public String getTipo()         { return tipo; }
    public Categoria getCategoria() { return categoria; }
}