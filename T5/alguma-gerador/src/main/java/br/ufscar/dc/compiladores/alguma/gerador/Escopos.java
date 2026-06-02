package br.ufscar.dc.compiladores.alguma.gerador;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Gerenciador de escopos (pilha de TabelaDeSimbolos).
 * O topo da pilha é sempre o escopo local atual;
 * a base representa o escopo global.
 */
public class Escopos {
    private final Deque<TabelaDeSimbolos> pilha = new ArrayDeque<>();

    /** Abre um novo escopo (empurra tabela vazia) */
    public void empurrar()    { pilha.push(new TabelaDeSimbolos()); }

    /** Fecha o escopo atual (descarta a tabela do topo) */
    public void desempilhar() { pilha.pop(); }

    /** Verifica se o nome já existe no escopo mais interno (detecta redeclaração) */
    public boolean existeNoEscopoAtual(String nome) {
        return !pilha.isEmpty() && pilha.peek().existe(nome);
    }

    /** Busca do escopo mais interno para o mais externo (visibilidade léxica) */
    public Simbolo buscar(String nome) {
        for (TabelaDeSimbolos t : pilha) {
            if (t.existe(nome)) return t.buscar(nome);
        }
        return null;
    }

    /** Adiciona símbolo no escopo atual */
    public void adicionar(String nome, Simbolo s) {
        if (!pilha.isEmpty()) pilha.peek().adicionar(nome, s);
    }

    /** Retorna true para tipos primitivos e tipos declarados pelo usuário */
    public boolean tipoExiste(String tipo) {
        switch (tipo) {
            case "inteiro": case "real": case "literal": case "logico": return true;
        }
        Simbolo s = buscar(tipo);
        return s != null && s.getCategoria() == Simbolo.Categoria.TIPO;
    }
}
