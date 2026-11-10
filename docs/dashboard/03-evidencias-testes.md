# Evidências de Testes — Dashboard

Três níveis de teste:

| Nível | Ferramenta | Como executar | O que valida |
|-------|-----------|---------------|--------------|
| Unitário (regras) | JUnit 5 | `./mvnw test` | Cálculos de indicadores, séries, distribuições, ranking, filtros e validações |
| Serviço | JUnit 5 + Mockito | `./mvnw test` | Montagem da consulta ao MongoDB a partir dos filtros; limites; opções de filtro |
| API + segurança | MockMvc + Spring Security Test (com o `SecurityConfig` real) | `./mvnw test` | 401/403/200 por perfil, erros 400/500 em JSON, sem vazamento de detalhes internos |
| Ponta a ponta | `curl` (script) | `./scripts/testar-api-dashboard.sh` com a aplicação rodando | Login real, banco MongoDB real, todos os endpoints e erros |

> Com JDK 17 como `JAVA_HOME` padrão, use
> `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw test`.

## 1. Testes automatizados (`./mvnw test`)

Resultado da execução (09/11/2026):

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.dashboard.DashboardRestControllerTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.dashboard.DashboardCalculatorTest
[INFO] Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.dashboard.FiltroDashboardTest
[INFO] Tests run: 6,  Failures: 0, Errors: 0, Skipped: 0 -- in com.umc.pediupatrao.dashboard.DashboardServiceTest
[INFO] Tests run: 40, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Relatórios detalhados: `target/surefire-reports/` (gerados a cada execução).

### Massa de dados usada nos testes unitários (`PedidosTestData`)

Período 01/11/2026 (domingo) a 09/11/2026 (segunda):

| Pedido | Entrada | Status | Itens | Subtotal | Desconto | Total |
|--------|---------|--------|-------|----------|----------|-------|
| p1 | 02/11 19:10 (seg) | FINALIZADO | 2× Pizza A (R$ 40) + 1× Refri (R$ 10) | 90 | 10% | 81 |
| p2 | 03/11 12:30 (ter) | RECEBIDO | 1× Pizza A | 40 | 0 | 40 |
| p3 | 03/11 20:00 (ter) | CANCELADO | 3× Pizza B (R$ 50) | 150 | 0 | 150 |
| p4 | 09/11 20:45 (seg) | FINALIZADO | 4× Refri | 40 | 0 | 40 |

Resultados esperados e verificados (o cancelado fica fora das vendas):

| Indicador | Esperado | Por quê |
|-----------|----------|---------|
| Faturamento bruto | R$ 170,00 | 90 + 40 + 40 |
| Descontos | R$ 9,00 | 10% de 90 |
| Faturamento líquido | R$ 161,00 | 81 + 40 + 40 |
| Pedidos (total / válidos / cancelados) | 4 / 3 / 1 | |
| Taxa de cancelamento | 25% | 1 de 4 |
| Ticket médio | R$ 53,67 | 161 ÷ 3 |
| Itens vendidos | 8 | 3 + 1 + 4 |
| Série semanal | 26/10: 0 · 02/11: R$ 121 (2) · 09/11: R$ 40 (1) | semanas começam na segunda |
| Mais vendido | Refri: 5 un., R$ 50, 62,5% | Pizza B não aparece (só em pedido cancelado) |
| Situação | FINALIZADO 2 (50%), RECEBIDO 1, CANCELADO 1, demais 0 | os 7 status sempre presentes |

### Casos de teste cobertos

| Classe | Casos |
|--------|-------|
| `DashboardCalculatorTest` | resumo; variação vs período anterior; variação nula sem base; período vazio (zeros, sem NaN); série por dia/semana/mês com intervalos vazios; vendas por hora (24 posições); por dia da semana (7 posições); situação com todos os status; situação vazia; ranking (ordem, receita, %, cancelados fora); limite; desempate por receita; ranking vazio; pedidos legados sem itens/data |
| `FiltroDashboardTest` | período padrão (30 dias); só início; início > fim (erro); limite de 366 dias; status case-insensitive e inválido; limites do dia inclusivos; período anterior; agrupamento padrão/inválido |
| `DashboardServiceTest` | consulta por período (`$gte`/`$lte` em `dataHoraEntrada`); filtros de status e `itens.produtoId`; resumo faz 2 consultas (atual + anterior); limite inválido rejeitado sem consultar o banco; limite padrão; opções de filtro ordenadas |
| `DashboardRestControllerTest` | anônimo → 401 JSON; ATENDENTE → 403 JSON nos 7 endpoints; GERENTE e ADMIN → 200; início > fim → 400; data mal formatada → 400 com formato esperado; status inválido → 400; agrupamento inválido → 400; limite não numérico → 400; erro interno → 500 sem vazar a mensagem original (que continha "senha") |

## 2. Teste ponta a ponta (aplicação + MongoDB real)

Pré-requisitos: MongoDB local rodando e a aplicação no ar (`./mvnw spring-boot:run`).

```bash
./scripts/testar-api-dashboard.sh
```

O script faz login real com `admin`, `gerente` e `atendente` (incluindo o token CSRF do
formulário), chama todos os endpoints e casos de erro, e grava:

- `docs/evidencias/resultado-testes-api.txt` — tabela esperado × obtido de cada chamada;
- `docs/evidencias/*.json` — respostas reais (resumo, série, por hora, por dia, situação,
  ranking, filtros e os erros 400/401/403).

Casos executados pelo script:

| Grupo | Chamada | Esperado |
|-------|---------|----------|
| Autorização | `/resumo` sem login | 401 |
| | `/resumo` como atendente | 403 |
| | `/resumo` como admin / gerente | 200 |
| Endpoints | `/resumo`, `/vendas/serie?agrupamento=SEMANA`, `/vendas/por-hora`, `/vendas/por-dia-semana`, `/pedidos/situacao`, `/produtos/mais-vendidos?limite=5`, `/filtros`, `/resumo?status=CANCELADO` | 200 |
| Validação | início > fim; `inicio=31/12/2026`; `status=ENTREGUE`; `agrupamento=ANO`; `limite=0`; período > 366 dias | 400 |
| Tela | `/dashboard` como gerente | 200 |
| | `/dashboard` como atendente | 403 |

Resultado da execução (09/11/2026 23:15:37), com a aplicação e o MongoDB reais:

```
# Autorizacao
OK      | esperado 401 | obtido 401 | anonimo    | GET /api/dashboard/resumo
OK      | esperado 403 | obtido 403 | atendente  | GET /api/dashboard/resumo
OK      | esperado 200 | obtido 200 | admin      | GET /api/dashboard/resumo
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/resumo
# Endpoints (gerente)
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/resumo
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/vendas/serie?agrupamento=SEMANA
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/vendas/por-hora
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/vendas/por-dia-semana
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/pedidos/situacao
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/produtos/mais-vendidos?limite=5
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/filtros
OK      | esperado 200 | obtido 200 | gerente    | GET /api/dashboard/resumo?status=CANCELADO
# Validacao de parametros (gerente)
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/resumo?inicio=2026-11-10&fim=2026-11-01
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/resumo?inicio=31/12/2026
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/resumo?status=ENTREGUE
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/vendas/serie?agrupamento=ANO
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/produtos/mais-vendidos?limite=0
OK      | esperado 400 | obtido 400 | gerente    | GET /api/dashboard/resumo?inicio=2020-01-01&fim=2026-11-01
# Tela (gerente / atendente)
OK      | esperado 200 | obtido 200 | gerente    | GET /dashboard
OK      | esperado 403 | obtido 403 | atendente  | GET /dashboard
RESULTADO: todos os testes passaram.
```

Problema encontrado durante esta etapa e corrigido: na primeira execução o script fazia o
login com o valor do cookie `XSRF-TOKEN`, e o Spring Security 6 recusava o login (ele espera
o token *mascarado* que o Thymeleaf injeta no campo `_csrf` do formulário). Resultado: todas
as chamadas autenticadas voltavam 401. O script passou a ler o `_csrf` do HTML do login. O
login pelo navegador não era afetado.

## 3. Teste manual da tela (roteiro)

| # | Passo | Resultado esperado |
|---|-------|--------------------|
| 1 | Login como `gerente`, menu **Dashboard** | Painel carrega com indicadores, 5 gráficos e ranking |
| 2 | Clicar em “Últimos 7 dias” | Todos os blocos são recarregados para o novo período |
| 3 | Filtrar status = Cancelado | Faturamento zerado; situação mostra só cancelados |
| 4 | Escolher um produto | Indicadores consideram só pedidos com esse produto |
| 5 | Agrupar por Semana / Mês | Gráfico de linha muda de granularidade |
| 6 | Início posterior ao fim | Mensagem de validação, nenhuma chamada à API |
| 7 | Período sem vendas (ex.: ano passado) | Mensagens “Sem vendas no período” em cada gráfico |
| 8 | Parar o MongoDB e clicar em Atualizar | Alerta de erro com “Tentar novamente”; tela não quebra |
| 9 | Aguardar 60 s com a atualização automática ligada | “Atualizado às …” muda de horário |
| 10 | Login como `atendente`; acessar `/dashboard` e `/api/dashboard/resumo` | Menu sem Dashboard; página de acesso negado e JSON 403 |
