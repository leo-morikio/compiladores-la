package br.ufscar.dc.compiladores.alguma.gerador;

import java.io.PrintWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import br.ufscar.dc.compiladores.AlgumaLexer;
import br.ufscar.dc.compiladores.AlgumaParser;
import br.ufscar.dc.compiladores.MeuErrorListener;

/**
 * Ponto de entrada do compilador LA → C.
 *
 * Uso:
 *   java -jar alguma-gerador.jar <arquivo-entrada> <arquivo-saida>
 *
 * Comportamento:
 *   - Se houver erro léxico ou sintático → imprime a mensagem de erro no arquivo de saída.
 *   - Se houver erro semântico           → imprime as mensagens de erro no arquivo de saída.
 *   - Se não houver erros               → gera código C no arquivo de saída.
 *
 * Não imprime nada no terminal (requisito do corretor automático).
 */
public class Principal {

    public static void main(String[] args) {
        // Valida argumentos obrigatórios
        if (args.length < 2) {
            System.err.println("Uso: java -jar alguma-gerador.jar <entrada> <saida>");
            return;
        }

        try (PrintWriter pw = new PrintWriter(args[1], "UTF-8")) {
            try {
                // --- Fase 1: Análise léxica + sintática ---
                CharStream cs = CharStreams.fromFileName(args[0]);

                AlgumaLexer lexer = new AlgumaLexer(cs);
                lexer.removeErrorListeners();
                lexer.addErrorListener(new MeuErrorListener());

                CommonTokenStream tokens = new CommonTokenStream(lexer);

                AlgumaParser parser = new AlgumaParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new MeuErrorListener());

                ParseTree tree = parser.programa();

                // --- Fase 2: Análise semântica ---
                AnalisadorSemantico semantico = new AnalisadorSemantico();
                ParseTreeWalker.DEFAULT.walk(semantico, tree);

                if (!semantico.getErros().isEmpty()) {
                    // Há erros semânticos → reporta-os (sem "Fim da compilacao")
                    for (String erro : semantico.getErros()) {
                        pw.println(erro);
                    }
                    return;
                }

                // --- Fase 3: Geração de código C ---
                GeradorDeCodigo gerador = new GeradorDeCodigo();
                String codigoC = gerador.visit(tree);
                pw.print(codigoC);

            } catch (RuntimeException e) {
                // Erro léxico ou sintático lançado pelo MeuErrorListener
                pw.println(e.getMessage());
            } catch (Exception e) {
                // Erro inesperado — silencioso para não poluir a saída
                System.err.println("Erro interno: " + e.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Erro ao abrir arquivo: " + e.getMessage());
        }
    }
}
