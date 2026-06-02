package br.ufscar.dc.compiladores.alguma.gerador;

import br.ufscar.dc.compiladores.AlgumaBaseVisitor;
import br.ufscar.dc.compiladores.AlgumaParser;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerador de código C para a linguagem LA.
 *
 * Estratégia:
 *  - Percorre a AST usando o padrão Visitor (AlgumaBaseVisitor<String>).
 *  - Cada método visitXxx retorna o fragmento de código C equivalente.
 *  - O visitPrograma monta o arquivo completo: includes + declarações globais + main().
 *  - Mantém uma cópia simplificada da tabela de símbolos para saber os tipos
 *    das variáveis na hora de gerar scanf/printf com o formato correto.
 */
public class GeradorDeCodigo extends AlgumaBaseVisitor<String> {

    // Tabela de símbolos usada durante a geração (precisa saber tipo de cada var)
    private final Escopos escopos = new Escopos();

    // Pilha de símbolos de rotinas (funções/procedimentos) ativos — para saber tipo de retorno
    private final Deque<Simbolo> rotinaAtual = new ArrayDeque<>();

    // -----------------------------------------------------------------------
    // programa : declaracoes 'algoritmo' corpo 'fim_algoritmo' EOF
    // -----------------------------------------------------------------------
    @Override
    public String visitPrograma(AlgumaParser.ProgramaContext ctx) {
        escopos.empurrar(); // escopo global

        StringBuilder sb = new StringBuilder();
        sb.append("#include <stdio.h>\n");
        sb.append("#include <stdlib.h>\n");
        sb.append("#include <string.h>\n");
        sb.append("\n");

        // 1. Declarações globais (tipos, funções, procedimentos)
        // Precisamos processar em dois passos: primeiro registrar os símbolos,
        // depois emitir o código, para que funções possam ser chamadas antes
        // de serem definidas no corpo.
        String codigoDecl = visitDeclaracoes(ctx.declaracoes());
        if (!codigoDecl.isBlank()) {
            sb.append(codigoDecl);
            sb.append("\n");
        }

        // 2. Função main
        sb.append("int main() {\n");
        sb.append(visitCorpo(ctx.corpo()));
        sb.append("    return 0;\n");
        sb.append("}\n");

        escopos.desempilhar();
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // declaracoes : decl_local_global*
    // -----------------------------------------------------------------------
    @Override
    public String visitDeclaracoes(AlgumaParser.DeclaracoesContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (var dlg : ctx.decl_local_global()) {
            sb.append(visitDecl_local_global(dlg));
        }
        return sb.toString();
    }

    @Override
    public String visitDecl_local_global(AlgumaParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null)  return visitDeclaracao_local(ctx.declaracao_local());
        if (ctx.declaracao_global() != null) return visitDeclaracao_global(ctx.declaracao_global());
        return "";
    }

    // -----------------------------------------------------------------------
    // declaracao_local
    //   : 'declare' variavel
    //   | 'constante' IDENT ':' tipo_basico '=' valor_constante
    //   | 'tipo' IDENT ':' tipo
    // -----------------------------------------------------------------------
    @Override
    public String visitDeclaracao_local(AlgumaParser.Declaracao_localContext ctx) {
        // 'tipo' IDENT ':' tipo  →  typedef struct { ... } Nome;
        if (ctx.tipo() != null && ctx.IDENT() != null) {
            String nome = ctx.IDENT().getText();
            // Registra o tipo na tabela para geração posterior
            registrarTipoLocal(ctx);
            if (ctx.tipo().registro() != null) {
                return gerarTypedefRegistro(nome, ctx.tipo().registro());
            }
            // typedef de tipo_estendido (alias de ponteiro ou básico)
            String cTipo = converterTipo(ctx.tipo().tipo_estendido().getText());
            return "typedef " + cTipo + " " + nome + ";\n";
        }
        // 'constante' IDENT ':' tipo_basico '=' valor_constante
        if (ctx.tipo_basico() != null) {
            String nome  = ctx.IDENT().getText();
            String tipo  = ctx.tipo_basico().getText();
            String valor = ctx.valor_constante().getText();
            escopos.adicionar(nome, new Simbolo(nome, tipo, Simbolo.Categoria.CONSTANTE));
            String cTipo = converterTipo(tipo);
            if ("literal".equals(tipo)) {
                // constante string: char nome[80] = "...";
                return "char " + nome + "[80] = " + valor + ";\n";
            }
            return cTipo + " " + nome + " = " + valor + ";\n";
        }
        // 'declare' variavel  →  declaração de variável local
        if (ctx.variavel() != null) {
            return visitVariavel(ctx.variavel()) + "\n";
        }
        return "";
    }

    /**
     * Registra tipo ou constante na tabela de símbolos sem gerar código.
     * Usado para que referências posteriores no mesmo escopo sejam resolvidas.
     */
    private void registrarTipoLocal(AlgumaParser.Declaracao_localContext ctx) {
        String nome = ctx.IDENT().getText();
        Simbolo s = new Simbolo(nome, nome, Simbolo.Categoria.TIPO);
        if (ctx.tipo().registro() != null) {
            resolverCamposRegistro(ctx.tipo().registro())
                    .forEach(s::adicionaCampo);
        }
        escopos.adicionar(nome, s);
    }

    // -----------------------------------------------------------------------
    // variavel : identificador (',' identificador)* ':' tipo
    // -----------------------------------------------------------------------
    @Override
    public String visitVariavel(AlgumaParser.VariavelContext ctx) {
        // Campos de registro são tratados dentro do typedef — não geram código aqui
        if (ctx.parent instanceof AlgumaParser.RegistroContext) return "";

        String tipoLA  = resolverNomeTipo(ctx.tipo());
        StringBuilder sb = new StringBuilder();

        for (var idCtx : ctx.identificador()) {
            String nome = idCtx.IDENT(0).getText();

            // Registra na tabela de símbolos
            Simbolo s = new Simbolo(nome, tipoLA, Simbolo.Categoria.VARIAVEL);
            if (ctx.tipo().registro() != null) {
                resolverCamposRegistro(ctx.tipo().registro()).forEach(s::adicionaCampo);
            }
            escopos.adicionar(nome, s);

            // Gera declaração C
            sb.append("    ").append(gerarDeclaracaoVar(idCtx, ctx.tipo())).append(";\n");
        }
        return sb.toString();
    }

    /**
     * Gera "tipo nome" (ou "tipo nome[N]" para vetor, "tipo *nome" para ponteiro).
     * O identificador pode ter índice: x[10] → int x[10]
     */
    private String gerarDeclaracaoVar(AlgumaParser.IdentificadorContext idCtx,
                                      AlgumaParser.TipoContext tipoCtx) {
        String nome = idCtx.IDENT(0).getText();

        // Verifica se há dimensão de vetor na declaração (ex: v[10])
        // Na gramática a dimensão fica no identificador: '^'? IDENT ('[' exp_aritmetica ']')*
        boolean temIndice = idCtx.getChildCount() > 1;
        String indiceStr = "";
        if (temIndice) {
            // Percorre filhos procurando '[' exp ']'
            for (int i = 0; i < idCtx.getChildCount(); i++) {
                String txt = idCtx.getChild(i).getText();
                if (txt.equals("[")) {
                    // próximo filho é a expressão
                    String dim = idCtx.getChild(i + 1).getText();
                    indiceStr += "[" + dim + "]";
                    i += 2; // pula ']'
                }
            }
        }

        boolean isPonteiro = idCtx.getChild(0).getText().equals("^");

        if (tipoCtx.registro() != null) {
            // struct inline (declarado diretamente, não via 'tipo')
            // Neste caso o tipo é uma struct anônima — geramos struct { ... }
            StringBuilder campos = new StringBuilder("struct {\n");
            for (var varCtx : tipoCtx.registro().variavel()) {
                for (var id2 : varCtx.identificador()) {
                    campos.append("        ")
                          .append(gerarDeclaracaoVar(id2, varCtx.tipo()))
                          .append(";\n");
                }
            }
            campos.append("    } ").append(nome);
            return campos.toString();
        }

        String tipoLA = tipoCtx.tipo_estendido().getText();
        String cTipo  = converterTipo(tipoLA);

        // ponteiro declarado com '^' no identificador
        if (isPonteiro) {
            return cTipo + " *" + nome + indiceStr;
        }

        if ("literal".equals(tipoLA)) {
            // strings em LA → char nome[80]
            return "char " + nome + "[80]" + indiceStr;
        }

        return cTipo + " " + nome + indiceStr;
    }

    /**
     * Gera typedef struct para um tipo registro nomeado.
     * Resultado: typedef struct { int x; double y; } MeuTipo;
     */
    private String gerarTypedefRegistro(String nome, AlgumaParser.RegistroContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("typedef struct {\n");
        for (var varCtx : ctx.variavel()) {
            for (var idCtx : varCtx.identificador()) {
                sb.append("    ")
                  .append(gerarDeclaracaoVar(idCtx, varCtx.tipo()))
                  .append(";\n");
            }
        }
        sb.append("} ").append(nome).append(";\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // declaracao_global
    //   : 'procedimento' IDENT '(' parametros? ')' declaracao_local* cmd* 'fim_procedimento'
    //   | 'funcao' IDENT '(' parametros? ')' ':' tipo_estendido declaracao_local* cmd* 'fim_funcao'
    // -----------------------------------------------------------------------
    @Override
    public String visitDeclaracao_global(AlgumaParser.Declaracao_globalContext ctx) {
        String nome = ctx.IDENT().getText();
        boolean ehFuncao = ctx.tipo_estendido() != null;

        String tipoRetorno = ehFuncao ? converterTipo(ctx.tipo_estendido().getText()) : "void";

        // Registra no escopo externo
        Simbolo simb;
        if (ehFuncao) {
            simb = new Simbolo(nome, ctx.tipo_estendido().getText(), Simbolo.Categoria.FUNCAO);
        } else {
            simb = new Simbolo(nome, "procedimento", Simbolo.Categoria.PROCEDIMENTO);
        }
        escopos.adicionar(nome, simb);

        // Novo escopo para o corpo da rotina
        escopos.empurrar();
        rotinaAtual.push(simb);

        // Parâmetros
        StringBuilder params = new StringBuilder();
        if (ctx.parametros() != null) {
            params.append(visitParametros(ctx.parametros()));
        }

        // Corpo: declarações locais + comandos
        StringBuilder corpo = new StringBuilder();
        for (var dl : ctx.declaracao_local()) {
            corpo.append(visitDeclaracao_local(dl));
        }
        for (var cmd : ctx.cmd()) {
            corpo.append(visitCmd(cmd));
        }

        rotinaAtual.pop();
        escopos.desempilhar();

        return tipoRetorno + " " + nome + "(" + params + ") {\n" + corpo + "}\n\n";
    }

    // -----------------------------------------------------------------------
    // parametros : parametro (',' parametro)*
    // -----------------------------------------------------------------------
    @Override
    public String visitParametros(AlgumaParser.ParametrosContext ctx) {
        List<String> lista = new ArrayList<>();
        for (var p : ctx.parametro()) {
            lista.add(visitParametro(p));
        }
        return String.join(", ", lista);
    }

    // parametro : 'var'? identificador (',' identificador)* ':' tipo_estendido
    @Override
    public String visitParametro(AlgumaParser.ParametroContext ctx) {
        String tipoLA = ctx.tipo_estendido().getText();
        String cTipo  = converterTipo(tipoLA);
        boolean isVar = ctx.getChild(0).getText().equals("var");
        // 'var' em LA → passagem por referência → ponteiro em C
        String prefixo = isVar ? cTipo + " *" : cTipo + " ";
        if ("literal".equals(tipoLA)) prefixo = "char *";

        List<String> nomes = new ArrayList<>();
        for (var idCtx : ctx.identificador()) {
            String nome = idCtx.IDENT(0).getText();
            escopos.adicionar(nome, new Simbolo(nome, tipoLA, Simbolo.Categoria.VARIAVEL));
            if (!rotinaAtual.isEmpty()) rotinaAtual.peek().adicionaParametro(tipoLA);
            nomes.add(prefixo + nome);
        }
        return String.join(", ", nomes);
    }

    // -----------------------------------------------------------------------
    // corpo : declaracao_local* cmd*
    // -----------------------------------------------------------------------
    @Override
    public String visitCorpo(AlgumaParser.CorpoContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (var dl : ctx.declaracao_local()) {
            sb.append(visitDeclaracao_local(dl));
        }
        for (var cmd : ctx.cmd()) {
            sb.append(visitCmd(cmd));
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmd — despacha para o comando correto
    // -----------------------------------------------------------------------
    @Override
    public String visitCmd(AlgumaParser.CmdContext ctx) {
        if (ctx.cmdLeia()       != null) return visitCmdLeia(ctx.cmdLeia());
        if (ctx.cmdEscreva()    != null) return visitCmdEscreva(ctx.cmdEscreva());
        if (ctx.cmdSe()         != null) return visitCmdSe(ctx.cmdSe());
        if (ctx.cmdCaso()       != null) return visitCmdCaso(ctx.cmdCaso());
        if (ctx.cmdPara()       != null) return visitCmdPara(ctx.cmdPara());
        if (ctx.cmdEnquanto()   != null) return visitCmdEnquanto(ctx.cmdEnquanto());
        if (ctx.cmdFaca()       != null) return visitCmdFaca(ctx.cmdFaca());
        if (ctx.cmdAtribuicao() != null) return visitCmdAtribuicao(ctx.cmdAtribuicao());
        if (ctx.cmdChamada()    != null) return visitCmdChamada(ctx.cmdChamada());
        if (ctx.cmdRetorne()    != null) return visitCmdRetorne(ctx.cmdRetorne());
        return "";
    }

    // -----------------------------------------------------------------------
    // cmdLeia : 'leia' '(' identificador (',' identificador)* ')'
    //   → scanf("%d", &x);   /   gets(x);  para literal
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdLeia(AlgumaParser.CmdLeiaContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (var idCtx : ctx.identificador()) {
            String nome = textoIdentificador(idCtx);
            String tipo = tipoDoIdentificador(idCtx);
            String fmt  = formatoScanf(tipo);
            if ("literal".equals(tipo)) {
                // gets não precisa de &
                sb.append("    scanf(\"%[^\\n]\", ").append(nome).append(");\n");
            } else {
                sb.append("    scanf(\"").append(fmt).append("\", &").append(nome).append(");\n");
            }
        }
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmdEscreva : 'escreva' '(' expressao (',' expressao)* ')'
    //   → printf(...)
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdEscreva(AlgumaParser.CmdEscrevaContext ctx) {
        StringBuilder fmtSb = new StringBuilder();
        List<String> args = new ArrayList<>();

        for (var expCtx : ctx.expressao()) {
            String tipo = tipoExpressao(expCtx);
            fmtSb.append(formatoPrintf(tipo));
            args.add(gerarExpressao(expCtx));
        }

        String argStr = args.isEmpty() ? "" : ", " + String.join(", ", args);
        return "    printf(\"" + fmtSb + "\"" + argStr + ");\n";
    }

    // -----------------------------------------------------------------------
    // cmdSe : 'se' expressao 'entao' cmd* ('senao' cmd*)? 'fim_se'
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdSe(AlgumaParser.CmdSeContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("    if (").append(gerarExpressao(ctx.expressao())).append(") {\n");

        // cmds do 'entao' — até encontrar o 'senao' ou 'fim_se'
        // Na gramática os cmds do then e do else ficam misturados; usamos o texto
        // para identificar a posição do token 'senao'
        boolean temSenao = ctx.getText().contains("senao");
        List<AlgumaParser.CmdContext> cmdsThen = new ArrayList<>();
        List<AlgumaParser.CmdContext> cmdsElse = new ArrayList<>();

        // A gramática define: 'se' expressao 'entao' cmd* ('senao' cmd*)? 'fim_se'
        // Os cmds do then e else estão listados em ctx.cmd()
        // Identificamos a posição do token 'senao' pelo índice do filho
        int idxSenao = -1;
        for (int i = 0; i < ctx.getChildCount(); i++) {
            if (ctx.getChild(i).getText().equals("senao")) {
                idxSenao = i;
                break;
            }
        }

        for (var cmdCtx : ctx.cmd()) {
            int pos = ctx.children.indexOf(cmdCtx);
            if (idxSenao < 0 || pos < idxSenao) {
                cmdsThen.add(cmdCtx);
            } else {
                cmdsElse.add(cmdCtx);
            }
        }

        for (var c : cmdsThen) sb.append("    ").append(visitCmd(c).stripLeading());
        sb.append("    }");

        if (temSenao) {
            sb.append(" else {\n");
            for (var c : cmdsElse) sb.append("    ").append(visitCmd(c).stripLeading());
            sb.append("    }");
        }
        sb.append("\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmdCaso : 'caso' exp_aritmetica 'seja' selecao ('senao' cmd*)? 'fim_caso'
    //   → switch/case em C
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdCaso(AlgumaParser.CmdCasoContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("    switch (").append(gerarExpAritmetica(ctx.exp_aritmetica())).append(") {\n");
        sb.append(visitSelecao(ctx.selecao()));

        // senao → default
        if (ctx.cmd() != null && !ctx.cmd().isEmpty()) {
            sb.append("        default:\n");
            for (var c : ctx.cmd()) sb.append("    ").append(visitCmd(c).stripLeading());
            sb.append("            break;\n");
        }
        sb.append("    }\n");
        return sb.toString();
    }

    @Override
    public String visitSelecao(AlgumaParser.SelecaoContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (var item : ctx.item_selecao()) {
            sb.append(visitItem_selecao(item));
        }
        return sb.toString();
    }

    @Override
    public String visitItem_selecao(AlgumaParser.Item_selecaoContext ctx) {
        StringBuilder sb = new StringBuilder();
        // constantes : numero_intervalo (',' numero_intervalo)*
        for (var ni : ctx.constantes().numero_intervalo()) {
            String ini = gerarNumeroIntervaloInicio(ni);
            String fim = gerarNumeroIntervaloFim(ni);
            if (fim == null) {
                sb.append("        case ").append(ini).append(":\n");
            } else {
                // intervalo a..b → case a: case a+1: ... case b:
                // Em C não há range nativo; geramos cada valor com um loop de inteiros
                // Se os valores forem literais numéricos simples usamos um goto-trick
                // A solução mais portável é gerar um if dentro de default,
                // mas aqui simplesmente listamos os cases quando possível.
                // Para compiladores LA dos casos de teste os intervalos são pequenos.
                try {
                    int a = Integer.parseInt(ini);
                    int b = Integer.parseInt(fim);
                    for (int v = a; v <= b; v++) {
                        sb.append("        case ").append(v).append(":\n");
                    }
                } catch (NumberFormatException ex) {
                    // fallback: apenas o case inicial
                    sb.append("        case ").append(ini).append(":\n");
                }
            }
        }
        for (var c : ctx.cmd()) sb.append("    ").append(visitCmd(c).stripLeading());
        sb.append("            break;\n");
        return sb.toString();
    }

    private String gerarNumeroIntervaloInicio(AlgumaParser.Numero_intervaloContext ctx) {
        String sinal = (ctx.op_unario(0) != null) ? ctx.op_unario(0).getText() : "";
        return sinal + ctx.NUM_INT(0).getText();
    }

    private String gerarNumeroIntervaloFim(AlgumaParser.Numero_intervaloContext ctx) {
        // intervalo tem '..' → dois NUM_INT
        if (ctx.NUM_INT().size() < 2) return null;
        String sinal = (ctx.op_unario().size() > 1) ? ctx.op_unario(1).getText() : "";
        return sinal + ctx.NUM_INT(1).getText();
    }

    // -----------------------------------------------------------------------
    // cmdPara : 'para' IDENT '<-' exp_aritmetica 'ate' exp_aritmetica 'faca' cmd* 'fim_para'
    //   → for (i = ini; i <= fim; i++) { ... }
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdPara(AlgumaParser.CmdParaContext ctx) {
        String var = ctx.IDENT().getText();
        String ini = gerarExpAritmetica(ctx.exp_aritmetica(0));
        String fim = gerarExpAritmetica(ctx.exp_aritmetica(1));

        StringBuilder sb = new StringBuilder();
        sb.append("    for (").append(var).append(" = ").append(ini)
          .append("; ").append(var).append(" <= ").append(fim)
          .append("; ").append(var).append("++) {\n");
        for (var c : ctx.cmd()) sb.append("    ").append(visitCmd(c).stripLeading());
        sb.append("    }\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmdEnquanto : 'enquanto' expressao 'faca' cmd* 'fim_enquanto'
    //   → while (...) { ... }
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdEnquanto(AlgumaParser.CmdEnquantoContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("    while (").append(gerarExpressao(ctx.expressao())).append(") {\n");
        for (var c : ctx.cmd()) sb.append("    ").append(visitCmd(c).stripLeading());
        sb.append("    }\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmdFaca : 'faca' cmd* 'ate' expressao
    //   → do { ... } while (!(...));
    //   Semântica LA: faca...ate repete ATÉ a condição ser verdadeira,
    //   equivalente a do...while(!cond) em C.
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdFaca(AlgumaParser.CmdFacaContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("    do {\n");
        for (var c : ctx.cmd()) sb.append("    ").append(visitCmd(c).stripLeading());
        sb.append("    } while (").append(gerarExpressao(ctx.expressao())).append(");\n");
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // cmdAtribuicao : identificador '<-' expressao
    //   → ident = expr;
    //   Cuidado especial para literal: usa strcpy
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdAtribuicao(AlgumaParser.CmdAtribuicaoContext ctx) {
        String lhs   = textoIdentificador(ctx.identificador());
        String tipo  = tipoDoIdentificador(ctx.identificador());
        String rhs   = gerarExpressao(ctx.expressao());

        if ("literal".equals(tipo)) {
            return "    strcpy(" + lhs + ", " + rhs + ");\n";
        }

        // Ponteiro com ^ no lado esquerdo: *p <- expr  →  *lhs = rhs
        return "    " + lhs + " = " + rhs + ";\n";
    }

    // -----------------------------------------------------------------------
    // cmdChamada : IDENT '(' (expressao (',' expressao)*)? ')'
    //   → nome(args);
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdChamada(AlgumaParser.CmdChamadaContext ctx) {
        List<String> args = new ArrayList<>();
        for (var exp : ctx.expressao()) args.add(gerarExpressao(exp));
        return "    " + ctx.IDENT().getText() + "(" + String.join(", ", args) + ");\n";
    }

    // -----------------------------------------------------------------------
    // cmdRetorne : 'retorne' expressao
    //   → return expr;
    // -----------------------------------------------------------------------
    @Override
    public String visitCmdRetorne(AlgumaParser.CmdRetorneContext ctx) {
        return "    return " + gerarExpressao(ctx.expressao()) + ";\n";
    }

    // =======================================================================
    // Geração de expressões — retornam string C
    // =======================================================================

    private String gerarExpressao(AlgumaParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() == 1) return gerarTermoLogico(ctx.termo_logico(0));
        List<String> partes = new ArrayList<>();
        for (var tl : ctx.termo_logico()) partes.add(gerarTermoLogico(tl));
        return String.join(" || ", partes);
    }

    private String gerarTermoLogico(AlgumaParser.Termo_logicoContext ctx) {
        if (ctx.fator_logico().size() == 1) return gerarFatorLogico(ctx.fator_logico(0));
        List<String> partes = new ArrayList<>();
        for (var fl : ctx.fator_logico()) partes.add(gerarFatorLogico(fl));
        return String.join(" && ", partes);
    }

    private String gerarFatorLogico(AlgumaParser.Fator_logicoContext ctx) {
        // 'nao'? parcela_logica
        boolean negado = ctx.getChild(0).getText().equals("nao");
        String parcela = gerarParcelaLogica(ctx.parcela_logica());
        return negado ? "!(" + parcela + ")" : parcela;
    }

    private String gerarParcelaLogica(AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() != null) return gerarExpRelacional(ctx.exp_relacional());
        // 'verdadeiro' | 'falso'
        return ctx.getText().equals("verdadeiro") ? "1" : "0";
    }

    private String gerarExpRelacional(AlgumaParser.Exp_relacionalContext ctx) {
        String e1 = gerarExpAritmetica(ctx.exp_aritmetica(0));
        if (ctx.exp_aritmetica().size() == 1) return e1;
        String op = ctx.op_relacional().getText();
        String e2 = gerarExpAritmetica(ctx.exp_aritmetica(1));
        // LA usa '=' para igualdade; em C é '=='
        if (op.equals("="))  op = "==";
        if (op.equals("<>")) op = "!=";
        return e1 + " " + op + " " + e2;
    }

    private String gerarExpAritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        StringBuilder sb = new StringBuilder(gerarTermo(ctx.termo(0)));
        for (int i = 1; i < ctx.termo().size(); i++) {
            sb.append(" ").append(ctx.op1(i - 1).getText()).append(" ")
              .append(gerarTermo(ctx.termo(i)));
        }
        return sb.toString();
    }

    private String gerarTermo(AlgumaParser.TermoContext ctx) {
        StringBuilder sb = new StringBuilder(gerarFator(ctx.fator(0)));
        for (int i = 1; i < ctx.fator().size(); i++) {
            sb.append(" ").append(ctx.op2(i - 1).getText()).append(" ")
              .append(gerarFator(ctx.fator(i)));
        }
        return sb.toString();
    }

    private String gerarFator(AlgumaParser.FatorContext ctx) {
        StringBuilder sb = new StringBuilder(gerarParcela(ctx.parcela(0)));
        for (int i = 1; i < ctx.parcela().size(); i++) {
            sb.append(" ").append(ctx.op3(i - 1).getText()).append(" ")
              .append(gerarParcela(ctx.parcela(i)));
        }
        return sb.toString();
    }

    private String gerarParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario()     != null) return gerarParcelaUnario(ctx.parcela_unario(), ctx.op_unario());
        if (ctx.parcela_nao_unario() != null) return gerarParcelaNaoUnario(ctx.parcela_nao_unario());
        return "";
    }

    private String gerarParcelaUnario(AlgumaParser.Parcela_unarioContext ctx,
                                       AlgumaParser.Op_unarioContext opCtx) {
        String prefixo = (opCtx != null) ? opCtx.getText() : "";

        if (ctx.NUM_INT()  != null) return prefixo + ctx.NUM_INT().getText();
        if (ctx.NUM_REAL() != null) return prefixo + ctx.NUM_REAL().getText();

        // '(' expressao ')'
        if (ctx.identificador() == null) {
            return prefixo + "(" + gerarExpressao(ctx.expressao(0)) + ")";
        }

        // chamada de função: ident '(' args ')'
        if (ctx.getChildCount() > 1 && ctx.getChild(1).getText().equals("(")) {
            List<String> args = new ArrayList<>();
            for (var e : ctx.expressao()) args.add(gerarExpressao(e));
            return prefixo + ctx.identificador().getText()
                   + "(" + String.join(", ", args) + ")";
        }

        // identificador simples ou acesso a campo/vetor
        return prefixo + textoIdentificador(ctx.identificador());
    }

    private String gerarParcelaNaoUnario(AlgumaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) return ctx.CADEIA().getText();
        // '&' identificador → endereço de variável
        return "&" + textoIdentificador(ctx.identificador());
    }

    // =======================================================================
    // Utilitários
    // =======================================================================

    /**
     * Converte um nome de tipo LA para o tipo C equivalente.
     * Trata ponteiros (^tipo → tipo*) e tipos compostos.
     */
    private String converterTipo(String tipoLA) {
        // Remove aspas se houver
        switch (tipoLA) {
            case "inteiro":  return "int";
            case "real":     return "double";
            case "literal":  return "char"; // literal → char[80], mas aqui retornamos o base
            case "logico":   return "int";  // logico   → int (0/1)
        }
        // Ponteiro: ^tipo
        if (tipoLA.startsWith("^")) {
            return converterTipo(tipoLA.substring(1)) + "*";
        }
        // Tipo definido pelo usuário (struct/typedef)
        return tipoLA;
    }

    /**
     * Retorna o especificador de formato para scanf.
     */
    private String formatoScanf(String tipoLA) {
        switch (tipoLA) {
            case "inteiro": return "%d";
            case "real":    return "%lf";
            case "literal": return "%s";
            default:        return "%d";
        }
    }

    /**
     * Retorna o especificador de formato para printf.
     */
    private String formatoPrintf(String tipoLA) {
        switch (tipoLA) {
            case "inteiro": return "%d";
            case "real":    return "%f";   // %g remove zeros desnecessários
            case "literal": return "%s";
            case "logico":  return "%d";
            default:        return "%s";
        }
    }

    /**
     * Gera o texto C de um identificador (resolve acessos a campos e índices).
     * Ex: p.x → p.x ; v[i] → v[i] ; ^p → *p
     */
    private String textoIdentificador(AlgumaParser.IdentificadorContext ctx) {
        StringBuilder sb = new StringBuilder();
        boolean deref = ctx.getChild(0).getText().equals("^");
        if (deref) sb.append("*");

        sb.append(ctx.IDENT(0).getText());

        // Filhos adicionais: '.campo' ou '[exp]'
        int inicio = deref ? 2 : 1;
        for (int i = inicio; i < ctx.getChildCount(); i++) {
            String filho = ctx.getChild(i).getText();
            if (filho.equals(".")) {
                sb.append(".");
            } else if (filho.equals("[")) {
                sb.append("[");
            } else if (filho.equals("]")) {
                sb.append("]");
            } else if (!filho.equals("^")) {
                sb.append(filho);
            }
        }
        return sb.toString();
    }

    /**
     * Tenta descobrir o tipo de um identificador consultando a tabela de símbolos.
     */
    private String tipoDoIdentificador(AlgumaParser.IdentificadorContext ctx) {
        String nome = ctx.IDENT(0).getText();
        Simbolo s = escopos.buscar(nome);
        if (s == null) return "tipo_indefinido";

        String tipo = s.getTipo();
        // Acesso a campo de registro: p.campo → tipo do campo
        String texto = ctx.getText();
        if (texto.contains(".")) {
            String[] partes = texto.replaceAll("\\[.*?\\]", "").split("\\.");
            Simbolo atual = s;
            for (int i = 1; i < partes.length; i++) {
                if (atual == null) return "tipo_indefinido";
                tipo = atual.getTipoCampo(partes[i]);
                if (tipo == null) {
                    // tenta buscar no tipo referenciado
                    Simbolo tipoSim = escopos.buscar(atual.getTipo());
                    tipo = tipoSim != null ? tipoSim.getTipoCampo(partes[i]) : "tipo_indefinido";
                }
                atual = escopos.buscar(tipo != null ? tipo : "");
            }
        }
        return tipo != null ? tipo : "tipo_indefinido";
    }

    /**
     * Infere o tipo de uma expressão (usado para determinar formato do printf).
     */
    private String tipoExpressao(AlgumaParser.ExpressaoContext ctx) {
        if (ctx.op_logico_1() != null && !ctx.op_logico_1().isEmpty()) return "logico";
        if (ctx.termo_logico().size() == 1) return tipoTermoLogico(ctx.termo_logico(0));
        return "logico";
    }

    private String tipoTermoLogico(AlgumaParser.Termo_logicoContext ctx) {
        if (ctx.op_logico_2() != null && !ctx.op_logico_2().isEmpty()) return "logico";
        if (ctx.fator_logico().size() == 1) return tipoFatorLogico(ctx.fator_logico(0));
        return "logico";
    }

    private String tipoFatorLogico(AlgumaParser.Fator_logicoContext ctx) {
        return tipoParcelaLogica(ctx.parcela_logica());
    }

    private String tipoParcelaLogica(AlgumaParser.Parcela_logicaContext ctx) {
        if (ctx.exp_relacional() == null) return "logico";
        if (ctx.exp_relacional().op_relacional() != null) return "logico";
        return tipoExpAritmetica(ctx.exp_relacional().exp_aritmetica(0));
    }

    private String tipoExpAritmetica(AlgumaParser.Exp_aritmeticaContext ctx) {
        String t = tipoTermo(ctx.termo(0));
        for (int i = 1; i < ctx.termo().size(); i++) t = combinar(t, tipoTermo(ctx.termo(i)));
        return t;
    }

    private String tipoTermo(AlgumaParser.TermoContext ctx) {
        String t = tipoFatorExp(ctx.fator(0));
        for (int i = 1; i < ctx.fator().size(); i++) t = combinar(t, tipoFatorExp(ctx.fator(i)));
        return t;
    }

    private String tipoFatorExp(AlgumaParser.FatorContext ctx) {
        String t = tipoParcela(ctx.parcela(0));
        for (int i = 1; i < ctx.parcela().size(); i++) t = combinar(t, tipoParcela(ctx.parcela(i)));
        return t;
    }

    private String tipoParcela(AlgumaParser.ParcelaContext ctx) {
        if (ctx.parcela_nao_unario() != null) {
            if (ctx.parcela_nao_unario().CADEIA() != null) return "literal";
            return "^" + tipoDoIdentificador(ctx.parcela_nao_unario().identificador());
        }
        var pu = ctx.parcela_unario();
        if (pu.NUM_INT()  != null) return "inteiro";
        if (pu.NUM_REAL() != null) return "real";
        if (pu.identificador() == null) return tipoExpressao(pu.expressao(0));
        if (pu.getChildCount() > 1 && pu.getChild(1).getText().equals("(")) {
            Simbolo s = escopos.buscar(pu.identificador().IDENT(0).getText());
            return s != null ? s.getTipo() : "tipo_indefinido";
        }
        return tipoDoIdentificador(pu.identificador());
    }

    private String combinar(String t1, String t2) {
        if (t1.equals(t2)) return t1;
        if ((t1.equals("inteiro") || t1.equals("real")) &&
            (t2.equals("inteiro") || t2.equals("real"))) return "real";
        return "tipo_indefinido";
    }

    /**
     * Resolve o nome do tipo LA para uma string normalizada ("inteiro", "real", etc.
     * ou o nome do tipo definido pelo usuário).
     */
    private String resolverNomeTipo(AlgumaParser.TipoContext ctx) {
        if (ctx.registro() != null)      return "registro";
        if (ctx.tipo_estendido() != null) return ctx.tipo_estendido().getText();
        return "tipo_indefinido";
    }

    /**
     * Lê os campos de um bloco 'registro' e retorna mapa nomeCampo → tipoLA.
     */
    private java.util.Map<String, String> resolverCamposRegistro(AlgumaParser.RegistroContext ctx) {
        java.util.Map<String, String> campos = new java.util.LinkedHashMap<>();
        for (var varCtx : ctx.variavel()) {
            String tipoTexto = resolverNomeTipo(varCtx.tipo());
            for (var idCtx : varCtx.identificador()) {
                campos.put(idCtx.IDENT(0).getText(), tipoTexto);
            }
        }
        return campos;
    }
}
