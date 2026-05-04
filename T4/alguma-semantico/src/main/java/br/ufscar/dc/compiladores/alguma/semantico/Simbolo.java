package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Simbolo {
    public enum Categoria { VARIAVEL, CONSTANTE, TIPO, PROCEDIMENTO, FUNCAO }

    private final String nome;
    private final String tipo;
    private final Categoria categoria;
    private final Map<String, String> campos = new LinkedHashMap<>();
    private final List<String> parametros = new ArrayList<>();

    public Simbolo(String nome, String tipo, Categoria categoria) {
        this.nome = nome;
        this.tipo = tipo;
        this.categoria = categoria;
    }

    public String getNome()         { return nome; }
    public String getTipo()         { return tipo; }
    public Categoria getCategoria() { return categoria; }

    public void adicionaCampo(String nomeCampo, String tipoCampo) {
        campos.put(nomeCampo, tipoCampo);
    }

    public String getTipoCampo(String nomeCampo) {
        return campos.get(nomeCampo);
    }

    public boolean temCampos() {
        return !campos.isEmpty();
    }

    public void adicionaParametro(String tipoParametro) {
        parametros.add(tipoParametro);
    }

    public List<String> getParametros() {
        return parametros;
    }
}