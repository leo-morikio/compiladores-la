package br.ufscar.dc.compiladores.alguma.gerador;

import br.ufscar.dc.compiladores.AlgumaBaseListener;
import br.ufscar.dc.compiladores.AlgumaParser;
import org.antlr.v4.runtime.Token;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnalisadorSemantico extends AlgumaBaseListener {

    private final Escopos escopos = new Escopos();
    private final Deque<Simbolo> rotinas = new ArrayDeque<>();
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
        if (ctx.tipo_basico() != null) {
            String nome = ctx.IDENT().getText();
            String tipo = ctx.tipo_basico().getText();
            declarar(nome, tipo, Simbolo.Categoria.CONSTANTE, ctx.IDENT().getSymbol());
        } else if (ctx.tipo() != null && ctx.IDENT() != null) {
            String nome = ctx.IDENT().getText();
            verificarTipo(ctx.tipo());
            declarar(nome, nome, Simbolo.Categoria.TIPO, ctx.IDENT().getSymbol());
            if (ctx.tipo().registro() != null) {
                var tipoSim = escopos.buscar(nome);
                if (tipoSim != null) {
                    var campos = resolverCamposRegistro(ctx.tipo().registro());
                    campos.forEach(tipoSim::adicionaCampo);
                }
            }
        }
    }

    // -------------------------------------------------------
    // variavel → identificador {',' identificador} ':' tipo
    //
    // Ignoramos campos de registro (escopo do tipo, não do programa)
    // -------------------------------------------------------

    @Override
    public void enterVariavel(AlgumaParser.VariavelContext ctx) {
        if (ctx.parent instanceof AlgumaParser.RegistroContext) return;

        verificarTipo(ctx.tipo());
        String tipoTexto = resolverNomeTipo(ctx.tipo());

        for (var idCtx : ctx.identificador()) {
            String nome = idCtx.IDENT(0).getText();
            declarar(nome, tipoTexto, Simbolo.Categoria.VARIAVEL, idCtx.IDENT(0).getSymbol());
            if (ctx.tipo().registro() != null) {
                var simb = escopos.buscar(nome);
                if (simb != null) {
                    var campos = resolverCamposRegistro(ctx.tipo().registro());
                    campos.forEach(simb::adicionaCampo);
                }
            }
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
            String tipoRet = resolverNomeTipoEstendido(ctx.tipo_estendido());
            declarar(nome, tipoRet, Simbolo.Categoria.FUNCAO, token);
        } else {
            declarar(nome, "procedimento", Simbolo.Categoria.PROCEDIMENTO, token);
        }

        var rotina = escopos.buscar(nome);
        rotinas.push(rotina);
        escopos.empurrar();
    }

    @Override
    public void exitDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        rotinas.pop();
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
            if (!rotinas.isEmpty()) {
                rotinas.peek().adicionaParametro(tipoTexto);
            }
        }
    }

    // -------------------------------------------------------
    // cmdLeia → 'leia' '(' identificador {',' identificador} ')'
    // -------------------------------------------------------

    @Override
    public void enterCmdLeia(AlgumaParser.CmdLeiaContext ctx) {
        for (var idCtx : ctx.identificador()) {
            tipoIdentificador(idCtx);
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
        String tipoEsq = tipoIdentificador(idCtx);
        String tipoDir = tipoExpressao(ctx.expressao());

        if (!"tipo_indefinido".equals(tipoEsq) && !compativeis(tipoEsq, tipoDir)) {
            erro(ctx.getStart(), "atribuicao nao compativel para " + idCtx.getText());
        }
    }

    // -------------------------------------------------------
    // cmdChamada → IDENT '(' (expressao {',' expressao})? ')'
    // -------------------------------------------------------

    @Override
    public void enterCmdChamada(AlgumaParser.CmdChamadaContext ctx) {
        tipoChamada(ctx.IDENT().getText(), ctx.IDENT().getSymbol(), ctx.expressao());
    }

    @Override
    public void enterCmdEnquanto(AlgumaParser.CmdEnquantoContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    @Override
    public void enterCmdSe(AlgumaParser.CmdSeContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    @Override
    public void enterCmdFaca(AlgumaParser.CmdFacaContext ctx) {
        tipoExpressao(ctx.expressao());
    }

    @Override
    public void enterCmdRetorne(AlgumaParser.CmdRetorneContext ctx) {
        if (rotinas.isEmpty() || rotinas.peek().getCategoria() != Simbolo.Categoria.FUNCAO) {
            erro(ctx.getStart(), "comando retorne nao permitido nesse escopo");
        }
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
        tipoIdentificador(ctx);
    }

    private Map<String, String> resolverCamposRegistro(AlgumaParser.RegistroContext ctx) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (var varCtx : ctx.variavel()) {
            String tipoTexto = resolverNomeTipo(varCtx.tipo());
            for (var idCtx : varCtx.identificador()) {
                campos.put(idCtx.IDENT(0).getText(), tipoTexto);
            }
        }
        return campos;
    }

    private String resolverNomeTipo(AlgumaParser.TipoContext ctx) {
        if (ctx.registro() != null)      return "registro";
        if (ctx.tipo_estendido() != null) return resolverNomeTipoEstendido(ctx.tipo_estendido());
        return "tipo_indefinido";
    }

    private String resolverNomeTipoEstendido(AlgumaParser.Tipo_estendidoContext ctx) {
        return ctx.getText();
    }

    private void verificarTipo(AlgumaParser.TipoContext ctx) {
        if (ctx.registro() != null) return;

        if (ctx.tipo_estendido() != null) {
            String tipoTexto = ctx.tipo_estendido().getText();
            String tipoBase = tipoTexto.replaceAll("\\^", "");
            if (!escopos.tipoExiste(tipoBase)) {
                erro(ctx.tipo_estendido().getStart(), "tipo " + tipoBase + " nao declarado");
            }
        }
    }

    private String tipoIdentificador(AlgumaParser.IdentificadorContext ctx) {
        String texto = ctx.getText();
        int deref = 0;
        while (texto.startsWith("^")) {
            deref++;
            texto = texto.substring(1);
        }
        while (texto.endsWith("^")) {
            deref++;
            texto = texto.substring(0, texto.length() - 1);
        }

        String semIndices = texto.replaceAll("\\[.*?\\]", "");
        String[] partes = semIndices.split("\\.");
        String nome = partes[0];

        Simbolo s = escopos.buscar(nome);
        if (s == null) {
            erro(ctx.getStart(), "identificador " + ctx.getText() + " nao declarado");
            return "tipo_indefinido";
        }

        String tipo = s.getTipo();
        // Para procurar um tipo definido pelo usuário, remova ponteiros (^)
        String tipoBase = tipo.replaceAll("\\^", "");
        Simbolo tipoSim = s.temCampos() ? s : escopos.buscar(tipoBase);

        if (partes.length > 1) {
            for (int i = 1; i < partes.length; i++) {
                if (tipoSim != null && tipoSim.temCampos()) {
                    String campo = tipoSim.getTipoCampo(partes[i]);
                    if (campo == null) {
                        erro(ctx.getStart(), "identificador " + ctx.getText() + " nao declarado");
                        return "tipo_indefinido";
                    }
                    tipo = campo;
                    tipoBase = tipo.replaceAll("\\^", "");
                    tipoSim = escopos.buscar(tipoBase);
                } else {
                    erro(ctx.getStart(), "identificador " + ctx.getText() + " nao declarado");
                    return "tipo_indefinido";
                }
            }
        }

        for (int i = 0; i < deref; i++) {
            tipo = removerPonteiro(tipo);
            if ("tipo_indefinido".equals(tipo)) break;
        }

        return tipo;
    }

    private String tipoChamada(String nome, Token token, List<AlgumaParser.ExpressaoContext> expressoes) {
        List<String> tipos = new ArrayList<>();
        for (var expCtx : expressoes) {
            tipos.add(tipoExpressao(expCtx));
        }

        Simbolo s = escopos.buscar(nome);
        if (s == null) {
            erro(token, "identificador " + nome + " nao declarado");
            return "tipo_indefinido";
        }

        if (s.getCategoria() != Simbolo.Categoria.FUNCAO && s.getCategoria() != Simbolo.Categoria.PROCEDIMENTO) {
            return "tipo_indefinido";
        }

        if (!compativelParametros(s.getParametros(), tipos)) {
            erro(token, "incompatibilidade de parametros na chamada de " + nome);
        }

        return s.getCategoria() == Simbolo.Categoria.FUNCAO ? s.getTipo() : "tipo_indefinido";
    }

    private boolean compativelParametros(List<String> formais, List<String> reais) {
        if (formais.size() != reais.size()) return false;
        for (int i = 0; i < formais.size(); i++) {
            if (!formais.get(i).equals(reais.get(i))) return false;
        }
        return true;
    }

    private String removerPonteiro(String tipo) {
        if ("tipo_indefinido".equals(tipo)) return tipo;
        if (!tipo.startsWith("^")) return "tipo_indefinido";
        return tipo.substring(1);
    }

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
        if (ctx.exp_relacional() == null) return "logico";
        return tipoExpRelacional(ctx.exp_relacional());
    }

    private String tipoExpRelacional(AlgumaParser.Exp_relacionalContext ctx) {
        String t1 = tipoExpAritmetica(ctx.exp_aritmetica(0));
        if (ctx.exp_aritmetica().size() == 1) return t1;
        tipoExpAritmetica(ctx.exp_aritmetica(1));
        return "logico";
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

    private String tipoParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null)     return tipoParcelaUnario(ctx.parcela_unario());
        if (ctx.parcela_nao_unario() != null) return tipoParcelaNaoUnario(ctx.parcela_nao_unario());
        return "tipo_indefinido";
    }

    private String tipoParcelaUnario(AlgumaParser.Parcela_unarioContext ctx) {
        if (ctx.NUM_INT() != null)  return "inteiro";
        if (ctx.NUM_REAL() != null) return "real";

        if (ctx.identificador() == null) {
            return tipoExpressao(ctx.expressao(0));
        }

        String nome = ctx.identificador().IDENT(0).getText();

        if (ctx.getChildCount() > 1 && ctx.getChild(1).getText().equals("(")) {
            return tipoChamada(nome, ctx.identificador().getStart(), ctx.expressao());
        }

        return tipoIdentificador(ctx.identificador());
    }

    private String tipoParcelaNaoUnario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) return "literal";
        String tipo = tipoIdentificador(ctx.identificador());
        if ("tipo_indefinido".equals(tipo)) return tipo;
        return "^" + tipo;
    }

    private String combinarArit(String t1, String t2) {
        if ("tipo_indefinido".equals(t1) || "tipo_indefinido".equals(t2)) return "tipo_indefinido";
        if (t1.equals(t2)) return t1;
        boolean n1 = t1.equals("inteiro") || t1.equals("real");
        boolean n2 = t2.equals("inteiro") || t2.equals("real");
        if (n1 && n2) return "real";
        return "tipo_indefinido";
    }

    private boolean compativeis(String esq, String dir) {
        if ("tipo_indefinido".equals(dir)) return false;
        if (esq.equals(dir)) return true;
        boolean nE = esq.equals("inteiro") || esq.equals("real");
        boolean nD = dir.equals("inteiro") || dir.equals("real");
        return nE && nD;
    }
}
