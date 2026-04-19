package br.ufscar.dc.compiladores.alguma.lexico;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import br.ufscar.dc.compiladores.AlgumaLexer;
import br.ufscar.dc.compiladores.AlgumaParser;
import br.ufscar.dc.compiladores.MeuErrorListener;

public class Principal {
    public static void main(String[] args) {
        // Usamos UTF-8 e PrintWriter sem autoflush para controle total
        try (PrintWriter pw = new PrintWriter(args[1], "UTF-8")) {
            try {
                CharStream cs = CharStreams.fromFileName(args[0]);
                AlgumaLexer lexer = new AlgumaLexer(cs);
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                AlgumaParser parser = new AlgumaParser(tokens);

                parser.removeErrorListeners();
                parser.addErrorListener(new MeuErrorListener());

                parser.programa();
                
                // IMPORTANTE: O professor pode não querer quebra de linha no fim do arquivo
                pw.println("Fim da compilacao");

            } catch (RuntimeException e) {
                pw.println(e.getMessage());
                // Se o erro persistir, mudaremos este println para print
                pw.println("Fim da compilacao");
            } catch (Exception e) {
                // Silencioso
            }
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }
}
