#!/usr/bin/env bash
# Versión "smoke test" del barrido — N chicos y tf=10s para verificar el pipeline
# rápido. Para el barrido real, usar run_all_s2.sh.
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p results/log
T_GLOBAL_START=$SECONDS

echo "=================================================================="
echo "  TP4 SDS — Sistema 2 — SMOKE TEST (tf=10s, N chicos)"
echo "=================================================================="

T0=$SECONDS
mvn -q compile
T_COMPILE=$((SECONDS - T0))

run_stage() {
    local name="$1"; local args="$2"
    echo ""
    echo "[stage] $name :: $args"
    local t_start=$SECONDS
    mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="$args"
    local elapsed=$((SECONDS - t_start))
    echo "        → $name: ${elapsed}s"
    eval "T_${name}=$elapsed"
}

run_stage ENERGY "--experiment energy --N 100 --k 1000 --tf 5"
run_stage TIMING "--experiment timing --Ns 100,200,300 --tf 10 --k 1000"
run_stage JVSN   "--experiment jvsn --Ns 100,200 --tf 10 --k 1000 --realizations 2"
run_stage KSWEEP "--experiment ksweep --ks 100,1000 --Ns 100,200 --tf 10 --realizations 2"

T_TOTAL=$((SECONDS - T_GLOBAL_START))
format_hms() { printf "%02d:%02d:%02d" $(($1/3600)) $((($1%3600)/60)) $(($1%60)); }

echo ""
echo "=================================================================="
echo "  RESUMEN — smoke test"
echo "=================================================================="
printf "  %-10s %8s\n" "compile"  "${T_COMPILE}s"
printf "  %-10s %8s\n" "energy"   "${T_ENERGY}s"
printf "  %-10s %8s\n" "timing"   "${T_TIMING}s"
printf "  %-10s %8s\n" "jvsn"     "${T_JVSN}s"
printf "  %-10s %8s\n" "ksweep"   "${T_KSWEEP}s"
printf "  %-10s %8s   %s\n" "TOTAL"  "${T_TOTAL}s" "$(format_hms $T_TOTAL)"
echo "=================================================================="
