// Classe principal responsável por executar o analisador léxico
public class Principal {

    public static void main(String[] args) {

        // Verifica se os argumentos obrigatórios foram passados
        // args[0] = arquivo de entrada
        // args[1] = arquivo de saída
        if (args.length < 2) {
            System.err.println("Uso: programa <arquivo-entrada> <arquivo-saida>");
            return;
        }

        try {
            // Leitura do arquivo de entrada
            CharStream cs = CharStreams.fromFileName(args[0]);

            // Inicialização do lexer gerado pelo ANTLR
            AlgumaLexer lex = new AlgumaLexer(cs);

            // Escrita no arquivo de saída
            PrintWriter pw = new PrintWriter(args[1]);

            Token t = null;

            // Percorre todos os tokens até EOF
            while ((t = lex.nextToken()).getType() != Token.EOF) {

                // Tratamento de comentário não fechado
                if (t.getType() == AlgumaLexer.COMENTARIO_NAO_FECHADO) {
                    pw.printf("Linha %d: comentario nao fechado%n", t.getLine());
                    break;
                }

                // Tratamento de cadeia não fechada
                if (t.getType() == AlgumaLexer.CADEIA_NAO_FECHADA) {
                    pw.printf("Linha %d: cadeia literal nao fechada%n", t.getLine());
                    break;
                }

                // Tratamento de símbolo inválido
                if (t.getType() == AlgumaLexer.ERRO) {
                    pw.printf("Linha %d: %s - simbolo nao identificado%n",
                            t.getLine(), t.getText());
                    break;
                }

                // Impressão do token válido no formato exigido
                String nomeToken = nomeToken(t);
                pw.printf("<'%s',%s>%n", t.getText(), nomeToken);
            }

            pw.close();

        } catch (IOException ex) {
            System.err.println("Erro ao ler arquivo: " + ex.getMessage());
        }
    }

    // Método auxiliar para mapear tipos de token para nomes esperados na saída
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