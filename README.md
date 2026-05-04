# Construção de Compiladores - UFSCar

Este repositório centraliza os projetos desenvolvidos na disciplina de Construção de Compiladores da UFSCar, focados na criação de um compilador para a linguagem LA (Linguagem Algorítmica).

## Integrantes

* **Leonardo Poloni Berti Morikio** - RA: 823832
* **Matheus Marangoni Salomão** - RA: 821684
* **João Lucas Gomes Pelegrino** - RA: 822033

---

## Status do Projeto

O desenvolvimento do compilador é realizado de forma incremental, abrangendo as seguintes etapas concluídas e validadas:

1. **Trabalho 1 (Analisador Léxico):** Implementação da análise de tokens, identificação de tipos, tratamento de símbolos não identificados, cadeias literais e comentários não fechados.
2. **Trabalho 2 (Analisador Sintático):** Implementação da gramática completa utilizando ANTLR4, abrangendo a verificação sintática de registros, ponteiros, vetores e sub-rotinas (procedimentos e funções).
3. **Trabalho 3 (Analisador Semântico):** Implementação da análise de escopos, tabelas de símbolos e verificação de compatibilidade de tipos em expressões e atribuições.

---

## Estrutura do Repositório

Seguindo as diretrizes da disciplina para manutenção de um histórico unificado, o repositório está organizado da seguinte forma:

* **Pasta T1/**: Concentra o código-fonte e a evolução dos trabalhos 1 (Léxico) e 2 (Sintático).
* **Pasta T3/**: Contém o desenvolvimento do analisador semântico, incluindo o gerenciamento de escopos e tipos.
* **Pasta T3/alguma-semantico/**: Arquivos de configuração Maven (`pom.xml`), código-fonte Java e lógica de análise semântica.
* **Pasta casos-de-teste/**: Suíte oficial de testes utilizada para a validação dos analisadores em todas as etapas.
* **Pasta corretor/**: Ferramenta de correção automática fornecida para validação dos resultados.

---

## Documentação e Execução

Cada etapa do projeto possui sua própria documentação detalhada dentro de suas respectivas pastas.

* Para detalhes sobre o **Analisador Léxico e Sintático**, consulte o README dentro de `T1/`.
* Para detalhes sobre o **Analisador Semântico**, consulte o README dentro de `T3/alguma-semantico/`.

As instruções abrangem pré-requisitos de sistema, comandos de compilação via Maven e parâmetros de execução via linha de comando.

---
*UFSCar - 2026*