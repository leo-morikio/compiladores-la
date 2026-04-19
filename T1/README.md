# Compilador LA — Trabalhos 1 e 2

**Disciplina:** Construção de Compiladores  
**Professor:** Daniel Lucrédio  
**Instituição:** UFSCar - Universidade Federal de São Carlos  

---

## Integrantes

- Leonardo Poloni Berti Morikio - RA: 823832  
- Matheus Marangoni Salomão - RA: 821684  
- João Lucas Gomes Pelegrino - RA: 822033  

---

## Descrição

Este projeto contempla o desenvolvimento de um compilador para a Linguagem LA (Linguagem Algorítmica). O sistema está dividido em duas frentes principais:

- **Analisador Léxico (T1):** Identificação de tokens e tratamento de erros (símbolos não identificados, cadeias e comentários não fechados).  
- **Analisador Sintático (T2):** Verificação da estrutura gramatical, com suporte a registros, ponteiros, vetores e chamadas de funções/procedimentos.  

O compilador foi desenvolvido em Java com o auxílio da ferramenta ANTLR4 e gerenciamento de dependências via Maven.

---

## Documentação Externa (Requisitos e Execução)

### Pré-requisitos

- Java JDK 17 ou superior  
- Apache Maven 3.8 ou superior  

### Como Compilar

1. Acesse a pasta raiz do projeto (onde reside o arquivo `pom.xml`).  
2. Execute o comando no terminal: mvn clean package  

O Maven gerará um arquivo `.jar` na pasta `target/`, geralmente nomeado como `alguma-lexico-1.0-SNAPSHOT-jar-with-dependencies.jar`.

---

## Como Executar

O programa deve ser executado via linha de comando com dois argumentos obrigatórios:

java -jar target/alguma-lexico-1.0-SNAPSHOT-jar-with-dependencies.jar <caminho_entrada> <caminho_saida>

---

## Formato de Saída e Erros

### Analisador Sintático (T2)

Se houver erro, a análise é interrompida e reportada no arquivo de saída:

- **Formato:**  
  `Linha X: erro sintatico proximo a <lexema>`

- **Sucesso:**  
  `Fim da compilacao`

---

### Analisador Léxico (T1)

Erros léxicos possuem prioridade e seguem as mensagens:

- `simbolo nao identificado`  
- `cadeia literal nao fechada`  
- `comentario nao fechado`  

---

## Documentação Interna

| Arquivo                 | Descrição |
|------------------------|----------|
| **Alguma.g4**          | Regras gramaticais da linguagem LA. Regra `identificador` otimizada para registros, ponteiros, ponteiros e vetores. |
| **MeuErrorListener.java** | Extensão do `BaseErrorListener` do ANTLR para capturar e formatar erros sintáticos. |
| **Principal.java**     | Gerencia o fluxo: leitura (`CharStreams`), Lexer/Parser, tratamento de exceções e escrita em UTF-8. |

---

## Resultados

O projeto foi validado utilizando a suíte de testes oficial da disciplina.

- **Status:** Aprovado  
- **Pontuação:** 62/62 casos de teste (100% de aproveitamento)  

---

### Observação importante

O projeto utiliza obrigatoriamente a codificação UTF-8 e o Maven gerencia as dependências do ANTLR automaticamente.
