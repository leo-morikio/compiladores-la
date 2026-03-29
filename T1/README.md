# T1 — Analisador Léxico da Linguagem LA

**Disciplina:** Construção de Compiladores  
**Professor:** Daniel Lucrédio

## Integrantes

- Leonardo Poloni Berti Morikio - 823832
- Matheus Marangoni Salomão - 821684
- João Lucas Gomes Pelegrino - 822033

---

## Descrição

Este projeto consiste na implementação de um analisador léxico para a linguagem LA (Linguagem Algorítmica), utilizando a ferramenta ANTLR.

O analisador é responsável por ler um arquivo contendo um programa em LA e produzir um arquivo de saída com a sequência de tokens identificados, conforme especificação do trabalho.

---

## Tecnologias utilizadas

- Java
- ANTLR
- Maven

---

## Compilação

Para compilar o projeto, utilize o Maven:

```bash
mvn clean package
```

O arquivo executável será gerado no diretório `target/`.

---

## Execução

O programa deve ser executado via linha de comando com dois argumentos obrigatórios: o arquivo de entrada e o arquivo de saída.

```bash
java -jar target/alguma-lexico-1.0-SNAPSHOT.jar <arquivo_entrada> <arquivo_saida>
```

### Exemplo:

```bash
java -jar target/alguma-lexico-1.0-SNAPSHOT.jar entrada.txt saida.txt
```

---

## Entrada

Arquivo texto contendo código escrito na linguagem LA.

---

## Saída

Arquivo texto contendo os tokens identificados, no seguinte formato:

```
<'lexema',TIPO>
```

---

## Tratamento de erros

O analisador interrompe a execução ao encontrar o primeiro erro léxico.

Os seguintes tipos de erro são tratados:

- Símbolo não identificado
- Cadeia de caracteres não fechada
- Comentário não fechado

### Exemplo:

```
Linha 5: ~ - simbolo nao identificado
```

---

## Casos de teste

O analisador foi validado utilizando os casos de teste fornecidos na disciplina, obtendo sucesso em todos eles.

---

## Observações

- Espaços em branco são ignorados
- Comentários não são considerados na geração de tokens
- A saída é obrigatoriamente escrita em arquivo

---