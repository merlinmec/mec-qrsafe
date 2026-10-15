---
name: mec-qrsafe
description: Editor de QR code que mostra a conta da correção de erro antes de deixar exportar.
colors:
  accent-violet: "#6d28d9"
  accent-violet-deep: "#5b21b6"
  accent-violet-wash: "#f1ebfd"
  page-mist: "#f4f4f6"
  stage-gray: "#ececf1"
  surface-white: "#ffffff"
  surface-sunken: "#f8f8fa"
  hairline: "#e3e3e8"
  hairline-strong: "#cfcfd6"
  ink: "#18181b"
  ink-secondary: "#52525b"
  ink-tertiary: "#71717a"
  verdict-pass: "#15803d"
  verdict-pass-wash: "#e8f6ed"
  verdict-warn: "#b45309"
  verdict-warn-wash: "#fdf3e2"
  verdict-block: "#b91c1c"
  verdict-block-wash: "#fdecec"
typography:
  title:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
    fontSize: "1rem"
    fontWeight: 600
    letterSpacing: "-0.01em"
  section:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 600
  body:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif"
    fontSize: "0.8125rem"
    fontWeight: 500
  data:
    fontFamily: "ui-monospace, 'Cascadia Code', 'SF Mono', Consolas, monospace"
    fontSize: "0.75rem"
    fontFeature: "'tnum'"
rounded:
  sm: "6px"
  md: "8px"
  lg: "12px"
  pill: "999px"
spacing:
  "1": "4px"
  "2": "8px"
  "3": "12px"
  "4": "16px"
  "5": "20px"
  "6": "24px"
  "8": "32px"
components:
  button-primary:
    backgroundColor: "{colors.accent-violet}"
    textColor: "{colors.surface-white}"
    rounded: "{rounded.md}"
    padding: "10px 16px"
  button-primary-hover:
    backgroundColor: "{colors.accent-violet-deep}"
  button-secondary:
    backgroundColor: "{colors.surface-white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "9px 14px"
  button-ghost:
    backgroundColor: "transparent"
    textColor: "{colors.ink-secondary}"
    rounded: "{rounded.md}"
    padding: "6px 8px"
  input-text:
    backgroundColor: "{colors.surface-white}"
    textColor: "{colors.ink}"
    rounded: "{rounded.md}"
    padding: "8px 10px"
  chip:
    backgroundColor: "{colors.surface-white}"
    textColor: "{colors.ink-secondary}"
    rounded: "{rounded.pill}"
    padding: "5px 10px"
  chip-selected:
    backgroundColor: "{colors.accent-violet-wash}"
    textColor: "{colors.accent-violet}"
---

# Design System: mec-qrsafe

## Overview

**Creative North Star: "A Bancada de Inspeção"**

Um editor de QR code do padrão da categoria — opções numa coluna, o QR grande no palco, validação e exportar logo abaixo —, mas com uma bancada de inspeção ao lado: a correção de erro bloco a bloco e um raio-X da matriz. O sistema é sóbrio de propósito para que a única coisa colorida e viva na tela seja o QR e o estado dele. A interface recua; o objeto em inspeção e o veredito sobre ele lideram.

Densidade de ferramenta de trabalho, tema claro (desktop de dia, com o código aberto ao lado). Neutros frios, uma cor de marca e três cores de estado, nada além disso. O visual escolhido pelo usuário é o padrão da categoria (régua: QRCode Monkey, QRCodeChimp, Adobe Express); o diferencial vive no conteúdo, não numa estética fora do comum.

**Key Characteristics:**
- Três zonas fixas no desktop: controles (340px), palco do QR, inspeção (340px).
- O QR é desenhado num canvas próprio, em pixels inteiros por módulo, sobre um palco cinza.
- Estado da validação dito em texto e cor ao mesmo tempo, nunca só por cor.
- Números sempre com algarismos tabulares.

## Colors

Neutros frios com um violeta de marca; verde, âmbar e vermelho são do veredito e de mais nada.

### Primary
- **Violeta mec** ({colors.accent-violet}): ação principal (Baixar), controles ativos (preenchimento do slider, chip selecionado), foco e links. O mesmo violeta do portfólio do autor. `accent-violet-deep` no hover; `accent-violet-wash` como fundo de seleção.

### Neutral
- **Névoa** ({colors.page-mist}): fundo da página.
- **Cinza de palco** ({colors.stage-gray}): o fundo atrás do QR — separa o objeto em inspeção das colunas brancas.
- **Branco de superfície** ({colors.surface-white}) e **branco rebaixado** ({colors.surface-sunken}): colunas, campos, trilhos de segmentado e de barra.
- **Fio** ({colors.hairline}) e **fio forte** ({colors.hairline-strong}): divisórias de seção e bordas de campo.
- **Tinta**, **tinta secundária**, **tinta terciária** ({colors.ink}, {colors.ink-secondary}, {colors.ink-tertiary}): texto, rótulos, dicas.

### Named Rules
**The Verdict Owns Color Rule.** Verde ({colors.verdict-pass}), âmbar ({colors.verdict-warn}) e vermelho ({colors.verdict-block}) só aparecem para dizer se o QR escaneia: o painel do veredito, os selos das violações e as barras dos blocos. Nenhum outro elemento usa essas cores.

**The Red Is Damage Rule.** No raio-X, vermelho significa codeword destruído pelo logo. As cores dos blocos ficam no arco de 30° a 330° de matiz para nunca colidir com ele.

## Typography

**Body Font:** fonte do sistema (system-ui, com -apple-system, Segoe UI, Roboto)
**Data Font:** ui-monospace (com Cascadia Code, SF Mono, Consolas)

**Character:** a fonte do sistema mantém a ferramenta familiar e sem ruído; a monoespaçada aparece só onde o conteúdo é código ou contagem.

### Hierarchy
- **Title** (600, 1rem, -0.01em): nome do produto, título "Por dentro do QR", título do veredito.
- **Section** (600, 0.875rem): títulos das seções do painel.
- **Body** (400, 0.875rem, 1.5): texto corrido e mensagens das violações.
- **Label** (500, 0.8125rem): rótulos de campo e valores de controle, com algarismos tabulares.
- **Data** (mono, 0.75rem, tnum): conteúdo do QR, hex das cores, contagens de codeword (4/13).

### Named Rules
**The Mono Means Data Rule.** Monoespaçada só para código e contagem (conteúdo, hex, "6/13"). Valores de controle como "20% da largura" usam a fonte da interface com algarismos tabulares.

## Layout

Grade de três colunas no desktop largo: controles 340px | palco flexível | inspeção 340px; as colunas laterais são fixas (sticky) abaixo do cabeçalho de 56px e rolam por dentro. Abaixo de 1240px a inspeção desce para baixo do palco; abaixo de 820px tudo empilha com o QR primeiro. O palco limita o QR e a coluna de validação a 520px. Ritmo de espaçamento em passos de 4px (4, 8, 12, 16, 20, 24, 32); seções do painel com 20px de lado e 24px embaixo, separadas por fio.

## Elevation & Depth

Plano por padrão, profundidade por tons (palco cinza contra colunas brancas). Sombra só em dois lugares: o QR no palco, que é o objeto em inspeção, e os controles flutuantes (polegar do slider, opção ativa do segmentado, dica sobre o canvas).

### Shadow Vocabulary
- **Repouso** (`0 1px 2px rgb(24 24 27 / 0.06), 0 1px 1px rgb(24 24 27 / 0.04)`): opção ativa do segmentado, polegar do slider, avisos sobre o canvas.
- **Objeto** (`0 6px 20px -6px rgb(24 24 27 / 0.16), 0 2px 6px -2px rgb(24 24 27 / 0.08)`): o canvas do QR e a dica de módulo.

## Shapes

Cantos suaves e consistentes: 6px em peças pequenas (miniatura do logo, dica), 8px em campos, botões e no próprio QR, 999px em chips, selos e barras. Bordas de 1px; nada de faixa colorida lateral.

## Components

### Buttons
- **Shape:** cantos de 8px.
- **Primary:** violeta com texto branco, 10px 16px; só para Baixar.
- **Hover / Focus:** escurece para o violeta profundo; foco é um anel de 3px em violeta translúcido; pressionar desce 1px.
- **Secondary / Ghost:** secundário branco com fio forte; fantasma sem borda, tinta secundária, para ações de seção (Inverter, Remover, Centralizar).
- **Disabled:** primário vira lilás apagado e sempre vem com o motivo escrito abaixo (aria-describedby).

### Chips
- **Style:** pílula branca com fio, texto secundário.
- **State:** selecionado ganha borda e texto violeta sobre o lavado violeta.

### Inputs / Fields
- **Style:** branco, fio forte, 8px; textarea de conteúdo em mono.
- **Focus:** borda violeta + anel de 3px.
- **Error:** borda vermelha (`aria-invalid`).
- **Slider:** trilho de 4px, preenchimento violeta, polegar branco com aro violeta.
- **Segmented:** rádios nativos num trilho rebaixado; a opção ativa vira uma peça branca com sombra de repouso.

### Navigation
Cabeçalho de 56px, branco translúcido com desfoque, marca à esquerda (selo violeta com três "olhos" de QR), frase do produto e link para o GitHub à direita. No celular a frase some.

### Veredito
Painel com ícone, título ("Escaneia", "Escaneia, com ressalvas", "Não escaneia"), frase de consequência e nota de 0 a 100, sobre o lavado da cor do estado. As violações vêm abaixo em lista, cada uma com selo (Bloqueia/Aviso), a mensagem do servidor e um link "Ajustar em <seção>" que rola e pisca a seção certa.

### Barras dos blocos
Uma linha por bloco Reed-Solomon: amostra da cor do bloco, nome, barra (verde até 75% da correção, âmbar até 100%, vermelho acima) com um traço no limiar de 75%, e a contagem em mono. Passar o mouse ou focar uma linha acende os codewords daquele bloco no QR. A barra cresce por `scaleX`, nunca por largura.

### Raio-X
Modo do palco: "estrutura" pinta os padrões fixos (finder violeta, timing verde-azulado, alinhamento azul, formato âmbar, versão laranja, módulo escuro rosa); "blocos" pinta cada módulo de dado na cor do seu bloco e os destruídos em vermelho. No raio-X a placa do logo vira um contorno tracejado, para mostrar o que está embaixo.

## Do's and Don'ts

### Do:
- **Do** desenhar o logo sempre sobre uma placa na cor clara do QR — é o caso que a validação modela.
- **Do** dizer o estado em texto além da cor (título do veredito, selo da violação, aria-label das barras).
- **Do** manter o QR em pixels inteiros por módulo (sem borda borrada).
- **Do** usar algarismos tabulares em todo número que muda.

### Don't:
- **Don't** usar verde, âmbar ou vermelho fora do veredito e das barras.
- **Don't** usar vermelho como cor de bloco no raio-X.
- **Don't** montar grade de "número grande com rótulo pequeno" para os dados técnicos; a inspeção é uma lista de chave e valor.
- **Don't** oferecer uma forma de módulo ou de canto que não esteja no teste contra o leitor do backend (ShapesAgainstReaderTest).
