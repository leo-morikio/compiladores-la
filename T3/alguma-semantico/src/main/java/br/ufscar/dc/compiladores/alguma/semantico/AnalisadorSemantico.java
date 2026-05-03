package br.ufscar.dc.compiladores.alguma.semantico;

import br.ufscar.dc.compiladores.AlgumaBaseListener;
import br.ufscar.dc.compiladores.AlgumaParser;
import org.antlr.v4.runtime.Token;
import java.util.ArrayList;
import java.util.List;

public class AnalisadorSemantico extends AlgumaBaseListener {

    private final Escopos escopos = new Escopos();
    private final List<String> erros = new ArrayList<>();

    private void erro(Token token, String msg) {
        erros.add("Linha " + token.getLine() + ": " + msg);
    }

    public List<String> getErros() { return erros; }

    // -------------------------------------------------------
    // Escopo global
    // -------------------------------------------------------

    @Override
    public void enterPrograma(AlgumaParser.ProgramaContext ctx) {
        escopos.empurrar();
    }

    @Override
    public void exitPrograma(AlgumaParser.ProgramaContext ctx) {
        escopos.desempilhar();
    }

    // -------------------------------------------------------
    // declaracao_local
    //   → 'declare' variavel          (tratado em enterVariavel)
    //   | 'constante' IDENT ':' tipo_basico '=' valor_constante
    //   | 'tipo' IDENT ':' tipo
    // -------------------------------------------------------

    @Override
    public void enterDeclaracao_local(AlgumaParser.Declaracao_localContext ctx) {
        // Caso 'constante'
        if (ctx.tipo_basico() != null) {
            String nome = ctx.IDENT().getText();
            String tipo = ctx.tipo_basico().getText();
            declarar(nome, tipo, Simbolo.Categoria.CONSTANTE, ctx.IDENT().getSymbol());
        }
        // Caso 'tipo'
        else if (ctx.tipo() != null && ctx.IDENT() != null) {
            String nome = ctx.IDENT().getText();
            // O tipo referenciado deve existir (ex: tipo Ponto : registro {...})
            verificarTipo(ctx.tipo());
            declarar(nome, nome, Simbolo.Categoria.TIPO, ctx.IDENT().getSymbol());
        }
        // Caso 'declare variavel' → tratado em enterVariavel
    }

    // -------------------------------------------------------
    // variavel → identificador {',' identificador} ':' tipo
    //
    // Ignoramos campos de registro (escopo do tipo, não do programa)
    // -------------------------------------------------------

    @Override
    public void enterVariavel(AlgumaParser.VariavelContext ctx) {
        // Campos de registro não entram na tabela de símbolos global
        if (ctx.parent instanceof AlgumaParser.RegistroContext) return;

        verificarTipo(ctx.tipo()); // erro tipo não declarado

        String tipoTexto = resolverNomeTipo(ctx.tipo());

        for (var idCtx : ctx.identificador()) {
            // IDENT(0) é sempre o nome base do identificador
            String nome = idCtx.IDENT(0).getText();
            declarar(nome, tipoTexto, Simbolo.Categoria.VARIAVEL, idCtx.IDENT(0).getSymbol());
        }
    }

    // -------------------------------------------------------
    // declaracao_global
    //   → 'procedimento' IDENT '(' ... ')' ...
    //   | 'funcao' IDENT '(' ... ')' ':' tipo_estendido ...
    // -------------------------------------------------------

    @Override
    public void enterDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        Token token = ctx.IDENT().getSymbol();

        if (ctx.tipo_estendido() != null) {
            // funcao
            String tipoRet = resolverNomeTipoEstendido(ctx.tipo_estendido());
            declarar(nome, tipoRet, Simbolo.Categoria.FUNCAO, token);
        } else {
            // procedimento
            declarar(nome, "procedimento", Simbolo.Categoria.PROCEDIMENTO, token);
        }

        escopos.empurrar(); // novo escopo para parâmetros e corpo
    }

    @Override
    public void exitDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        escopos.desempilhar();
    }

    // -------------------------------------------------------
    // parametro → ['var'] identificador {',' identificador} ':' tipo_estendido
    // -------------------------------------------------------

    @Override
    public void enterParametro(AlgumaParser.ParametroContext ctx) {
        String tipoTexto = resolverNomeTipoEstendido(ctx.tipo_estendido());
        String tipoBase  = tipoTexto.startsWith("^") ? tipoTexto.substring(1) : tipoTexto;

        if (!escopos.tipoExiste(tipoBase)) {
            erro(ctx.tipo_estendido().getStart(), "tipo " + tipoBase + " nao declarado");
        }

        for (var idCtx : ctx.identificador()) {
            String nome = idCtx.IDENT(0).getText();
            declarar(nome, tipoTexto, Simbolo.Categoria.VARIAVEL, idCtx.IDENT(0).getSymbol());
        }
    }

    // -------------------------------------------------------
    // cmdLeia → 'leia' '(' identificador {',' identificador} ')'
    // -------------------------------------------------------

    @Override
    public void enterCmdLeia(AlgumaParser.CmdLeiaContext ctx) {
        for (var idCtx : ctx.identificador()) {
            checarDeclarado(idCtx);
        }
    }

    // -------------------------------------------------------
    // cmdEscreva → 'escreva' '(' expressao {',' expressao} ')'
    // -------------------------------------------------------

    @Override
    public void enterCmdEscreva(AlgumaParser.CmdEscrevaContext ctx) {
        for (var expCtx : ctx.expressao()) {
            tipoExpressao(expCtx);
        }
    }

    // -------------------------------------------------------
    // cmdAtribuicao → identificador '<-' expressao
    // (o '^' de deref. faz parte do identificador na sua gramática)
    // -------------------------------------------------------

    @Override
    public void enterCmdAtribuicao(AlgumaParser.CmdAtribuicaoContext ctx) {
        var idCtx = ctx.identificador();
        String nome = idCtx.IDENT(0).getText();
        boolean ehPonteiro = ctx.identificador().getText().startsWith("^");

        Simbolo s = escopos.buscar(nome);
        if (s == null) {
            erro(idCtx.getStart(), "identificador " + nome + " nao declarado");
            return;
        }

        String tipoDir = tipoExpressao(ctx.expressao());
        String tipoEsq = ehPonteiro ? "^" + s.getTipo() : s.getTipo();

        if (!compativeis(tipoEsq, tipoDir)) {
            erro(ctx.getStart(), "atribuicao nao compativel para " + nome);
        }
    }

    // -------------------------------------------------------
    // cmdChamada → IDENT '(' (expressao {',' expressao})? ')'
    // -------------------------------------------------------

    /**
     * Valida a regra semântica de atribuição.
     * Verifica se a variável receptora foi declarada e se o tipo da expressão
     * à direita é compatível com o tipo da variável à esquerda.
     * Trata especificamente a lógica de ponteiros (indicados por '^').
     */

    @Override
    public void enterCmdChamada(AlgumaParser.CmdChamadaContext ctx) {
        String nome = ctx.IDENT().getText();
        if (escopos.buscar(nome) == null) {
            erro(ctx.IDENT().getSymbol(), "identificador " + nome + " nao declarado");
        }
        for (var expCtx : ctx.expressao()) {
            tipoExpressao(expCtx);
        }
    }

    // cmdEnquanto → 'enquanto' expressao 'faca' cmd* 'fim_enquanto'
    @Override
    public void enterCmdEnquanto(AlgumaParser.CmdEnquantoContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    // cmdSe → 'se' expressao 'entao' ...
    @Override
    public void enterCmdSe(AlgumaParser.CmdSeContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    // cmdFaca → 'faca' cmd* 'ate' expressao
    @Override
    public void enterCmdFaca(AlgumaParser.CmdFacaContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    // cmdRetorne → 'retorne' expressao
    @Override
    public void enterCmdRetorne(AlgumaParser.CmdRetorneContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    // -------------------------------------------------------
    // Helpers — declaração
    // -------------------------------------------------------

    private void declarar(String nome, String tipo, Simbolo.Categoria cat, Token token) {
        if (escopos.existeNoEscopoAtual(nome)) {
            erro(token, "identificador " + nome + " ja declarado anteriormente");
        } else {
            escopos.adicionar(nome, new Simbolo(nome, tipo, cat));
        }
    }

    private void checarDeclarado(AlgumaParser.IdentificadorContext ctx) {
        String nome = ctx.IDENT(0).getText();
        if (escopos.buscar(nome) == null) {
            erro(ctx.getStart(), "identificador " + nome + " nao declarado");
        }
    }

    /** Extrai o nome de tipo a partir de uma regra 'tipo' */
    private String resolverNomeTipo(AlgumaParser.TipoContext ctx) {
        if (ctx.registro() != null)      return "registro";
        if (ctx.tipo_estendido() != null) return resolverNomeTipoEstendido(ctx.tipo_estendido());
        return "tipo_indefinido";
    }

    /** Extrai o nome de tipo a partir de 'tipo_estendido' (pode ter ^) */
    private String resolverNomeTipoEstendido(AlgumaParser.Tipo_estendidoContext ctx) {
        // tipo_estendido → '^'* tipo_basico_ident
        // getText() devolve algo como "inteiro", "^inteiro", "NomeTipo", etc.
        return ctx.getText();
    }

    /** Verifica se o tipo referenciado existe */
    private void verificarTipo(AlgumaParser.TipoContext ctx) {
        if (ctx.registro() != null) return; // campos verificados no uso

        if (ctx.tipo_estendido() != null) {
            String tipoTexto = ctx.tipo_estendido().getText();
            // Remove ponteiros (^) para chegar no tipo base
            String tipoBase = tipoTexto.replaceAll("\\^", "");
            if (!escopos.tipoExiste(tipoBase)) {
                erro(ctx.tipo_estendido().getStart(), "tipo " + tipoBase + " nao declarado");
            }
        }
    }

    // -------------------------------------------------------
    // Resolução de tipos — segue a hierarquia da gramática
    // -------------------------------------------------------

    private String tipoExpressao(AlgumaParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() == 1)
            return tipoTermoLogico(ctx.termo_logico(0));
        for (var tl : ctx.termo_logico()) tipoTermoLogico(tl);
        return "logico";
    }

    private String tipoTermoLogico(AlgumaParser.Termo_logicoContext ctx) {
        if (ctx.fator_logico().size() == 1)
            return tipoFatorLogico(ctx.fator_logico(0));
        for (var fl : ctx.fator_logico()) tipoFatorLogico(fl);
        return "logico";
    }

    private String tipoFatorLogico(AlgumaParser.Fator_logicoContext ctx) {
        return tipoParcelaLogica(ctx.parcela_logica());
    }

    private String tipoParcelaLogica(AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() == null) return "logico"; // verdadeiro | falso
        return tipoExpRelacional(ctx.exp_relacional());
    }

    private String tipoExpRelacional(AlgumaParser.Exp_relacionalContext ctx) {
        String t1 = tipoExpAritmetica(ctx.exp_aritmetica(0));
        if (ctx.exp_aritmetica().size() == 1) return t1;
        tipoExpAritmetica(ctx.exp_aritmetica(1));
        return "logico"; // comparação → logico
    }

    private String tipoExpAritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        String tipo = tipoTermo(ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++)
            tipo = combinarArit(tipo, tipoTermo(ctx.termo(i)));
        return tipo;
    }

    private String tipoTermo(AlgumaParser.TermoContext ctx) {
        String tipo = tipoFator(ctx.fator(0));
        for (int i = 1; i < ctx.fator().size(); i++)
            tipo = combinarArit(tipo, tipoFator(ctx.fator(i)));
        return tipo;
    }

    private String tipoFator(AlgumaParser.FatorContext ctx) {
        String tipo = tipoParcela(ctx.parcela(0));
        for (int i = 1; i < ctx.parcela().size(); i++)
            tipo = combinarArit(tipo, tipoParcela(ctx.parcela(i)));
        return tipo;
    }

    // parcela → op_unario? parcela_unario | parcela_nao_unario
    private String tipoParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null)     return tipoParcelaUnario(ctx.parcela_unario());
        if (ctx.parcela_nao_unario() != null) return tipoParcelaNaoUnario(ctx.parcela_nao_unario());
        return "tipo_indefinido";
    }

    // parcela_unario
    //   : identificador                               ← alt 1
    //   | identificador '(' (expressao,*)? ')'       ← alt 2 (chamada de func)
    //   | NUM_INT                                     ← alt 3
    //   | NUM_REAL                                    ← alt 4
    //   | '(' expressao ')'                          ← alt 5
    private String tipoParcelaUnario(AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null)  return "inteiro";
        if (ctx.NUM_REAL() != null) return "real";

        // Alt 5: '(' expressao ')'
        // identificador é null, mas tem expressao
        if (ctx.identificador() == null) {
            return tipoExpressao(ctx.expressao(0));
        }

        String nome = ctx.identificador().IDENT(0).getText();

        // Alt 2: chamada de função → tem '(' logo após o identificador
        // Detectamos pelo número de filhos (identificador + '(' + ... + ')')
        if (ctx.getChildCount() > 1 && ctx.getChild(1).getText().equals("(")) {
            Simbolo s = escopos.buscar(nome);
            if (s == null) {
                erro(ctx.identificador().getStart(), "identificador " + nome + " nao declarado");
            }
            for (var expCtx : ctx.expressao()) tipoExpressao(expCtx);
            return s != null ? s.getTipo() : "tipo_indefinido";
        }

        // Alt 1: identificador simples
        Simbolo s = escopos.buscar(nome);
        if (s == null) {
            erro(ctx.identificador().getStart(), "identificador " + nome + " nao declarado");
            return "tipo_indefinido";
        }
        return s.getTipo();
    }

    // parcela_nao_unario → '&' identificador | CADEIA
    private String tipoParcelaNaoUnario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) return "literal";
        // '&' identificador → endereço (ponteiro)
        String nome = ctx.identificador().IDENT(0).getText();
        Simbolo s = escopos.buscar(nome);
        if (s == null) {
            erro(ctx.identificador().getStart(), "identificador " + nome + " nao declarado");
            return "tipo_indefinido";
        }
        return "^" + s.getTipo();
    }

    // -------------------------------------------------------
    // Compatibilidade de tipos
    // -------------------------------------------------------

    /**
     * Regras de inferência de tipos para expressões aritméticas.
     * Define como dois tipos interagem em uma operação (ex: inteiro + real = real).
     * Retorna "tipo_indefinido" caso os tipos sejam incompatíveis para aritmética.
     */
    
    private String combinarArit(String t1, String t2) {
        if ("tipo_indefinido".equals(t1) || "tipo_indefinido".equals(t2)) return "tipo_indefinido";
        if (t1.equals(t2)) return t1;
        boolean n1 = t1.equals("inteiro") || t1.equals("real");
        boolean n2 = t2.equals("inteiro") || t2.equals("real");
        if (n1 && n2) return "real";
        return "tipo_indefinido";
    }

    /**
     * Regras do enunciado:
     *   ponteiro ← endereço      (^T ← ^T)
     *   (real|inteiro) ← (real|inteiro)
     *   literal ← literal
     *   logico ← logico
     *   registro ← registro (mesmo nome)
     */
    private boolean compativeis(String esq, String dir) {
        if ("tipo_indefinido".equals(dir)) return false;
        if (esq.equals(dir))              return true;
        boolean nE = esq.equals("inteiro") || esq.equals("real");
        boolean nD = dir.equals("inteiro") || dir.equals("real");
        return nE && nD;
    }
}