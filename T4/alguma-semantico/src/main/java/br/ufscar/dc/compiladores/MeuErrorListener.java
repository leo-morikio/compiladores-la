package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

public class MeuErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, 
                            int line, int charPositionInLine, String msg, 
                            RecognitionException e) {
        
        Token t = (Token) offendingSymbol;
        String tText = t.getText();
        
        // Conversão técnica exata para o fim de arquivo
        if (t.getType() == Token.EOF) {
            tText = "EOF";
        }

        int tipo = t.getType();
        
        // 1. Erros do T1 (Léxicos)
        if (tipo == AlgumaLexer.CADEIA_NAO_FECHADA) {
            throw new RuntimeException("Linha " + line + ": cadeia literal nao fechada");
        } 
        else if (tipo == AlgumaLexer.COMENTARIO_NAO_FECHADO) {
            throw new RuntimeException("Linha " + line + ": comentario nao fechado");
        } 
        else if (tipo == AlgumaLexer.ERRO) {
            throw new RuntimeException("Linha " + line + ": " + tText + " - simbolo nao identificado");
        }

        // 2. Erros do T2 (Sintáticos)
        throw new RuntimeException("Linha " + line + ": erro sintatico proximo a " + tText);
    }
}
