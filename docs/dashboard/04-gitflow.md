# Gitflow da implementação do Dashboard

O enunciado exige a implementação da API em uma **branch seguindo o gitflow**.

## Branches

| Branch | Papel |
|--------|-------|
| `main` | Versões entregues (produção). Contém a entrega da Atividade M1. |
| `develop` | Integração. Criada a partir de `main`; recebe as features concluídas. |
| `feature/dashboard-api` | Implementação da API, da tela, dos testes e da documentação do dashboard. |
| `release/2.0.0` | Preparação da versão do dashboard (ajustes finais), depois integrada em `main` e `develop`. |

Fluxo: `main` → `develop` → `feature/dashboard-api` → (merge) `develop` → `release/2.0.0` → (merge) `main` + tag `v2.0.0` → (merge de volta) `develop`.

## Comandos

```bash
cd ~/atividade-alessandro/"PediuPatrao (1)"/PediuPatrao

# 1) develop a partir de main
git checkout main
git checkout -b develop
git push -u origin develop

# 2) feature a partir de develop
git checkout -b feature/dashboard-api

# commits da feature (um por assunto)
git add pom.xml src/main/java/com/umc/pediupatrao/dashboard src/main/java/com/umc/pediupatrao/config/SecurityConfig.java
git commit -m "feat(dashboard): API REST de indicadores com filtros e tratamento de erros"

git add src/main/java/com/umc/pediupatrao/config/DemoDataSeeder.java src/main/resources/application.properties
git commit -m "feat(dashboard): dados de demonstracao para o painel"

git add src/main/resources/templates/dashboard.html src/main/resources/templates/fragments/sidebar.html
git commit -m "feat(dashboard): tela do dashboard integrada a API"

git add src/test scripts
git commit -m "test(dashboard): testes unitarios, de API e script ponta a ponta"

git add docs README.md
git commit -m "docs(dashboard): requisitos, endpoints, evidencias de testes e gitflow"

git push -u origin feature/dashboard-api

# 3) integrar a feature em develop (merge sem fast-forward preserva o historico da feature)
git checkout develop
git merge --no-ff feature/dashboard-api -m "Merge feature/dashboard-api into develop"
git push

# 4) release e entrega em main
git checkout -b release/2.0.0
git push -u origin release/2.0.0
git checkout main
git merge --no-ff release/2.0.0 -m "Release 2.0.0: dashboard administrativo"
git tag -a v2.0.0 -m "Dashboard administrativo"
git push && git push origin v2.0.0

# 5) sincronizar develop com main
git checkout develop
git merge --no-ff main -m "Merge main into develop"
git push
```

Alternativa: em vez dos merges locais dos passos 3 e 4, abrir *Pull Requests* no GitHub
(`feature/dashboard-api` → `develop` e `release/2.0.0` → `main`), o que deixa o fluxo
visível na aba **Pull requests** do repositório.

Para conferir: `git log --oneline --graph --all`.
