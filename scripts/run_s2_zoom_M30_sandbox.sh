#!/usr/bin/env bash
# Sandbox: corre ksweep en la franja N ∈ [400, 600] con M=30 en un
# directorio AISLADO, sin tocar results/s2/.
#
# Output:
#   results/s2_sandbox/k_sweep.csv
#   results/s2_sandbox/energy_k*_N*.csv     (1 archivo por (k, N))
#   figures/sandbox_11_s2_k_sweep_zoom_M30.png  (al final, vía plotter)
#
# Rollback: bash scripts/rollback_sandbox.sh
#
# Uso: bash scripts/run_s2_zoom_M30_sandbox.sh
set -euo pipefail

cd "$(dirname "$0")/.."

SANDBOX="results/s2_sandbox"
NS="400,425,450,475,500,525,550,575,600"
KS="100,1000,10000"
M=30
TF=500

T0=$SECONDS
echo "=================================================================="
echo "  TP4 SDS — SANDBOX — ksweep franja [400,600] con M=$M"
echo "  Outdir: $SANDBOX  (NO se toca results/s2/)"
echo "  Ns: $NS"
echo "  ks: $KS"
echo "  realizaciones: $M"
echo "  Para deshacer:  bash scripts/rollback_sandbox.sh"
echo "=================================================================="

mkdir -p "$SANDBOX"

mvn -q compile

echo "------------------------------------------------------------------"
echo "[stage] KSWEEP_SANDBOX_M${M}"
mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 \
    -Dexec.args="--experiment ksweep --ks $KS --Ns $NS --tf $TF --realizations $M --outdir $SANDBOX"

echo "------------------------------------------------------------------"
echo "[stage] PLOT_SANDBOX"
python3 plotter/plot_s2_k_sweep_zoom_sandbox.py

T=$((SECONDS - T0))
printf "\nTOTAL: %02d:%02d:%02d\n" $((T/3600)) $(((T%3600)/60)) $((T%60))
echo "Sandbox CSV:    $SANDBOX/k_sweep.csv"
echo "Sandbox figura: figures/sandbox_11_s2_k_sweep_zoom_M30.png"
