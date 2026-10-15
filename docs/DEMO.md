# Demonstração pública (custo zero)

A demo é o editor completo rodando num serviço gratuito do Render: qualquer pessoa abre o link,
customiza um QR, vê a validação ao vivo e baixa o arquivo. Não há banco nem conta — o app não guarda
nada (gera → valida → exporta), então não há dado de visitante para isolar ou apagar.

## Como funciona

- **Um serviço só**: o `Dockerfile` da raiz faz o build do editor com o Vite e põe o resultado em
  `classpath:/static` do `qr-api`; a API serve o editor na raiz e a API em `/api/v1/...`. Na
  hospedagem gratuita, dois serviços seriam dois servidores dormindo e acordando.
- **Limite por IP** (`RateLimitFilter`): 120 requisições por minuto na API e 10 exportações por
  minuto. Desligado fora da demo; o `render.yaml` liga. O IP vem do `X-Forwarded-For` do proxy do
  Render (`server.forward-headers-strategy=native`): o Tomcat usa o último endereço não confiável da
  lista, então um cabeçalho inventado pelo visitante não renova a cota.
- **Memória**: no máximo 2 exportações ao mesmo tempo (um PNG de 4096 px ocupa ~64 MB enquanto é
  desenhado); a terceira espera até 10 s e, se não houver vaga, recebe 503. Corpo da requisição
  limitado a 2 MB.
- **Saúde**: `GET /api/v1/health` responde sem codificar nada; é o `healthCheckPath` do Render.

Medido localmente com os limites do plano gratuito (512 MB, 0,1 CPU, `docker run --memory 512m
--cpus 0.1`): a aplicação sobe em cerca de 1 minuto, usa ~145 MB parada e ~225 MB logo depois de
exportar um PNG de 4096 px (que leva ~5 s nessa CPU).

## Passo a passo

1. Em https://render.com: **New → Blueprint**, escolha o repositório `mec-qrsafe`. O Render lê o
   `render.yaml` da raiz; não há segredo para colar.
2. Espere o primeiro deploy (o build do Docker leva alguns minutos) e abra o endereço que ele mostrar.

Depois disso, todo merge no `master` publica a demo sozinho (`autoDeploy`).

## Sem ping para manter acordada

O plano gratuito dorme após 15 minutos sem acesso; acordar leva cerca de 1 minuto. As 750 horas
gratuitas por mês são **por workspace**, e o fin-mec e o Árvore já usam as delas com o ping em
horário comercial (cerca de 265 h cada). Um terceiro ping estouraria a cota e o Render suspenderia
os três. Por isso a demo do mec-qrsafe fica sem ping: quem abre o link depois de um tempo parado
espera o servidor acordar.
