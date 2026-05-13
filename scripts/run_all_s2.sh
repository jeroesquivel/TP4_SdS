#!/usr/bin/env bash
# Corre los experimentos del Sistema 2 secuencialmente, mostrando progreso en stdout
# y un resumen final con tiempos por etapa.
# Uso: bash scripts/run_all_s2.sh
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p results/log

T_GLOBAL_START=$SECONDS

echo "=================================================================="
echo "  TP4 SDS — Sistema 2 — barrido completo"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=================================================================="
echo ""

echo "[0/4] mvn compile ..."
T0=$SECONDS
mvn -q compile
T_COMPILE=$((SECONDS - T0))
echo "      → compile: ${T_COMPILE}s"
echo ""

run_stage() {
    local name="$1"
    local args="$2"
    echo "------------------------------------------------------------------"
    echo "[stage] $name"
    echo "        args: $args"
    echo "------------------------------------------------------------------"
    local t_start=$SECONDS
    mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="$args"
    local elapsed=$((SECONDS - t_start))
    echo ""
    echo "        → $name: ${elapsed}s"
    echo ""
    eval "T_${name}=$elapsed"
}

run_stage ENERGY_K1E2 "--experiment energy --N 100 --k 100   --tf 5"
run_stage ENERGY_K1E3 "--experiment energy --N 100 --k 1000  --tf 5"
run_stage ENERGY_K1E4 "--experiment energy --N 100 --k 10000 --tf 5"
run_stage TIMING "--experiment timing --Ns 100,200,300,400,500,600,700,800,900,1000 --tf 500 --k 1000"
run_stage JVSN   "--experiment jvsn --Ns 100,200,300,400,500,600,700,800,900,1000 --tf 500 --k 1000 --realizations 10"
run_stage KSWEEP "--experiment ksweep --ks 100,1000,1500,5000,10000 --Ns 100,200,300,350,400,450,500,550,600,700,800,900,1000 --tf 500 --realizations 10"

T_TOTAL=$((SECONDS - T_GLOBAL_START))

format_hms() {
    local s=$1
    printf "%02d:%02d:%02d" $((s/3600)) $(((s%3600)/60)) $((s%60))
}

echo ""
echo "=================================================================="
echo "  RESUMEN — tiempos por etapa"
echo "=================================================================="
printf "  %-14s %10s   %s\n" "stage"          "elapsed"  "h:mm:ss"
printf "  %-14s %10s   %s\n" "--------------" "----------" "--------"
printf "  %-14s %10ss   %s\n" "compile"        "$T_COMPILE"     "$(format_hms $T_COMPILE)"
printf "  %-14s %10ss   %s\n" "energy_k1e2"    "$T_ENERGY_K1E2" "$(format_hms $T_ENERGY_K1E2)"
printf "  %-14s %10ss   %s\n" "energy_k1e3"    "$T_ENERGY_K1E3" "$(format_hms $T_ENERGY_K1E3)"
printf "  %-14s %10ss   %s\n" "energy_k1e4"    "$T_ENERGY_K1E4" "$(format_hms $T_ENERGY_K1E4)"
printf "  %-14s %10ss   %s\n" "timing"         "$T_TIMING"      "$(format_hms $T_TIMING)"
printf "  %-14s %10ss   %s\n" "jvsn"           "$T_JVSN"        "$(format_hms $T_JVSN)"
printf "  %-14s %10ss   %s\n" "ksweep"         "$T_KSWEEP"      "$(format_hms $T_KSWEEP)"
printf "  %-14s %10s   %s\n"  "--------------" "----------"     "--------"
printf "  %-14s %10ss   %s\n" "TOTAL"          "$T_TOTAL"       "$(format_hms $T_TOTAL)"
echo "=================================================================="
echo "  Output: results/s2/   (CSVs)"
echo "  Log:    results/log/  (si redirigís stdout)"
echo "=================================================================="
