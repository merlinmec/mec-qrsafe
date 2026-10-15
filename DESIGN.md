---
name: mec-qrsafe
description: Editor de QR code que mostra a conta da correção de erro antes de deixar exportar.
colors:
  gold-leaf: "oklch(84% 0.19 80.46)"
  gold-leaf-bright: "oklch(88% 0.16 84)"
  gold-leaf-deep: "oklch(61% 0.085 78)"
  dark-ink: "oklch(14% 0.018 95)"
  lacquer: "oklch(7% 0.006 95)"
  lacquer-sunken: "oklch(9% 0.006 95)"
  graphite: "oklch(11% 0.006 95)"
  graphite-raised: "oklch(15% 0.008 95)"
  rule: "oklch(78% 0 0 / 0.14)"
  rule-strong: "oklch(78% 0 0 / 0.26)"
  champagne: "oklch(91% 0 0)"
  champagne-muted: "oklch(76% 0 0)"
  champagne-faint: "oklch(62% 0 0)"
  patina: "oklch(74% 0.12 188)"
  ember: "oklch(76% 0.15 55)"
  vermilion: "oklch(68% 0.19 28)"
typography:
  display:
    fontFamily: "'Alumni Sans', 'Albert Sans', Arial, sans-serif"
    fontSize: "2.5rem"
    fontWeight: 100
    lineHeight: 1
    letterSpacing: "-0.01em"
  headline:
    fontFamily: "'Alumni Sans', 'Albert Sans', Arial, sans-serif"
    fontSize: "2.125rem"
    fontWeight: 300
    lineHeight: 1.05
  title:
    fontFamily: "'Albert Sans', 'Avenir Next', 'Helvetica Neue', Arial, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 500
  body:
    fontFamily: "'Albert Sans', 'Avenir Next', 'Helvetica Neue', Arial, system-ui, sans-serif"
    fontSize: "0.9375rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "'SFMono-Regular', 'Roboto Mono', 'JetBrains Mono', Consolas, monospace"
    fontSize: "0.625rem"
    fontWeight: 400
    letterSpacing: "0.24em"
rounded:
  sm: "4px"
  md: "6px"
  lg: "10px"
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
    backgroundColor: "{colors.gold-leaf}"
    textColor: "{colors.dark-ink}"
    rounded: "{rounded.md}"
    padding: "10px 16px"
  button-primary-hover:
    backgroundColor: "{colors.gold-leaf-bright}"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.champagne}"
    rounded: "{rounded.md}"
    padding: "9px 14px"
  input-text:
    backgroundColor: "{colors.lacquer-sunken}"
    textColor: "{colors.champagne}"
    rounded: "{rounded.md}"
    padding: "8px 11px"
  chip:
    backgroundColor: "transparent"
    textColor: "{colors.champagne-muted}"
    typography: "{typography.label}"
    rounded: "{rounded.sm}"
    padding: "5px 9px"
  segmented-active:
    backgroundColor: "{colors.graphite-raised}"
    textColor: "{colors.gold-leaf}"
    rounded: "{rounded.sm}"
---

# Design System: mec-qrsafe

## Overview

**Creative North Star: "Laca e Folha de Ouro"**

Uma bancada escura de laca onde o QR é a única coisa clara: um papel nas cores do usuário, posto sobre o fundo preto. O ouro aparece só onde há uma decisão (a ação de baixar, o controle escolhido, os números que importam), e o resto é grafite e champanhe. O mundo veio da página de escolha de estilo que o usuário viu e preferiu ao primeiro visual, claro e violeta, que ele achou "muito IA".

A tipografia faz o trabalho de identidade: títulos em Alumni Sans finíssima e alta, texto em Albert Sans, e rótulos em mono maiúsculo e espaçado, como as etiquetas de um instrumento. O layout é o do padrão da categoria (controles | QR | inspeção); o caráter está nos materiais, não na composição.

**Key Characteristics:**
- Fundo de laca, superfícies em grafite, filetes finos de champanhe translúcido.
- Ouro é a única cor de ação; pátina, laranja e vermelhão são só do veredito.
- Alumni Sans 100–300 em tamanho de exibição; mono maiúsculo espaçado em todo rótulo.
- O QR nunca segue o tema: é desenhado nas cores do usuário, como papel sobre a laca.

## Colors

Laca quase preta com um toque quente, grafite, champanhe e um único metal.

### Primary
- **Folha de ouro** ({colors.gold-leaf}): botão de baixar (fundo, com tinta escura), opção ativa do segmentado (texto e filete), preenchimento do slider, chave ligada, números do painel "Por dentro do QR", foco e seleção de texto. `gold-leaf-bright` no hover; `gold-leaf-deep` translúcido no botão desabilitado.

### Neutral
- **Laca** ({colors.lacquer}): fundo da página e do palco do QR.
- **Grafite** ({colors.graphite}) e **grafite elevado** ({colors.graphite-raised}): colunas de controles e de inspeção, opção ativa, dica sobre o canvas.
- **Laca rebaixada** ({colors.lacquer-sunken}): campos e trilhos.
- **Filete** ({colors.rule}) e **filete forte** ({colors.rule-strong}): divisórias, bordas de campo e de etiqueta.
- **Champanhe** ({colors.champagne}), **champanhe apagado** e **champanhe fraco**: texto, texto secundário, rótulos e dicas.
- **Tinta escura** ({colors.dark-ink}): texto sobre o ouro.

### Estado
- **Pátina** ({colors.patina}): escaneia; barras de bloco com folga.
- **Brasa** ({colors.ember}): aviso; bloco perto do limite.
- **Vermelhão** ({colors.vermilion}): bloqueio; bloco que estourou.

### Named Rules
**The One Metal Rule.** O ouro é a única cor de ação e de escolha. Estado nunca usa ouro (por isso aviso é brasa, não âmbar dourado).

**The Paper On Lacquer Rule.** O QR é desenhado nas cores que o usuário escolheu, sobre o próprio fundo claro dele; o tema escuro nunca tinge o QR.

**The Red Is Damage Rule.** No raio-X, vermelho significa codeword destruído pelo logo; as cores dos blocos ficam entre 30° e 330° de matiz.

## Typography

**Display Font:** Alumni Sans (com Albert Sans de reserva)
**Body Font:** Albert Sans (com Avenir Next, Helvetica Neue)
**Label Font:** SFMono-Regular / Roboto Mono / JetBrains Mono

**Character:** a Alumni Sans finíssima dá a voz de exibição — alta, leve, de letreiro; a Albert Sans mantém o texto legível; a mono maiúscula espaçada etiqueta tudo, como um instrumento.

### Hierarchy
- **Display** (Alumni Sans 100, 2.5rem, 1): "Por dentro do QR" e a nota do veredito (3.25rem).
- **Headline** (Alumni Sans 300, 2.125rem, 1.05): título do veredito ("Escaneia", "Não escaneia").
- **Title** (Albert Sans 500, 0.9375rem): títulos das seções do painel e dos blocos.
- **Body** (Albert Sans 400, 0.9375rem, 1.5): texto corrido e mensagens das violações.
- **Label** (mono 400, 0.625rem, 0.24em, maiúsculo): rótulos de campo, chaves da lista de inspeção, selos, etiquetas de exemplo, links de ação ("Ajustar em Logo"), nome no cabeçalho.

### Named Rules
**The Labels Are Instruments Rule.** Todo rótulo é mono, maiúsculo e espaçado; nunca um rótulo em Albert Sans negrito.

**The Thin Voice Rule.** Alumni Sans só em tamanho de exibição (≥ 2rem) e em peso 100–300; abaixo disso, Albert Sans.

## Layout

Grade de três colunas no desktop largo: controles 340px | palco flexível | inspeção 340px, com as laterais fixas abaixo do cabeçalho de 56px e rolando por dentro. Abaixo de 1240px a inspeção desce para baixo do palco; abaixo de 820px tudo empilha com o QR primeiro. O palco limita o QR e a coluna de validação a 520px. Espaçamento em passos de 4px; seções do painel com 20px de lado e 24px embaixo, separadas por filete.

## Elevation & Depth

Profundidade por tom: laca (fundo) < laca rebaixada (campos) < grafite (colunas) < grafite elevado (ativo). Sombra só no QR sobre o palco, que é o objeto, e na dica sobre o canvas — sombras escuras e difusas, que só se notam como peso.

### Shadow Vocabulary
- **Objeto** (`0 18px 40px -12px oklch(0% 0 0 / 0.7), 0 2px 6px oklch(0% 0 0 / 0.5)`): o QR e a dica de módulo.

## Shapes

Cantos discretos: 4px em etiquetas, selos e opções do segmentado; 6px em campos e botões; 10px no veredito e nas violações; 999px só em chaves e barras. Filetes de 1px; o ativo ganha um filete de ouro por dentro (inset), nunca uma borda grossa.

## Components

### Buttons
- **Primary:** folha de ouro com tinta escura, 6px; só para baixar.
- **Hover:** ouro mais claro. **Disabled:** ouro escuro translúcido, texto em ouro apagado, e sempre o motivo escrito abaixo.
- **Secondary:** transparente com filete forte; hover em grafite elevado.
- **Ghost:** sem borda, champanhe apagado; hover com o texto em ouro (Inverter, Remover, Centralizar).

### Chips
- **Style:** etiqueta mono maiúscula com filete forte, fundo transparente.
- **State:** selecionada com borda e texto em ouro sobre ouro translúcido.

### Inputs / Fields
- **Style:** laca rebaixada, filete forte, 6px; hex e conteúdo em mono.
- **Focus:** borda de ouro + anel de ouro translúcido.
- **Slider:** trilho de 3px, preenchimento em ouro, polegar de ouro com aro de laca.
- **Segmented:** trilho rebaixado; a opção ativa vira grafite elevado com texto e filete de ouro.
- **Toggle:** trilho de grafite; ligado vira ouro com o polegar em tinta escura.

### Navigation
Cabeçalho de 56px em laca translúcida: selo de ouro com três olhos de QR, "MEC-QRSAFE" em mono de ouro espaçado, frase do produto em champanhe fraco, link do GitHub que acende em ouro.

### Veredito
Painel de 10px sobre o lavado da cor do estado, com filete da mesma cor: ícone, título em Alumni Sans 300, frase de consequência e a nota em Alumni Sans 100 com o rótulo "DE 100" em mono. As violações vêm abaixo, cada uma com o selo mono (BLOQUEIA / AVISO), a mensagem do servidor e o link "AJUSTAR EM <SEÇÃO>" em ouro.

### Barras dos blocos
Uma linha por bloco Reed-Solomon: amostra da cor do bloco, nome, barra (pátina, brasa acima de 75% da correção, vermelhão acima de 100%) com um traço no limiar e a contagem em mono. Passar o mouse acende os codewords do bloco no QR. A barra cresce por `scaleX`.

### Raio-X
Modo do palco: "estrutura" pinta os padrões fixos; "blocos" pinta cada módulo de dado na cor do seu bloco e os destruídos em vermelho. A placa do logo vira contorno tracejado.

## Do's and Don'ts

### Do:
- **Do** usar o ouro só para ação e escolha.
- **Do** etiquetar com mono maiúsculo espaçado (0.625rem, 0.24em).
- **Do** manter o QR nas cores do usuário, sobre o fundo claro dele.
- **Do** dizer o estado em texto além da cor.

### Don't:
- **Don't** voltar a um tema claro genérico; o usuário rejeitou esse visual.
- **Don't** usar ouro para estado nem pátina/brasa/vermelhão fora do veredito e das barras.
- **Don't** usar Alumni Sans abaixo de 2rem.
- **Don't** oferecer uma forma de módulo ou de canto que não esteja no teste contra o leitor do backend (ShapesAgainstReaderTest).
