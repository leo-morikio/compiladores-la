package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayDeque;
import java.util.Deque;


/**
 * Gerenciador de escopos para a análise semântica.
 * Utiliza uma Pilha (Deque) de Tabelas de Símbolos para controlar a visibilidade
 * das variáveis e funções. O topo da pilha sempre representa o escopo local atual,
 * enquanto a base da pilha representa o escopo global.
 */


// Pilha de escopos: o topo é sempre o escopo atual
public class Escopos {
    private final Deque<TabelaDeSimbolos> pilha = new ArrayDeque<>();

    public void empurrar()    { pilha.push(new TabelaDeSimbolos()); }
    public void desempilhar() { pilha.pop(); }

    /** Verifica se já existe no escopo atual (para detectar redeclaração) */
    public boolean existeNoEscopoAtual(String nome) {
        return !pilha.isEmpty() && pilha.peek().existe(nome);
    }

    /** Busca do escopo mais interno para o mais externo */
    public Simbolo buscar(String nome) {
        for (TabelaDeSimbolos t : pilha) {
            if (t.existe(nome)) return t.buscar(nome);
        }
        return null;
    }

    public void adicionar(String nome, Simbolo s) {
        if (!pilha.isEmpty()) pilha.peek().adicionar(nome, s);
    }

    /** Tipos builtin + tipos declarados pelo usuário */
    public boolean tipoExiste(String tipo) {
        switch (tipo) {
            case "inteiro": case "real": case "literal": case "logico": return true;
        }
        Simbolo s = buscar(tipo);
        return s != null && s.getCategoria() == Simbolo.Categoria.TIPO;
    }
}