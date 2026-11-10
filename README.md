# PediuPatrão — Controle de acesso, gestão de pedidos e auditoria

**Vídeo de apresentação do dashboard:** https://drive.google.com/file/d/1HsuzFPmZ3Bxy5zHqeq7U5w3zgP4swF-s/view?usp=sharing

Evolução do sistema **PediuPatrão** (pizzaria) implementando controle de acesso por
perfil, regras de negócio de pedidos, fluxo de status e auditoria persistente,
conforme a Atividade Prática M1 — Engenharia de Software.

## Tecnologias

- Java 21
- Spring Boot 3.4
- Spring Security
- Spring Data MongoDB
- Thymeleaf (+ thymeleaf-extras-springsecurity6)

## Como executar

Pré-requisitos:

- **JDK 21**
- **MongoDB** rodando localmente em `localhost:27017` (a aplicação usa o banco
  `pizzaria`, conforme `src/main/resources/application.properties`).

1. Garanta que o MongoDB local está no ar. Para testar a conexão:

   ```bash
   mongosh --eval "db.runCommand({ ping: 1 })"   # deve retornar { ok: 1 }
   ```

   > Se não tiver o MongoDB instalado, instale o MongoDB Community Server e
   > inicie o serviço antes de rodar a aplicação.

2. Na raiz do módulo (`PediuPatrao`), rode:

   ```bash
   ./mvnw spring-boot:run
   ```

   > Se o seu `JAVA_HOME` apontar para um JDK anterior ao 21, aponte-o para o 21
   > ao rodar o Maven, por exemplo:
   > `JAVA_HOME=/caminho/para/jdk-21 ./mvnw spring-boot:run`

A aplicação sobe em `http://localhost:8080` e redireciona para `/login`. Na
primeira execução, o banco e os dados iniciais (usuários e produtos) são criados
automaticamente pelo seed.

### Usuários criados automaticamente (seed)

Na primeira execução, o `DataInitializer` cria três usuários (senha `teste123`):

| Usuário     | Perfil    |
|-------------|-----------|
| `admin`     | ADMIN     |
| `gerente`   | GERENTE   |
| `atendente` | ATENDENTE |

## Requisitos implementados

### RF01 — Controle de usuários e permissões
- Três perfis: **ADMIN**, **GERENTE**, **ATENDENTE** (`entity/Role.java`).
- `UsuarioDetailsService` passa a usar a role real persistida no banco.
- Autorização efetiva no backend com `@EnableMethodSecurity` + regras no
  `SecurityConfig` e `@PreAuthorize` nos serviços — ocultar botões não basta.
- Administração de usuários/perfis restrita ao **ADMIN**; a role informada é
  sempre validada, impedindo autoelevação de privilégios.
- CSRF reativado (via cookie) e proteção das rotas `/usuarios/**` e
  `/api/usuarios/**`.

### RF02 — Evolução do cadastro de pedidos
- `Pedido` passa a ter itens, quantidades, preços, desconto, valor total,
  responsável e data/hora de entrada (`entity/Pedido.java`, `entity/ItemPedido.java`).
- Os preços são obtidos do catálogo no **servidor** e o valor total é
  **recalculado no servidor**; valores enviados pela requisição são ignorados.

### RF03 — Controle de entrada, saída e transições
- Fluxo validado por `entity/StatusPedido.java`:
  `RECEBIDO → EM_PREPARACAO → PRONTO → (SAIU_PARA_ENTREGA | RETIRADO) → FINALIZADO`.
- Registro de responsável e data/hora na **entrada** e na **saída**.
- Transições inválidas são bloqueadas; pedidos finalizados/cancelados não
  aceitam mais alteração de status.

### RF04 — Cancelamentos e descontos (exclusivos do GERENTE)
- `aplicarDesconto` e `cancelarPedido` protegidos por `@PreAuthorize("hasRole('GERENTE')")`.
- Desconto percentual limitado a **20%** e permitido apenas antes da saída.
- Cancelamento exige **justificativa**, preserva o histórico e impede alterações
  posteriores.
- Ambas as operações são registradas na auditoria.

### RF05 — Auditoria de pedidos e clientes
- Auditoria persistente no MongoDB (`entity/Auditoria.java`, coleção `auditorias`).
- São auditadas: criação/alteração de pedidos, mudanças de status, descontos,
  cancelamentos e alterações cadastrais de clientes (endereço, telefone etc.).
- Cada registro guarda usuário + perfil, data/hora, operação, entidade + id,
  campos alterados (valor anterior/novo) e justificativa quando aplicável.
- Consulta restrita a **ADMIN** e **GERENTE** (`/auditoria` e `/api/auditoria`).
- Registros são imutáveis (sem endpoints de alteração/exclusão) e **senhas/tokens
  nunca são gravados**.

## Principais alterações no código

| Arquivo | Alteração |
|---------|-----------|
| `entity/Role.java` | **Novo** — enum de perfis + validação. |
| `entity/StatusPedido.java` | **Novo** — estados e transições válidas. |
| `entity/ItemPedido.java` | **Novo** — item com snapshot de preço. |
| `entity/Pedido.java` | Reescrito com itens, valores, responsáveis e datas. |
| `entity/Auditoria.java` | **Novo** — registro de auditoria. |
| `repository/AuditoriaRepository.java` | **Novo**. |
| `service/AuditoriaService.java` | **Novo** — registro e consulta restrita. |
| `service/PedidoService.java` | Cálculo no servidor, fluxo de status, desconto e cancelamento + auditoria. |
| `service/ClienteService.java` | Auditoria de alterações cadastrais. |
| `service/UsuarioService.java` | Restrição a ADMIN + validação de perfil. |
| `service/UsuarioDetailsService.java` | Usa a role real do banco. |
| `config/SecurityConfig.java` | CSRF, method security e regras por perfil. |
| `config/DataInitializer.java` | Cria admin/gerente/atendente. |
| `controller/PedidoController.java` | Endpoints de status, desconto e cancelamento. |
| `controller/AuditoriaController.java` / `AuditoriaRestController.java` | **Novos** — consulta de auditoria. |
| `templates/*` | Perfis no form, telas de pedidos e auditoria, acesso negado, menu por perfil. |

## Dashboard Administrativo (Atividade 2)

Painel em `http://localhost:8080/dashboard` (menu **Dashboard**, perfis **ADMIN** e
**GERENTE**) que consome a API REST `/api/dashboard/**`, também implementada no projeto.

| Requisito | Onde |
|-----------|------|
| RF01 Indicadores gerais | `GET /api/dashboard/resumo` — faturamento, ticket médio, cancelamentos, variação vs período anterior |
| RF02 Análise de vendas | `GET /api/dashboard/vendas/serie`, `/vendas/por-hora`, `/vendas/por-dia-semana` |
| RF03 Situação dos pedidos | `GET /api/dashboard/pedidos/situacao` |
| RF04 Produtos mais vendidos | `GET /api/dashboard/produtos/mais-vendidos` |
| RF05 Filtros | período, status, produto, agrupamento e tamanho do ranking (`GET /api/dashboard/filtros`) |
| RF06 Integração com a API | a tela usa `fetch` em todos os dados; nenhum cálculo no navegador |
| RF07 Atualização e tratamento | atualização manual e automática (60 s), estados de carregando/vazio/erro, erros JSON padronizados |

Na primeira execução são gerados **pedidos fictícios dos últimos 45 dias** para o painel ter
dados (`pediupatrao.seed.demo-pedidos=true` em `application.properties`; use `false` para
desligar). Eles têm `responsavelEntrada = "seed-demo"`.

Documentação:

- [Requisitos funcionais e não funcionais](docs/dashboard/01-requisitos.md)
- [Documentação dos endpoints](docs/dashboard/02-api-endpoints.md)
- [Evidências de testes](docs/dashboard/03-evidencias-testes.md)
- [Gitflow](docs/dashboard/04-gitflow.md)

Testes: `./mvnw test` (unitários + API com segurança) e, com a aplicação rodando,
`./scripts/testar-api-dashboard.sh` (ponta a ponta no MongoDB real).

Código: pacote `com.umc.pediupatrao.dashboard` (`DashboardRestController`,
`DashboardService`, `DashboardCalculator`, `FiltroDashboard`, `DashboardExceptionHandler`)
e template `templates/dashboard.html`.

## Observação sobre segurança

As restrições de perfil são aplicadas no **backend** (Spring Security +
`@PreAuthorize`). A ocultação de botões nas telas é apenas conveniência visual;
chamadas diretas às APIs por um perfil sem permissão retornam **HTTP 403**.
