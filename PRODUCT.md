# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

React + Vite + TypeScript, CSS próprio (tokens em CSS custom properties, sem framework de CSS), testes com Vitest + Testing Library. Escolhido pelo usuário em 2026-10-14. React já era decisão fechada (seção 7 de `DECISOES-E-TRADEOFFS.txt`); o preview é um renderer canvas próprio, sem lib de QR no client (seção 8).

Entrega: só local por enquanto — dev server do Vite apontando para a API Spring local. Deploy (jar único, demo pública) fica para depois.

## Users

Primário: recrutador ou pessoa técnica avaliando o portfólio do autor. Abre o projeto pelo portfólio ou pelo GitHub, tem poucos minutos, quer entender o que foi construído e ver que funciona de verdade. A tela é vitrine da engenharia (encoder do zero, correção de erro, validação) tanto quanto ferramenta.

Secundário (o usuário do produto em si): quem precisa de um QR customizado com logo pra imprimir — cardápio, cartão, cartaz — e não pode descobrir depois de impresso que ele não lê.

## Product Purpose

Gerador de QR code customizável (cores e degradê, forma dos módulos e dos cantos, logo, margem, nível de correção) que valida a escaneabilidade ao vivo e recusa exportar o que não lê. Sucesso: o avaliador entende em segundos que a validação é real — vê o logo crescer, a correção subir sozinha, o QR ser bloqueado no ponto exato em que deixaria de ler — e que o encoder por trás foi escrito do zero.

## Positioning

Os geradores do mercado (QRCode Monkey, QR.io, QRCodeChimp, Adobe Express, qr-code-styling) deixam customizar livremente sem checar se o resultado ainda escaneia. Aqui a validação é a conta real da correção de erro — quantos codewords o logo destrói em cada bloco Reed-Solomon contra quantos o bloco corrige —, conferida contra um leitor de verdade (ZXing), sobre um encoder QR próprio em Kotlin. O export é bloqueado no servidor, não só na interface.

## Operating Context

- API local `http://localhost:8080/api/v1/qr-codes`: `POST /validate`, `POST /preview` (validação + matriz crua com o tipo de cada módulo + caixa do logo), `POST /export?format=svg|png&size=N` (422 com os motivos se não passar). Contrato nas seções 6 e 16 de `DECISOES-E-TRADEOFFS.txt` e no README.
- Round-trip só quando conteúdo, logo ou nível mudam; cor é cosmética e repinta local no canvas (seção 8).
- Avaliação acontece no desktop, com o README e o código abertos ao lado; mobile precisa funcionar, mas não é o cenário principal.

## Capabilities and Constraints

- v1: QR estático, sem contas, sem persistência (gera → valida → exporta).
- Customização disponível no backend hoje: cor escura, cor clara, logo (quadrado com largura e centro em % do símbolo, imagem PNG/JPEG opcional até 1 MB e 2048 px), margem em módulos, nível de correção (AUTO, L, M, Q, H).
- Forma dos módulos (quadrado, arredondado, pontos), dos cantos (quadrado, arredondado, círculo) e degradê (linear ou radial) existem no backend, no export e no editor; cada combinação oferecida é lida pelo ZXing nos testes. Formas novas só entram com o mesmo teste.
- O logo é sempre desenhado sobre uma placa na cor clara (decisão da seção 15: placa escura imita um finder pattern).
- Validação no backend é a fonte única de verdade; o front não reimplementa regra.
- Interface em português (pt-BR).

## Brand Commitments

Nome: mec-qrsafe (prefixo "mec" usado nos projetos do autor: fin-mec, mec-lineup). Sem logo, paleta ou voz definidos.

Visual do editor (escolha do usuário em 2026-10-14, depois de ver direções mais autorais): o padrão da categoria, executado com capricho — opções numa coluna, QR grande ao lado, validação e exportar junto do QR. A régua de acabamento são QRCode Monkey, QRCodeChimp e Adobe Express (gerador de QR). O diferencial aparece no conteúdo (a conta por bloco, o raio-X da matriz), não numa estética fora do comum.

## Evidence on Hand

- Testes reais: 369 no backend, incluindo a varredura em que a regra de área bloqueia se e somente se o decodificador do ZXing falha.
- Exemplos de export reais gerados pela API (PNG e SVG lidos pelo ZXing).
- Não há usuários, depoimentos nem métricas de uso — não inventar.

## Product Principles

1. Mostrar a conta, não só o veredito: o avaliador precisa ver por que o QR passa ou não (codewords, blocos, capacidade).
2. A validação é do servidor; a interface explica e antecipa, nunca decide sozinha.
3. Nada prometido que o backend não faz.
4. Feedback contínuo sem atrapalhar: ajustar um controle nunca trava a interação.

## Accessibility & Inclusion

Sem requisito específico além do básico: contraste legível, foco visível, controles operáveis por teclado e o estado de validação comunicado em texto (não só por cor).
