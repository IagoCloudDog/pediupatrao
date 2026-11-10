# Dashboard Administrativo — Documento de Requisitos

Sistema: **PediuPatrão** (pizzaria). Módulo: **Dashboard Administrativo + API de indicadores**.

## 1. Contexto e objetivo

O responsável pelo estabelecimento precisa acompanhar o desempenho das vendas, a situação
dos pedidos e as tendências da operação para tomar decisões (escala de funcionários nos
horários de pico, compras de insumos dos produtos mais vendidos, política de descontos,
controle de cancelamentos).

A solução é composta por:

- uma **API REST somente leitura** (`/api/dashboard/**`) que calcula os indicadores a partir
  dos pedidos persistidos no MongoDB;
- uma **tela de dashboard** (`/dashboard`) que consome essa API via JavaScript e exibe
  indicadores, gráficos, ranking e filtros.

## 2. Atores

| Ator | Acesso ao dashboard |
|------|---------------------|
| ADMIN | Sim |
| GERENTE (responsável pelo estabelecimento) | Sim |
| ATENDENTE | **Não** (bloqueado no backend: 403) |

## 3. Regras de negócio adotadas

| Código | Regra |
|--------|-------|
| RN01 | **Venda válida** = pedido com status diferente de `CANCELADO`. Somente vendas válidas entram em faturamento, ticket médio, série de vendas, distribuições por hora/dia e ranking de produtos. |
| RN02 | **Faturamento líquido** = soma de `valorTotal` (já com desconto). **Bruto** = soma de `subtotal`. **Descontos** = soma de `valorDesconto`. |
| RN03 | **Ticket médio** = faturamento líquido ÷ quantidade de pedidos válidos (0 quando não há pedidos). |
| RN04 | **Taxa de cancelamento** = cancelados ÷ total de pedidos do período × 100. |
| RN05 | A data de referência de um pedido é `dataHoraEntrada` (registro do pedido). |
| RN06 | Período padrão: **últimos 30 dias** até hoje. Início e fim são **inclusivos**. Período máximo: **366 dias**. |
| RN07 | **Comparação**: o resumo compara com o período imediatamente anterior de mesma duração. Sem base (período anterior = 0), a variação é `null` (não há divisão por zero). |
| RN08 | **Receita por produto** = soma dos subtotais dos itens (preço unitário × quantidade, antes do desconto do pedido, que é aplicado ao pedido como um todo). |
| RN09 | Ranking ordenado por quantidade vendida; empate desempata por receita e depois por nome. |
| RN10 | Valores monetários arredondados em 2 casas. |

## 4. Requisitos funcionais

### RF01 — Indicadores gerais de vendas
O dashboard deve exibir, para o período filtrado: faturamento líquido, faturamento bruto,
descontos concedidos, total de pedidos, pedidos válidos, pedidos cancelados, taxa de
cancelamento, ticket médio e itens vendidos, além da **variação percentual** do faturamento
e da quantidade de pedidos em relação ao período anterior.
*Endpoint:* `GET /api/dashboard/resumo`.

### RF02 — Análise de vendas (gráficos)
O dashboard deve exibir:
- gráfico de linha do faturamento ao longo do tempo, agrupado por **dia, semana ou mês**,
  com pontos zerados nos intervalos sem venda (linha contínua);
- gráfico de barras de pedidos por **hora do dia** (identificação do horário de pico);
- gráfico de barras de pedidos por **dia da semana** (tendência semanal).
*Endpoints:* `GET /api/dashboard/vendas/serie`, `/vendas/por-hora`, `/vendas/por-dia-semana`.

### RF03 — Situação dos pedidos
O dashboard deve exibir a quantidade e o percentual de pedidos em cada status
(RECEBIDO, EM_PREPARACAO, PRONTO, SAIU_PARA_ENTREGA, RETIRADO, FINALIZADO, CANCELADO),
em gráfico de rosca. Todos os status são sempre retornados (inclusive com zero).
*Endpoint:* `GET /api/dashboard/pedidos/situacao`.

### RF04 — Produtos mais vendidos
O dashboard deve exibir o ranking dos produtos mais vendidos (top 5, 10 ou 20) com
quantidade, receita e participação percentual nas unidades vendidas, em tabela e gráfico.
*Endpoint:* `GET /api/dashboard/produtos/mais-vendidos`.

### RF05 — Filtros de consulta
O usuário deve poder filtrar todos os indicadores por **período (início/fim)**, **status**
e **produto**, além de escolher o agrupamento da série e o tamanho do ranking. Deve haver
atalhos (últimos 7/30/90 dias, este mês) e botão para limpar os filtros. As opções de status
e produto são carregadas da API.
*Endpoint de apoio:* `GET /api/dashboard/filtros`.

### RF06 — Integração com a API
A tela não calcula indicadores: todos os dados vêm da API REST via `fetch` (JSON), na mesma
sessão autenticada do usuário. As chamadas das seções são feitas em paralelo.

### RF07 — Atualização e tratamento de dados
- Atualização **manual** (botão) e **automática a cada 60 s** (desligável), que não executa
  com a aba oculta nem se uma carga anterior ainda estiver em andamento.
- Exibição do horário da última atualização.
- Estados de **carregando**, **sem dados** (“Sem vendas no período”) e **erro** por seção,
  sem derrubar as demais seções (atualização parcial), com botão “Tentar novamente”.
- Respostas de requisições antigas são descartadas quando o filtro muda rapidamente.
- Validação no navegador (início > fim) e no servidor (400 com mensagem clara).
- Sessão expirada (401) redireciona para o login.
- Dados legados incompletos (sem itens, sem data) não quebram os cálculos.

## 5. Requisitos não funcionais

| Código | Categoria | Requisito |
|--------|-----------|-----------|
| RNF01 | Segurança | Acesso à tela e à API restrito a ADMIN e GERENTE, validado **no backend** (regra de URL no `SecurityConfig` + `@PreAuthorize` no serviço). |
| RNF02 | Segurança | A API é **somente leitura** (apenas GET); não expõe dados pessoais de clientes nem credenciais. |
| RNF03 | Segurança | Erros não expõem stacktrace nem detalhes internos (ex.: string de conexão); o detalhe fica apenas no log do servidor. |
| RNF04 | Interoperabilidade | Respostas em JSON com contrato estável e documentado; erros em formato único (`timestamp`, `status`, `erro`, `mensagem`, `caminho`). Em `/api/**`, 401/403 retornam JSON (não redirecionam para HTML). |
| RNF05 | Desempenho | A consulta ao MongoDB filtra no banco por período, status e produto (`dataHoraEntrada`, `status`, `itens.produtoId`), trazendo só os pedidos necessários; período limitado a 366 dias. |
| RNF06 | Usabilidade | Interface responsiva (Bootstrap 5), valores em formato brasileiro (R$, datas dd/mm), rótulos de status legíveis. |
| RNF07 | Acessibilidade | Campos com `label`, gráficos com `role="img"` e `aria-label`, ranking também em tabela (alternativa ao gráfico), mensagens de status com `aria-live`. |
| RNF08 | Manutenibilidade | Regras de cálculo isoladas em classe pura (`DashboardCalculator`), sem dependência de banco, cobertas por testes automatizados. |
| RNF09 | Testabilidade | Testes unitários e de integração web (JUnit 5, Mockito, MockMvc + Spring Security Test) executados com `./mvnw test`. |
| RNF10 | Portabilidade | Mesma stack do projeto (Java 21, Spring Boot 3.4, MongoDB, Thymeleaf); Chart.js servido localmente (`/plugins/chart.js`), sem nova dependência externa. |
| RNF11 | Confiabilidade | Dados de demonstração gerados de forma determinística e idempotente (configurável), sem afetar a auditoria. |

## 6. Decisões de análise e projeto

1. **API dedicada ao dashboard** (`/api/dashboard`), separada das APIs de CRUD: os endpoints
   são orientados aos *indicadores* que a tela precisa, e não às entidades. Isso evita que o
   front tenha de baixar todos os pedidos e calcular no navegador.
2. **Cálculo no servidor, em camada pura** (`DashboardCalculator`): as regras ficam testáveis
   sem banco e o resultado é o mesmo para qualquer cliente da API.
3. **Filtro no banco + agregação em Java**: a consulta já restringe por período/status/produto
   no MongoDB; a agregação é feita em memória, o que é suficiente para o volume de uma
   pizzaria e mantém o código simples. Se o volume crescer, as mesmas assinaturas podem
   passar a usar *aggregation pipeline* do MongoDB sem mudar o contrato da API.
4. **Um endpoint por seção do dashboard**: cada gráfico carrega e falha de forma independente
   (atualização parcial), e cada recurso pode ser reutilizado por outros clientes.
5. **Cancelados fora do faturamento, mas visíveis**: entram na situação dos pedidos e na taxa
   de cancelamento, que é um indicador de gestão importante.
6. **Série com intervalos vazios preenchidos**: o gráfico mostra queda real de vendas em vez
   de “pular” os dias sem venda.
7. **Segurança em duas camadas** e **erros em JSON** para `/api/**`, alinhado ao controle de
   acesso por perfil já existente no sistema.
8. **Dados de demonstração** (`DemoDataSeeder`) para o painel ter conteúdo realista (pico no
   jantar, fins de semana mais fortes, descontos e cancelamentos ocasionais).
