# Construção de Compiladores - UFSCar

Este repositório centraliza os projetos desenvolvidos na disciplina de Construção de Compiladores da UFSCar, focados na criação de um compilador para a linguagem LA (Linguagem Algorítmica).

## Integrantes
- Leonardo Poloni Berti Morikio - RA: 823832
- Matheus Marangoni Salomão - RA: 821684
- João Lucas Gomes Pelegrino - RA: 822033

---

## Status do Projeto

O desenvolvimento do compilador é realizado de forma incremental, abrangendo as seguintes etapas concluídas e validadas:

1. **Trabalho 1 (Analisador Léxico):** Implementação da análise de tokens, identificação de tipos, tratamento de símbolos não identificados, cadeias literais e comentários não fechados.
2. **Trabalho 2 (Analisador Sintático):** Implementação da gramática completa utilizando ANTLR4, abrangendo a verificação sintática de registros, ponteiros, vetores e sub-rotinas (procedimentos e funções).

---

## Estrutura do Repositório

Seguindo as diretrizes da disciplina para manutenção de um histórico unificado, o repositório está organizado da seguinte forma:

- **Pasta T1/**: Concentra o código-fonte e a evolução dos trabalhos 1 e 2.
- **Pasta T1/alguma-lexico/**: Contém os arquivos de configuração Maven (pom.xml), código-fonte Java e a gramática definida em ANTLR4 (Alguma.g4).
- **Pasta casos-de-teste/**: Suíte oficial de testes utilizada para a validação dos analisadores.
- **Pasta corretor/**: Ferramenta de correção automática utilizada para conferência dos resultados.

---

## Documentação e Execução

As informações detalhadas sobre os pré-requisitos de sistema, comandos de compilação via Maven e os parâmetros obrigatórios de execução via linha de comando para o Trabalho 2 estão disponíveis no arquivo README.md localizado dentro da pasta T1.

---
*UFSCar - 2026*
