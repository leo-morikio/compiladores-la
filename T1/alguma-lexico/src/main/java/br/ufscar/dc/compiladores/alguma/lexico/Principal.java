package br.ufscar.dc.compiladores.alguma.lexico;

import java.io.PrintWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import br.ufscar.dc.compiladores.AlgumaLexer;
import br.ufscar.dc.compiladores.AlgumaParser;
import br.ufscar.dc.compiladores.MeuErrorListener;

public class Principal {
    public static void main(String[] args) {
        try (PrintWriter pw = new PrintWriter(args[1], "UTF-8")) {
            try {
                CharStream cs = CharStreams.fromFileName(args[0]);
                AlgumaLexer lexer = new AlgumaLexer(cs);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                AlgumaParser parser = new AlgumaParser(tokens);

                parser.removeErrorListeners();
                parser.addErrorListener(new MeuErrorListener());

                // Léxico também precisa do listener para pegar CADEIA_NAO_FECHADA etc.
                lexer.removeErrorListeners();
                lexer.addErrorListener(new MeuErrorListener());

                ParseTree tree = parser.programa();

                // Se chegou aqui, não houve erro léxico/sintático → roda semântico
                AnalisadorSemantico semantico = new AnalisadorSemantico();
                ParseTreeWalker.DEFAULT.walk(semantico, tree);

                for (String erro : semantico.getErros()) {
                    pw.println(erro);
                }

            } catch (RuntimeException e) {
                // Erro léxico ou sintático capturado pelo MeuErrorListener
                pw.println(e.getMessage());
            } catch (Exception e) {
                // silencioso
            }

            pw.println("Fim da compilacao");

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }
}