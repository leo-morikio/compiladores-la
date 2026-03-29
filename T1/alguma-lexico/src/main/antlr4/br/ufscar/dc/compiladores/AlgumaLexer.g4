lexer grammar AlgumaLexer;

// Palavras-chave da linguagem LA
ALGORITMO       : 'algoritmo';
FIM_ALGORITMO   : 'fim_algoritmo';
DECLARE         : 'declare';
LEIA            : 'leia';
ESCREVA         : 'escreva';
LITERAL         : 'literal';
INTEIRO         : 'inteiro';
REAL            : 'real';
LOGICO          : 'logico';
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
CASO            : 'caso';
SEJA            : 'seja';
FIM_CASO        : 'fim_caso';
NAO             : 'nao';
E               : 'e';
OU              : 'ou';
VERDADEIRO      : 'verdadeiro';
FALSO           : 'falso';
TIPO            : 'tipo';
REGISTRO        : 'registro';
FIM_REGISTRO    : 'fim_registro';
PROCEDIMENTO    : 'procedimento';
FIM_PROCEDIMENTO: 'fim_procedimento';
FUNCAO          : 'funcao';
FIM_FUNCAO      : 'fim_funcao';
RETORNE         : 'retorne';
CONSTANTE       : 'constante';
VAR             : 'var';

// Identificadores (depois das palavras-chave)
IDENT           : ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

// Números reais antes de inteiros
NUM_REAL        : ('0'..'9')+ '.' ('0'..'9')+;
NUM_INT         : ('0'..'9')+;

// Cadeia com aspas duplas, não pode ter quebra de linha
CADEIA          : '"' (~('\n'|'"'))* '"';

// Comentário válido - abre e fecha na mesma linha
COMENTARIO : '{' (~('}'|'\n'))* '}' -> skip;

// Comentário não fechado na mesma linha
COMENTARIO_NAO_FECHADO : '{' (~('}'|'\n'))* '\n';

// Cadeia não fechada na mesma linha
CADEIA_NAO_FECHADA : '"' (~('\n'|'"'))* '\n';

// Espaços em branco ignorados
WS              : (' '|'\t'|'\r'|'\n') -> skip;

// Operador de atribuição (antes dos relacionais)
ATRIBUICAO      : '<-';

// Operadores relacionais
OP_REL_MENOR_IGUAL : '<=';
OP_REL_MAIOR_IGUAL : '>=';
OP_REL_DIF         : '<>';
OP_REL_MENOR       : '<';
OP_REL_MAIOR       : '>';
OP_REL_IGUAL       : '=';

// Operadores aritméticos
OP_ARIT_SOMA    : '+';
OP_ARIT_SUB     : '-';
OP_ARIT_MULT    : '*';
OP_ARIT_DIV     : '/';
OP_ARIT_MOD     : '%';

// Operadores de ponteiro e endereço
CIRCUNFLEXO     : '^';
E_COMERCIAL     : '&';

// Delimitadores
DELIM           : ':';
VIRG            : ',';
ABREPAR         : '(';
FECHAPAR        : ')';
ABRECOLCHETE    : '[';
FECHACOLCHETE   : ']';
PONTOPONTO      : '..';
PONTO           : '.';

// Qualquer outro símbolo é erro léxico
ERRO            : .;