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
        
        // O ANTLR usa <EOF> para o fim do arquivo, mas o padrão do trabalho pede apenas EOF
        if (tText.equals("<EOF>")) {
            tText = "EOF";
        }
        
        // Lança uma exceção com a mensagem formatada para ser capturada na classe Principal
        throw new RuntimeException("Linha " + line + ": erro sintatico proximo a " + tText);
    }
}
