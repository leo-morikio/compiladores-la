package br.ufscar.dc.compiladores;

import java.io.IOException;
import java.io.PrintWriter;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class Principal {
    public static void main(String[] args) {
        // args[0] é o arquivo de entrada, args[1] é o de saída
        try (PrintWriter pw = new PrintWriter(args[1])) {
            CharStream cs = CharStreams.fromFileName(args[0]);
            
            // Criando o Lexer (usando a gramática que você salvou)
            AlgumaLexer lexer = new AlgumaLexer(cs);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            
            // Criando o Parser
            AlgumaParser parser = new AlgumaParser(tokens);

            // Adicionando o tradutor de erros que você criou
            parser.removeErrorListeners();
            parser.addErrorListener(new MeuErrorListener());

            // Começa a analisar pela regra principal da gramática
            parser.programa(); 
            
            pw.println("Fim da compilacao");

        } catch (RuntimeException e) {
            // Se o MeuErrorListener encontrar um erro, ele cai aqui
            try (PrintWriter pw = new PrintWriter(args[1])) {
                pw.println(e.getMessage());
                pw.println("Fim da compilacao");
            } catch (IOException ex) {
                System.err.println("Erro ao gravar arquivo: " + ex.getMessage());
            }
        } catch (IOException e) {
            System.err.println("Erro ao abrir arquivo: " + e.getMessage());
        }
    }
}
