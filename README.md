# Construção de Compiladores - UFSCar

Este repositório centraliza o desenvolvimento do compilador para a linguagem LA (Linguagem Algorítmica), realizado como parte da disciplina de Construção de Compiladores do Departamento de Computação da UFSCar.

## Integrantes
- Leonardo Poloni Berti Morikio - RA: 823832
- Matheus Marangoni Salomão - RA: 821684
- João Lucas Gomes Pelegrino - RA: 822033

---

## Status do Projeto

O projeto é desenvolvido de forma incremental. Até o presente momento, as seguintes etapas foram concluídas e validadas:

1. **Trabalho 1 (Analisador Léxico):** Implementação da análise de tokens, tratamento de símbolos não identificados, comentários e literais não fechados.
2. **Trabalho 2 (Analisador Sintático):** Implementação da gramática completa da linguagem LA, com suporte a procedimentos, funções, registros, ponteiros e vetores.

---

## Estrutura do Repositório

Conforme a recomendação da disciplina para manter o histórico de evolução em um repositório unificado, a estrutura está organizada da seguinte forma:

- **[T1/alguma-lexico/](./T1/alguma-lexico/)**: Diretório contendo o código-fonte Java, a gramática ANTLR4 (`Alguma.g4`) e as classes de controle (incluindo o Parser e o ErrorListener customizado).
- **[casos-de-teste/](./casos-de-teste/)**: Suíte de testes oficial utilizada para a validação dos analisadores léxico e sintático.
- **[corretor/](./corretor/)**: Ferramenta de correção automática fornecida pela disciplina.

---

## Documentação e Execução

As instruções detalhadas de configuração de ambiente, compilação via Maven e comandos de execução para linha de comando (conforme exigido na especificação do Trabalho 2) estão localizadas no README específico do projeto:

- [Ver Instruções de Compilação e Execução](./T1/alguma-lexico/README.md)

---
*UFSCar - 2026*
