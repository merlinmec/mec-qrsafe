# mec-qrsafe — editor

Editor do mec-qrsafe em React + Vite + TypeScript. Visão geral, como rodar
a API e as decisões estão no [README da raiz](../README.md) e nas seções
7, 8, 9 e 16 do `DECISOES-E-TRADEOFFS.txt`.

```
npm install
npm run dev      # localhost:5173; /api vai pra API Spring em localhost:8080
npm test         # Vitest
npm run build    # checa tipos e gera dist/
npm run lint     # oxlint
```

## Organização

```
src/
├── api/          contrato e chamadas da API
├── design/       estado do QR (Context + useReducer) e o corpo das requisições
├── hooks/        validação ao vivo: debounce, cancelamento, /preview vs /validate
├── render/       desenho no canvas (funções puras) e a leitura da matriz
├── lib/          logo (leitura e limites), violações → seções, download
├── components/   painel, palco do QR, veredito, exportar, "Por dentro do QR"
└── styles/       tokens e base
```
