lexer grammar AlgumaLexer;

// --------------------
// Palavras-chave da linguagem LA
// --------------------
ALGORITMO       : 'algoritmo';
FIM_ALGORITMO   : 'fim_algoritmo';
DECLARE         : 'declare';
LEIA            : 'leia';
ESCREVA         : 'escreva';
LITERAL         : 'literal';
INTEIRO         : 'inteiro';
REAL            : 'real';
LOGICO          : 'logico';

// Estruturas de controle
SE              : 'se';
ENTAO           : 'entao';
SENAO           : 'senao';
FIM_SE          : 'fim_se';
ENQUANTO        : 'enquanto';
FACA            : 'faca';
FIM_ENQUANTO    : 'fim_enquanto';
PARA            : 'para';
ATE             : 'ate';
FIM_PARA        : 'fim_para';

// Estrutura de seleção múltipla
CASO            : 'caso';
SEJA            : 'seja';
FIM_CASO        : 'fim_caso';

// Operadores lógicos
NAO             : 'nao';
E               : 'e';
OU              : 'ou';

// Valores booleanos
VERDADEIRO      : 'verdadeiro';
FALSO           : 'falso';

// Tipos e estruturas
TIPO            : 'tipo';
REGISTRO        : 'registro';
FIM_REGISTRO    : 'fim_registro';

// Subprogramas
PROCEDIMENTO    : 'procedimento';
FIM_PROCEDIMENTO: 'fim_procedimento';
FUNCAO          : 'funcao';
FIM_FUNCAO      : 'fim_funcao';
RETORNE         : 'retorne';

// Declarações
CONSTANTE       : 'constante';
VAR             : 'var';

// --------------------
// Identificadores
// Letras ou '_' seguidos de letras, números ou '_'
// --------------------
IDENT : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

// --------------------
// Números (real antes de inteiro para evitar ambiguidade)
// --------------------
NUM_REAL : ('0'..'9')+ '.' ('0'..'9')+;
NUM_INT  : ('0'..'9')+;

// --------------------
// Cadeia de caracteres (não permite quebra de linha)
// --------------------
CADEIA : '"' (~('\n'|'"'))* '"';

// --------------------
// Comentários válidos (ignorados pelo analisador)
// --------------------
COMENTARIO : '{' (~('}'|'\n'))* '}' -> skip;

// --------------------
// Tratamento de erros léxicos
// --------------------

// Comentário não fechado na mesma linha
COMENTARIO_NAO_FECHADO : '{' (~('}'|'\n'))* '\n';

// Cadeia não fechada na mesma linha
CADEIA_NAO_FECHADA : '"' (~('\n'|'"'))* '\n';

// --------------------
// Espaços em branco (ignorados)
// --------------------
WS : (' '|'\t'|'\r'|'\n') -> skip;

// --------------------
// Operadores
// --------------------
ATRIBUICAO : '<-';

// Relacionais
OP_REL_MENOR_IGUAL : '<=';
OP_REL_MAIOR_IGUAL : '>=';
OP_REL_DIF         : '<>';
OP_REL_MENOR       : '<';
OP_REL_MAIOR       : '>';
OP_REL_IGUAL       : '=';

// Aritméticos
OP_ARIT_SOMA : '+';
OP_ARIT_SUB  : '-';
OP_ARIT_MULT : '*';
OP_ARIT_DIV  : '/';
OP_ARIT_MOD  : '%';

// Ponteiros/endereço
CIRCUNFLEXO : '^';
E_COMERCIAL : '&';

// --------------------
// Delimitadores
// --------------------
DELIM        : ':';
VIRG         : ',';
ABREPAR      : '(';
FECHAPAR     : ')';
ABRECOLCHETE : '[';
FECHACOLCHETE: ']';
PONTOPONTO   : '..';
PONTO        : '.';

// --------------------
// Regra de erro genérico
// Captura qualquer símbolo não reconhecido
// --------------------
ERRO : .;