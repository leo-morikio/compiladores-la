package br.ufscar.dc.compiladores.alguma.lexico;

import java.io.IOException;
import java.io.PrintWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import br.ufscar.dc.compiladores.AlgumaLexer;

public class Principal {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: programa <arquivo-entrada> <arquivo-saida>");
            return;
        }

        try {
            CharStream cs = CharStreams.fromFileName(args[0]);
            AlgumaLexer lex = new AlgumaLexer(cs);
            PrintWriter pw = new PrintWriter(args[1]);

            Token t = null;
            boolean erro = false;

            while ((t = lex.nextToken()).getType() != Token.EOF) {

                // Comentário não fechado
                if (t.getType() == AlgumaLexer.COMENTARIO_NAO_FECHADO) {
                    pw.printf("Linha %d: comentario nao fechado%n", t.getLine());
                    erro = true;
                    break;
                }

                // Cadeia não fechada na mesma linha
                if (t.getType() == AlgumaLexer.CADEIA_NAO_FECHADA) {
                    pw.printf("Linha %d: cadeia literal nao fechada%n", t.getLine());
                    erro = true;
                    break;
                }

                // Símbolo não identificado
                if (t.getType() == AlgumaLexer.ERRO) {
                    pw.printf("Linha %d: %s - simbolo nao identificado%n",
                            t.getLine(), t.getText());
                    erro = true;
                    break;
                }

                // Token válido
                String nomeToken = nomeToken(t);
                pw.printf("<'%s',%s>%n", t.getText(), nomeToken);
            }

            pw.close();

        } catch (IOException ex) {
            System.err.println("Erro ao ler arquivo: " + ex.getMessage());
        }
    }

    private static String nomeToken(Token t) {
        switch (t.getType()) {
            case AlgumaLexer.IDENT:     return "IDENT";
            case AlgumaLexer.NUM_INT:   return "NUM_INT";
            case AlgumaLexer.NUM_REAL:  return "NUM_REAL";
            case AlgumaLexer.CADEIA:    return "CADEIA";
            default:                    return "'" + t.getText() + "'";
        }
    }
}