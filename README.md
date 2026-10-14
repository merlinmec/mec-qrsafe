# mec-qrsafe

Gerador de QR codes customizáveis com um diferencial central: um motor de
**validação de escaneabilidade em tempo real**, que garante que a
customização visual (cor, forma, logo) escolhida ainda resulta num QR
legível por câmera — e recusa deixar exportar o que não passar. Usa um
encoder QR implementado do zero (Reed-Solomon sobre GF(256), sem lib
pronta tipo ZXing).

Antes de mexer no código, leia:

- **`DECISOES-E-TRADEOFFS.txt`** — arquitetura completa (módulos, API,
  regras de negócio) e por que cada decisão foi tomada.
- **`mec-explain.txt`** — guia de estudo: o que estudar pra entender o
  projeto de verdade, e como explicar ele pra alguém de fora. Documento
  vivo, atualizado conforme o projeto evolui.

## Estrutura

```
mec-qrsafe/
├── qr-engine/       encoder QR do zero — biblioteca pura, sem Spring
├── qr-validation/   motor de escaneabilidade — biblioteca pura, sem Spring
├── qr-render/       geração de SVG a partir da matriz — sem Spring
└── qr-api/          único módulo com Spring Boot
```

## Rodar

Requer Java 21 e Maven (ou o `JAVA_HOME` apontando pra um JDK 21 — nesta
máquina, `C:\Program Files\Java\jdk-21`, mesmo que o `JAVA_HOME` padrão da
sessão aponte pro 17).

```
mvn test                              # roda todos os testes do reactor
mvn -pl qr-api spring-boot:run        # sobe a API em localhost:8080
```

## Status (2026-10-14)

**Encoder completo**, do zero, sem lib pronta: byte mode (UTF-8), as 40
versões nos 4 níveis de correção, Reed-Solomon com intercalação de
blocos, padrões fixos, as 8 máscaras com as penalidades N1-N4 e
format/version info com BCH. A matriz sai com a quiet zone, com o tipo de
cada módulo (finder, timing, dado...) e com qual codeword cada módulo de
dado carrega, que é o que o validador consulta.

Testado em três camadas, como prevê a seção 4 de
`DECISOES-E-TRADEOFFS.txt`:

1. vetores da especificação (Reed-Solomon, format/version info,
   capacidade, posições dos alignment patterns);
2. matriz idêntica à do ZXing, módulo a módulo e com a mesma máscara
   escolhida, nas 40 versões × 4 níveis;
3. leitura real: o QR vira imagem e o leitor do ZXing devolve o texto
   original (acentos, japonês e emoji incluídos).

O ZXing entra só nos testes, como referência e como leitor.

**Validação completa**: contraste, logo encostando em padrões fixos
(finder, timing, format info), margem mínima de 4 módulos e a área do
logo pela conta real — quantos codewords ele destrói em cada bloco contra
quantos aquele bloco corrige. No modo AUTO, a correção sobe sozinha (M →
Q → H) até o logo caber. Conferido contra o leitor do ZXing: a regra de
área bloqueia exatamente quando o decodificador falha, e tudo que as
regras aprovam é lido da imagem.

A API `POST /api/v1/qr-codes/validate` funciona de ponta a ponta:

```json
{
  "content": { "type": "STATIC", "value": "https://exemplo.com.br/cardapio?mesa=12" },
  "style": {
    "darkColor": "#000000", "lightColor": "#FFFFFF",
    "logo": { "widthPercent": 25, "centerXPercent": 50, "centerYPercent": 50 },
    "quietZoneModules": 4
  },
  "errorCorrection": "AUTO"
}
```

```json
{ "score": 1.0, "passed": true, "violations": [], "errorCorrectionLevel": "H", "version": 5 }
```

Ainda faltam os endpoints `/preview` e `/export` e o frontend.
