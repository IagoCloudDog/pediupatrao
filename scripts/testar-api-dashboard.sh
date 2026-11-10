#!/usr/bin/env bash
# Teste ponta a ponta da API do dashboard com a aplicacao rodando (MongoDB real).
# Uso: ./scripts/testar-api-dashboard.sh [base_url]   (padrao http://localhost:8080)
# Gera as evidencias em docs/evidencias/ (respostas JSON + resumo dos status HTTP).
set -u

BASE="${1:-http://localhost:8080}"
DIR="$(cd "$(dirname "$0")/.." && pwd)/docs/evidencias"
mkdir -p "$DIR"
RESUMO="$DIR/resultado-testes-api.txt"
FALHAS=0

login() { # $1 usuario, $2 arquivo de cookies
  local jar="$2"
  rm -f "$jar"
  # 1) GET /login: o Thymeleaf injeta no formulario o campo hidden _csrf (token mascarado,
  #    exigido pelo Spring Security 6). Usamos esse valor, e nao o cookie XSRF-TOKEN cru.
  local token
  token=$(curl -s -c "$jar" -b "$jar" "$BASE/login" \
          | grep -o 'name="_csrf"[^>]*value="[^"]*"' | sed 's/.*value="\([^"]*\)".*/\1/' | head -1)
  # 2) POST do formulario de login com o token CSRF
  curl -s -c "$jar" -b "$jar" -o /dev/null \
       --data-urlencode "username=$1" --data-urlencode "password=teste123" \
       --data-urlencode "_csrf=$token" "$BASE/login"
}

checar() { # $1 descricao, $2 jar ("" = anonimo), $3 caminho, $4 status esperado, $5 arquivo de saida (opcional)
  local saida="${5:-/tmp/_resp.json}"
  local args=(-s -o "$saida" -w "%{http_code}" -H "Accept: application/json")
  [ -n "$2" ] && args+=(-b "$2")
  local codigo
  codigo=$(curl "${args[@]}" "$BASE$3")
  local ok="OK"
  if [ "$codigo" != "$4" ]; then ok="FALHOU"; FALHAS=$((FALHAS + 1)); fi
  printf '%-7s | esperado %s | obtido %s | %-10s | GET %s\n' "$ok" "$4" "$codigo" "$1" "$3" | tee -a "$RESUMO"
}

: > "$RESUMO"
echo "Testes da API do dashboard - $(date '+%d/%m/%Y %H:%M:%S') - $BASE" | tee -a "$RESUMO"
echo "--------------------------------------------------------------------------------" | tee -a "$RESUMO"

GER=/tmp/cookies-gerente.txt; ADM=/tmp/cookies-admin.txt; ATD=/tmp/cookies-atendente.txt
login gerente "$GER"; login admin "$ADM"; login atendente "$ATD"

echo "# Autorizacao" | tee -a "$RESUMO"
checar "anonimo"   ""     "/api/dashboard/resumo" 401 "$DIR/erro-401-anonimo.json"
checar "atendente" "$ATD" "/api/dashboard/resumo" 403 "$DIR/erro-403-atendente.json"
checar "admin"     "$ADM" "/api/dashboard/resumo" 200
checar "gerente"   "$GER" "/api/dashboard/resumo" 200

echo "# Endpoints (gerente)" | tee -a "$RESUMO"
checar "gerente" "$GER" "/api/dashboard/resumo"                                   200 "$DIR/resumo.json"
checar "gerente" "$GER" "/api/dashboard/vendas/serie?agrupamento=SEMANA"          200 "$DIR/vendas-serie-semana.json"
checar "gerente" "$GER" "/api/dashboard/vendas/por-hora"                          200 "$DIR/vendas-por-hora.json"
checar "gerente" "$GER" "/api/dashboard/vendas/por-dia-semana"                    200 "$DIR/vendas-por-dia-semana.json"
checar "gerente" "$GER" "/api/dashboard/pedidos/situacao"                         200 "$DIR/pedidos-situacao.json"
checar "gerente" "$GER" "/api/dashboard/produtos/mais-vendidos?limite=5"          200 "$DIR/produtos-mais-vendidos.json"
checar "gerente" "$GER" "/api/dashboard/filtros"                                  200 "$DIR/filtros.json"
checar "gerente" "$GER" "/api/dashboard/resumo?status=CANCELADO"                  200 "$DIR/resumo-filtro-cancelado.json"

echo "# Validacao de parametros (gerente)" | tee -a "$RESUMO"
checar "gerente" "$GER" "/api/dashboard/resumo?inicio=2026-11-10&fim=2026-11-01"  400 "$DIR/erro-400-periodo.json"
checar "gerente" "$GER" "/api/dashboard/resumo?inicio=31/12/2026"                 400 "$DIR/erro-400-data.json"
checar "gerente" "$GER" "/api/dashboard/resumo?status=ENTREGUE"                   400 "$DIR/erro-400-status.json"
checar "gerente" "$GER" "/api/dashboard/vendas/serie?agrupamento=ANO"             400
checar "gerente" "$GER" "/api/dashboard/produtos/mais-vendidos?limite=0"          400 "$DIR/erro-400-limite.json"
checar "gerente" "$GER" "/api/dashboard/resumo?inicio=2020-01-01&fim=2026-11-01"  400

echo "# Tela (gerente / atendente)" | tee -a "$RESUMO"
checar "gerente"   "$GER" "/dashboard" 200
checar "atendente" "$ATD" "/dashboard" 403

echo "--------------------------------------------------------------------------------" | tee -a "$RESUMO"
if [ "$FALHAS" -eq 0 ]; then
  echo "RESULTADO: todos os testes passaram." | tee -a "$RESUMO"
else
  echo "RESULTADO: $FALHAS teste(s) falharam." | tee -a "$RESUMO"
fi
rm -f "$GER" "$ADM" "$ATD" /tmp/_resp.json
exit "$FALHAS"
