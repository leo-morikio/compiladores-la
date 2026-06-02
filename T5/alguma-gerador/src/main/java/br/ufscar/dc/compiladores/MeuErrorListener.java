package br.ufscar.dc.compiladores;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

/**
 * Listener de erros customizado para o lexer e o parser.
 * Converte os erros ANTLR no formato exigido pelo corretor automático.
 */
public class MeuErrorListener extends BaseErrorListener {
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {

        Token t = (Token) offendingSymbol;
        String tText = t.getText();

        // Fim de arquivo: substitui pelo token simbólico EOF
        if (t.getType() == Token.EOF) {
            tText = "EOF";
        }

        int tipo = t.getType();

        // Erros léxicos (T1)
        if (tipo == AlgumaLexer.CADEIA_NAO_FECHADA) {
            throw new RuntimeException("Linha " + line + ": cadeia literal nao fechada");
        } else if (tipo == AlgumaLexer.COMENTARIO_NAO_FECHADO) {
            throw new RuntimeException("Linha " + line + ": comentario nao fechado");
        } else if (tipo == AlgumaLexer.ERRO) {
            throw new RuntimeException("Linha " + line + ": " + tText + " - simbolo nao identificado");
        }

        // Erros sintáticos (T2)
        throw new RuntimeException("Linha " + line + ": erro sintatico proximo a " + tText);
    }
}
