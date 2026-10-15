# Calibração do limite de contraste com câmera real

A `ContrastRule` bloqueia o export abaixo de **4,5:1** (contraste de luminância relativa, a mesma
fórmula do WCAG). O número nasceu como ponto de partida (seção 5 de `DECISOES-E-TRADEOFFS.txt`),
com a promessa de conferir com celulares de verdade. Este documento registra essa conferência.

## Como o teste funciona

[`docs/calibracao/index.html`](calibracao/index.html) é uma página autocontida (abre direto no
navegador, sem servidor) com **46 QRs**, todos da mesma versão (3, nível M) e cada um com uma URL
própria (`?cal=NN`), para o celular mostrar qual cartão leu:

- **5 tons × 8 contrastes** sobre fundo branco: neutro, azul, vermelho, verde e laranja, em 1,5 · 2 ·
  2,5 · 3 · 3,5 · 4 · 4,5 · 6 :1. A cor de cada cartão é calculada por busca binária no caminho
  preto → tom → branco até cair no contraste pedido;
- **5 invertidos** (módulos claros sobre fundo escuro): 2 · 3 · 4,5 · 7 · 15 :1;
- **1 controle**: preto sobre branco, 21:1.

Os QRs aparecem um por vez, em ordem aleatória a cada rodada (para a ordem não enviesar), e a
pessoa responde com o teclado: **1** leu rápido (até ~2 s), **2** leu lento, **3** não leu (nada em
~6 s). No fim, a página gera o JSON da rodada.

## Rodada 1 — Galaxy A14, ambiente escuro, lendo da tela (2026-10-15)

Câmera nativa, celular a ~30 cm do monitor. Dados brutos em
[`calibracao/rodada-1-galaxy-a14-escuro-tela.json`](calibracao/rodada-1-galaxy-a14-escuro-tela.json).

| Contraste | neutro | azul | vermelho | verde | laranja |
|---|---|---|---|---|---|
| 1,5:1 | rápido | lento | rápido | lento | lento |
| 2:1 | rápido | lento | lento | rápido | lento |
| 2,5:1 | rápido | rápido | lento | rápido | rápido |
| 3:1 | rápido | rápido | rápido | rápido | rápido |
| 3,5:1 | lento | rápido | rápido | rápido | rápido |
| 4:1 | rápido | rápido | rápido | rápido | rápido |
| 4,5:1 | rápido | lento | rápido | rápido | rápido |
| 6:1 | rápido | rápido | rápido | rápido | rápido |

Invertidos (2 a 15:1) e o controle: todos lidos rápido.

**Resultado: nenhuma falha em 46 cartões, nem a 1,5:1.** Dos 9 "lentos", 7 estão em 2,5:1 ou
abaixo; de 3:1 para cima, só dois lentos isolados.

## O que isso muda (e o que não muda)

**O limite continua em 4,5:1.** Ler de uma tela num quarto escuro é o cenário mais fácil que
existe: a tela emite luz, não reflete, e a câmera ajusta a exposição para ela. Mesmo assim a
leitura já começa a piorar perto de 2,5:1 — o 4,5:1 deixa quase o dobro de margem sobre o ponto
onde a câmera começa a hesitar no cenário mais favorável. Afrouxar com base num celular lendo da
tela seria calibrar o produto pelo caso que menos importa: o QR de verdade vai impresso, com
reflexo, luz ambiente e câmeras piores.

Dois achados que ficam registrados:

- **Tom importa perto do limite.** Neutro e verde leram rápido a 1,5:1 e 2:1; azul e laranja
  hesitaram. A fórmula do WCAG já pesa o verde (0,7152) muito mais que o azul (0,0722), mas o
  sensor da câmera não enxerga exatamente como a curva de luminância humana.
- **O A14 leu QR invertido em todos os contrastes.** Isso é um dado de um Android. A câmera do
  iPhone tem histórico de não ler invertido; a regra hoje trata invertido igual ao normal, e isso
  só pode ser decidido com uma rodada num iPhone.

## Próximas rodadas

O que falta, em ordem de valor:

1. **Papel impresso, luz ambiente normal**, com o mesmo A14 — o caso real do produto.
2. **iPhone**, tela e papel — principalmente os invertidos.
3. Luz fraca no papel.

Para rodar: abrir `docs/calibracao/index.html`, dar um nome à rodada (celular e condição),
responder, copiar o JSON e salvar aqui ao lado como `rodada-N-<celular>-<condição>.json`, com a
tabela acrescentada neste documento.
