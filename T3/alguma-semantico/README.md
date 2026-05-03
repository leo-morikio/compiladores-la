# T3 - Analisador Semântico (Linguagem LA)

Este projeto consiste na implementação de um **Analisador Semântico** para a Linguagem Algorítmica (LA), desenvolvido como parte do Trabalho 3 da disciplina de Construção de Compiladores (DC/UFSCar).

---

## 📌 Descrição e Regras Implementadas

O analisador processa o código-fonte e identifica erros semânticos **sem interromper a execução**, reportando-os até o final do arquivo.

As verificações incluem:

### 🔎 Erros de Escopo
- Identificadores (variáveis, constantes, funções, etc.) já declarados no mesmo escopo.

### ❌ Tipos Inexistentes
- Uso de tipos que não foram previamente declarados.

### ⚠️ Identificadores Não Declarados
- Uso de variáveis ou sub-rotinas não definidas.

### 🔄 Incompatibilidade de Tipos
- Validação de atribuições (ex: `real` recebe `inteiro`)
- Verificação de expressões (ex: impedir soma de literal com `logico`)
- Compatibilidade de registros e ponteiros

---

## ⚙️ Pré-requisitos

Certifique-se de ter instalado:

- Java JDK 17 ou superior
- Apache Maven 3.9.0 ou superior

Para validar:

```bash
java -version
mvn -version
```

## 🛠️ Compilação e Build
- Na pasta raiz do projeto:

```bash
mvn clean package
```

- O arquivo gerado estará em:

```bash
target/alguma-semantico-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## ▶️ Execução

```bash
java -jar target/alguma-semantico-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo_entrada> <arquivo_saida>
```

## ⚠️ O programa não imprime erros no terminal.
- Todos os resultados são gravados no arquivo de saída.

## 🧠 Organização do Código

### 📁 Principal.java

- Ponto de entrada do sistema
- Gerencia leitura de parâmetros e saída

### 📁 AnalisadorSemantico.java
- Implementa a lógica principal
- Percorre a AST e aplica regras

### 📁 Escopos.java
- Gerencia pilha de escopos
- Controla visibilidade de variáveis
### 📁 TabelaDeSimbolos.java
- Armazena nomes, tipos e metadados