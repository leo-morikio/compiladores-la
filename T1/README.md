# Construção de Compiladores - UFSCar

Este repositório centraliza os projetos desenvolvidos na disciplina de Construção de Compiladores da UFSCar, focados na criação de um compilador para a linguagem LA (Linguagem Algorítmica).

## Integrantes
- Leonardo Poloni Berti Morikio - RA: 823832
- Matheus Marangoni Salomão - RA: 821684
- João Lucas Gomes Pelegrino - RA: 822033

---

## Status do Projeto

O desenvolvimento do compilador é realizado de forma incremental, abrangendo as seguintes etapas concluídas:

1. **Trabalho 1 (Analisador Léxico):** Identificação de tokens, tratamento de símbolos não identificados, comentários e literais não fechados.
2. **Trabalho 2 (Analisador Sintático):** Implementação da gramática completa utilizando ANTLR4, com suporte a registros, ponteiros, vetores e sub-rotinas.

---

## Estrutura do Repositório

De acordo com as diretrizes da disciplina para a manutenção de um histórico unificado, a estrutura está organizada da seguinte forma:

- **[T1/](./T1/)**: Diretório que concentra a evolução dos trabalhos 1 e 2.
- **[Projeto Java](./T1/alguma-lexico/)**: Pasta contendo o código-fonte, arquivos de configuração Maven (pom.xml) e a gramática (Alguma.g4).
- **[casos-de-teste/](./casos-de-teste/)**: Suíte oficial de testes para validação dos analisadores.
- **[corretor/](./corretor/)**: Ferramenta de correção automática fornecida para o projeto.

---

## Documentação e Execução

As instruções detalhadas de requisitos de sistema, comandos para compilação via Maven e os parâmetros de execução via linha de comando exigidos para o Trabalho 2 podem ser acessadas no link abaixo:

- [Instruções de Compilação e Execução (T2)](./README.md)

---
*UFSCar - 2026*
