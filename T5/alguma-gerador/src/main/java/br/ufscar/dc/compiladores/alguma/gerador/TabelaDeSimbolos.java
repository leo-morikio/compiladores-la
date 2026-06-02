package br.ufscar.dc.compiladores.alguma.gerador;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Representa um único escopo (global, função, procedimento…).
 * Cada escopo tem sua própria tabela nome → Símbolo.
 */
public class TabelaDeSimbolos {
    private final Map<String, Simbolo> tabela = new LinkedHashMap<>();

    public boolean existe(String nome)              { return tabela.containsKey(nome); }
    public void adicionar(String nome, Simbolo s)   { tabela.put(nome, s); }
    public Simbolo buscar(String nome)              { return tabela.get(nome); }
}
