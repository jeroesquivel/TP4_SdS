#!/usr/bin/env bash
# Zoom de la franja crítica N ∈ [400, 600]: corre sólo los Ns nuevos
# (425, 450, 475, 525, 550, 575) y appendea las filas a los CSVs existentes:
#   results/s2/k_sweep.csv
#   results/s2/j_vs_n.csv
#   results/s2/timing.csv
#
# Uso: bash scripts/run_s2_zoom.sh
#
# Pre-requisito: ya tener corrido `scripts/run_all_s2.sh` (los CSVs base
# deben existir con los Ns de a 100). Los plotters ordenan por N, así que
# el orden de inserción no importa.
set -euo pipefail

cd "$(dirname "$0")/.."

NS_ZOOM="425,450,475,525,550,575"

T0=$SECONDS
echo "=================================================================="
echo "  TP4 SDS — Sistema 2 — ZOOM N ∈ [400, 600]"
echo "  Ns nuevos: $NS_ZOOM"
echo "=================================================================="

mvn -q compile

run_stage() {
    local name="$1"
    local args="$2"
    echo "------------------------------------------------------------------"
    echo "[stage] $name  args: $args"
    local t_start=$SECONDS
    mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="$args"
    echo "        → ${name}: $((SECONDS - t_start))s"
}

run_stage TIMING_ZOOM "--experiment timing --Ns $NS_ZOOM --tf 500 --k 1000 --append true"
run_stage JVSN_ZOOM   "--experiment jvsn   --Ns $NS_ZOOM --tf 500 --k 1000 --realizations 10 --append true"
run_stage KSWEEP_ZOOM "--experiment ksweep --ks 100,1000,10000 --Ns $NS_ZOOM --tf 500 --realizations 10 --append true"

T=$((SECONDS - T0))
printf "\nTOTAL: %02d:%02d:%02d\n" $((T/3600)) $(((T%3600)/60)) $((T%60))
