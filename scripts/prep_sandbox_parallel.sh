#!/usr/bin/env bash
# One-shot prep antes de lanzar sandboxes en paralelo:
#   1. Compila el código una vez.
#   2. Materializa el classpath en target/cp.txt, así los runs paralelos no
#      necesitan volver a invocar Maven (evita race en target/).
#
# Uso: bash scripts/prep_sandbox_parallel.sh
set -euo pipefail
cd "$(dirname "$0")/.."

echo "[prep] compilando..."
mvn -q compile

echo "[prep] generando classpath..."
mvn -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=runtime

echo "[prep] OK. Ya podés lanzar sandboxes en paralelo con scripts/run_s2_sandbox.sh"
echo "       target/classes lista, classpath en target/cp.txt"
