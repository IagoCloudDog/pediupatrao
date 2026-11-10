# API do Dashboard — Documentação dos Endpoints

Base: `http://localhost:8080/api/dashboard` · Formato: JSON (UTF-8) · Métodos: somente `GET`.

## Autenticação e autorização

A API usa a **mesma sessão** da aplicação (login por formulário do Spring Security, cookie
`JSESSIONID`). O dashboard no navegador já envia o cookie automaticamente.

| Perfil | Acesso |
|--------|--------|
| ADMIN | permitido |
| GERENTE | permitido |
| ATENDENTE | **403 Forbidden** |
| não autenticado | **401 Unauthorized** |

A restrição é aplicada **no backend** em duas camadas: regra de URL no `SecurityConfig`
(`/api/dashboard/**` → `hasAnyRole('ADMIN','GERENTE')`) e `@PreAuthorize` no `DashboardService`.

Para testar com `curl` fora do navegador (o login exige o token CSRF):

```bash
# 1) obter o token CSRF do formulario de login (campo hidden _csrf)
TOKEN=$(curl -s -c c.txt -b c.txt http://localhost:8080/login \
        | grep -o 'name="_csrf"[^>]*value="[^"]*"' | sed 's/.*value="\([^"]*\)".*/\1/')
# 2) autenticar
curl -s -c c.txt -b c.txt -o /dev/null \
     -d "username=gerente&password=teste123&_csrf=$TOKEN" http://localhost:8080/login
# 3) chamar a API
curl -s -b c.txt "http://localhost:8080/api/dashboard/resumo?inicio=2026-10-01&fim=2026-10-31"
```

O script `scripts/testar-api-dashboard.sh` automatiza esses passos.

## Parâmetros de filtro comuns

Todos os endpoints de dados (todos, exceto `/filtros`) aceitam:

| Parâmetro | Tipo | Obrigatório | Padrão | Descrição |
|-----------|------|-------------|--------|-----------|
| `inicio` | data `yyyy-MM-dd` | não | `fim` − 29 dias | Início do período (inclusivo, 00:00). |
| `fim` | data `yyyy-MM-dd` | não | hoje | Fim do período (inclusivo, até 23:59:59). |
| `status` | texto | não | todos | Um dos valores: `RECEBIDO`, `EM_PREPARACAO`, `PRONTO`, `SAIU_PARA_ENTREGA`, `RETIRADO`, `FINALIZADO`, `CANCELADO` (aceita minúsculas). |
| `produtoId` | texto | não | todos | Considera apenas pedidos que contêm o produto (id em `/filtros`). |

Regras de validação: `inicio` ≤ `fim`; período de no máximo **366 dias**.
O período considera a data de entrada do pedido (`dataHoraEntrada`).

Todas as respostas trazem o objeto `periodo` com o intervalo efetivamente consultado:

```json
"periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 }
```

## Formato de erro (todas as falhas)

```json
{
  "timestamp": "2026-11-09T23:10:42.123",
  "status": 400,
  "erro": "Bad Request",
  "mensagem": "Periodo invalido: 'inicio' (2026-11-10) e posterior a 'fim' (2026-11-01).",
  "caminho": "/api/dashboard/resumo"
}
```

| HTTP | Quando ocorre |
|------|---------------|
| 400 Bad Request | `inicio` > `fim`; período > 366 dias; data fora do formato `yyyy-MM-dd`; `status` inexistente; `agrupamento` inválido; `limite` não numérico ou fora de 1–50. |
| 401 Unauthorized | Requisição sem sessão autenticada. |
| 403 Forbidden | Usuário autenticado sem perfil ADMIN/GERENTE (ex.: ATENDENTE). |
| 500 Internal Server Error | Falha inesperada (ex.: MongoDB indisponível). A mensagem é genérica; o detalhe fica só no log do servidor. |

---

## 1. `GET /resumo` — Indicadores gerais (RF01)

Parâmetros: filtros comuns.

Exemplo de requisição:
```
GET /api/dashboard/resumo?inicio=2026-10-11&fim=2026-11-09
```

Resposta `200 OK`:
```json
{
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 },
  "faturamentoBruto": 25432.0,
  "descontosConcedidos": 512.7,
  "faturamentoLiquido": 24919.3,
  "totalPedidos": 186,
  "pedidosValidos": 173,
  "pedidosCancelados": 13,
  "taxaCancelamento": 6.99,
  "ticketMedio": 144.04,
  "itensVendidos": 672,
  "faturamentoLiquidoPeriodoAnterior": 12011.5,
  "variacaoFaturamento": 107.46,
  "pedidosValidosPeriodoAnterior": 84,
  "variacaoPedidos": 105.95
}
```

| Campo | Descrição |
|-------|-----------|
| `faturamentoBruto` | Soma de `subtotal` dos pedidos válidos (não cancelados). |
| `descontosConcedidos` | Soma de `valorDesconto` dos pedidos válidos. |
| `faturamentoLiquido` | Soma de `valorTotal` dos pedidos válidos. |
| `totalPedidos` | Todos os pedidos do período (inclusive cancelados). |
| `pedidosValidos` / `pedidosCancelados` | Pedidos não cancelados / cancelados. |
| `taxaCancelamento` | `pedidosCancelados / totalPedidos × 100`. |
| `ticketMedio` | `faturamentoLiquido / pedidosValidos`. |
| `itensVendidos` | Soma das quantidades dos itens dos pedidos válidos. |
| `*PeriodoAnterior` | Mesmos indicadores no período anterior de mesma duração. |
| `variacaoFaturamento` / `variacaoPedidos` | Variação % vs período anterior; `null` se o anterior for zero. |

Erros: 400, 401, 403, 500.

---

## 2. `GET /vendas/serie` — Faturamento ao longo do tempo (RF02)

| Parâmetro adicional | Tipo | Padrão | Descrição |
|---------------------|------|--------|-----------|
| `agrupamento` | `DIA` \| `SEMANA` \| `MES` | `DIA` | Granularidade. Semanas começam na segunda-feira; o rótulo é a data de início do intervalo. |

Exemplo: `GET /api/dashboard/vendas/serie?inicio=2026-11-01&fim=2026-11-09&agrupamento=SEMANA`

Resposta `200 OK`:
```json
{
  "periodo": { "inicio": "2026-11-01", "fim": "2026-11-09", "dias": 9 },
  "agrupamento": "SEMANA",
  "pontos": [
    { "rotulo": "2026-10-26", "faturamento": 1210.5, "pedidos": 9 },
    { "rotulo": "2026-11-02", "faturamento": 6532.0, "pedidos": 44 },
    { "rotulo": "2026-11-09", "faturamento": 610.0, "pedidos": 4 }
  ]
}
```

Os intervalos sem venda aparecem com `faturamento: 0` e `pedidos: 0`. Considera apenas
pedidos válidos. Erros: 400 (`agrupamento` inválido ou filtros), 401, 403, 500.

---

## 3. `GET /vendas/por-hora` — Vendas por hora do dia (RF02)

Parâmetros: filtros comuns. Retorna sempre 24 posições (`chave` 0–23).

```json
{
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 },
  "tipo": "HORA",
  "pontos": [
    { "chave": 0, "rotulo": "00h", "faturamento": 0.0, "pedidos": 0 },
    { "chave": 19, "rotulo": "19h", "faturamento": 4102.0, "pedidos": 28 }
  ]
}
```
*(exemplo abreviado — a resposta real tem as 24 horas)*

## 4. `GET /vendas/por-dia-semana` — Vendas por dia da semana (RF02)

Parâmetros: filtros comuns. Retorna sempre 7 posições (`chave` 1 = segunda … 7 = domingo).

```json
{
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 },
  "tipo": "DIA_SEMANA",
  "pontos": [
    { "chave": 1, "rotulo": "seg.", "faturamento": 2101.0, "pedidos": 16 },
    { "chave": 5, "rotulo": "sex.", "faturamento": 5120.4, "pedidos": 36 }
  ]
}
```
*(exemplo abreviado)*

Erros (3 e 4): 400, 401, 403, 500.

---

## 5. `GET /pedidos/situacao` — Situação dos pedidos (RF03)

Parâmetros: filtros comuns. Retorna **todos** os 7 status (inclusive com quantidade 0),
contando também os cancelados.

```json
{
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 },
  "total": 186,
  "status": [
    { "status": "RECEBIDO", "quantidade": 2, "percentual": 1.08 },
    { "status": "EM_PREPARACAO", "quantidade": 1, "percentual": 0.54 },
    { "status": "PRONTO", "quantidade": 1, "percentual": 0.54 },
    { "status": "SAIU_PARA_ENTREGA", "quantidade": 1, "percentual": 0.54 },
    { "status": "RETIRADO", "quantidade": 1, "percentual": 0.54 },
    { "status": "FINALIZADO", "quantidade": 167, "percentual": 89.78 },
    { "status": "CANCELADO", "quantidade": 13, "percentual": 6.99 }
  ]
}
```

Erros: 400, 401, 403, 500.

---

## 6. `GET /produtos/mais-vendidos` — Ranking de produtos (RF04)

| Parâmetro adicional | Tipo | Padrão | Faixa | Descrição |
|---------------------|------|--------|-------|-----------|
| `limite` | inteiro | 5 | 1–50 | Quantidade de posições do ranking. |

Exemplo: `GET /api/dashboard/produtos/mais-vendidos?limite=3`

```json
{
  "periodo": { "inicio": "2026-10-11", "fim": "2026-11-09", "dias": 30 },
  "limite": 3,
  "produtos": [
    { "posicao": 1, "produtoId": "6730f1...", "nome": "Pizza Calabresa", "quantidade": 210,
      "receita": 9450.0, "percentualQuantidade": 31.25 },
    { "posicao": 2, "produtoId": "6730f2...", "nome": "Pizza Marguerita", "quantidade": 150,
      "receita": 6300.0, "percentualQuantidade": 22.32 },
    { "posicao": 3, "produtoId": "6730f3...", "nome": "Pizza Portuguesa", "quantidade": 112,
      "receita": 5376.0, "percentualQuantidade": 16.67 }
  ]
}
```

Ordenação: quantidade (desc), depois receita (desc), depois nome. `receita` = soma dos
subtotais dos itens; `percentualQuantidade` = participação nas unidades vendidas do período.
Erros: 400 (`limite` fora de 1–50 ou não numérico; filtros), 401, 403, 500.

---

## 7. `GET /filtros` — Opções de filtro (RF05)

Sem parâmetros. Usado pelo front para montar os seletores.

```json
{
  "status": ["RECEBIDO", "EM_PREPARACAO", "PRONTO", "SAIU_PARA_ENTREGA", "RETIRADO", "FINALIZADO", "CANCELADO"],
  "agrupamentos": ["DIA", "SEMANA", "MES"],
  "produtos": [ { "id": "6730f6...", "nome": "Borda Recheada" }, { "id": "6730f1...", "nome": "Pizza Calabresa" } ],
  "periodoPadraoDias": 30,
  "periodoMaximoDias": 366
}
```

Erros: 401, 403, 500.

---

## Exemplos de erro

```text
GET /api/dashboard/resumo?inicio=31/12/2026
400 {"status":400,"erro":"Bad Request","mensagem":"Parametro 'inicio' invalido: '31/12/2026'. Esperado: LocalDate no formato yyyy-MM-dd.", ...}

GET /api/dashboard/resumo?status=ENTREGUE
400 {"status":400,"erro":"Bad Request","mensagem":"Status invalido: 'ENTREGUE'. Valores aceitos: RECEBIDO, ...", ...}

GET /api/dashboard/produtos/mais-vendidos?limite=0
400 {"status":400,"erro":"Bad Request","mensagem":"Limite invalido: 0. Informe um valor entre 1 e 50.", ...}

GET /api/dashboard/resumo   (logado como atendente)
403 {"status":403,"erro":"Forbidden","mensagem":"Acesso negado: perfil sem permissao para este recurso.", ...}

GET /api/dashboard/resumo   (sem login)
401 {"status":401,"erro":"Unauthorized","mensagem":"Autenticacao necessaria para acessar este recurso.", ...}
```

As respostas reais capturadas na execução estão em `docs/evidencias/`.
