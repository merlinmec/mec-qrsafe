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

## Status (2026-08-14)

Fase inicial de scaffold. Implementado de verdade: `GF256` +
`ReedSolomonEncoder` (aritmética de correção de erro), `QrMatrix`,
`SvgRenderer`, `ContrastRule`, e a API REST básica (`POST
/api/v1/qr-codes/validate`, ainda retorna HTTP 501 até o `QrEncoder`
estar completo). O restante do pipeline do encoder (modo/bitstream,
posicionamento na matriz, máscara, format info) e as regras de
`FinderPatternOverlapRule`/`QuietZoneRule` ainda são `TODO()` — ver
`DECISOES-E-TRADEOFFS.txt` seção 4 pra ordem de implementação sugerida.
