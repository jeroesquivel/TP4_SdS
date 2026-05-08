#!/usr/bin/env bash
# Corrida rápida (~3-5 min) para ver todos los gráficos del TP4 con datos
# representativos pero no convergidos. NO usar para entrega — los J(N) no
# están bien promediados con tf y M tan chicos.
#
# Subset:
#   - S1 completo (es barato).
#   - S2 con Ns = {100, 400, 1000}, tf = 50 s, realizations = 2.
#   - ksweep con ks = {1e2, 1e3, 1e4} y los mismos Ns.
#
# Uso: bash scripts/run_quick.sh
set -euo pipefail

cd "$(dirname "$0")/.."
mkdir -p results/log results/s1 results/s2 figures

T_GLOBAL_START=$SECONDS

echo "=================================================================="
echo "  TP4 SDS — RUN QUICK (subset para visualización, NO entregable)"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=================================================================="
echo ""

# Limpieza: sacar resultados viejos con N fuera de [100, 1000] que pueden
# mezclarse con la corrida nueva y romper la grilla de plots.
echo "[clean] borrando results/s2/*_N{10,20,50}_* viejos..."
shopt -s nullglob
for stale in results/s2/cfc_N{10,20,50}_real*.csv \
             results/s2/energy_N{10,20,50}_real*.csv \
             results/s2/radial_N{10,20,50}.csv \
             results/s2/energy_k*_N{10,20,50}.csv \
             results/s2/energy_velocity_verlet_N{10,20,50}_*.csv; do
    rm -f "$stale"
done
shopt -u nullglob
# Sobreescribimos j_vs_n.csv y k_sweep.csv con los valores nuevos al correr.

echo ""
echo "[0/6] mvn compile ..."
T0=$SECONDS
mvn -q compile
T_COMPILE=$((SECONDS - T0))
echo "      → compile: ${T_COMPILE}s"
echo ""

run_stage() {
    local name="$1"; local args="$2"; local main="${3:-ar.edu.itba.sds.sistema2.Main2}"
    echo "------------------------------------------------------------------"
    echo "[stage] $name"
    echo "        main: $main"
    echo "        args: $args"
    echo "------------------------------------------------------------------"
    local t_start=$SECONDS
    mvn -q exec:java -Dexec.mainClass="$main" -Dexec.args="$args"
    local elapsed=$((SECONDS - t_start))
    echo "        → $name: ${elapsed}s"
    echo ""
    eval "T_${name}=$elapsed"
}

# Sistema 1 — barato (single-particle, ECM sweep).
run_stage S1     "" "ar.edu.itba.sds.sistema1.Main1"

# Sistema 2 — subset para que el barrido entero termine rápido.
QUICK_NS="100,400,1000"
QUICK_TF=50
QUICK_REALS=2

run_stage ENERGY "--experiment energy --N 200 --k 1000  --tf 5"
run_stage TIMING "--experiment timing --Ns ${QUICK_NS} --tf ${QUICK_TF} --k 1000"
run_stage JVSN   "--experiment jvsn   --Ns ${QUICK_NS} --tf ${QUICK_TF} --k 1000 --realizations ${QUICK_REALS}"
run_stage KSWEEP "--experiment ksweep --ks 100,1000,10000 --Ns ${QUICK_NS} --tf ${QUICK_TF} --realizations ${QUICK_REALS}"

# Plotters
echo "------------------------------------------------------------------"
echo "[plot] generando figures/*.png ..."
echo "------------------------------------------------------------------"
T0=$SECONDS
PY=${PY:-python3}
cd plotter
for p in plot_s1_r_vs_t.py plot_s1_ecm_vs_dt.py \
         plot_s2_energy.py plot_s2_timing.py plot_s2_cfc_t.py \
         plot_s2_j_vs_n.py plot_s2_radial.py plot_s2_k_sweep.py; do
    echo "  → $p"
    if ! $PY "$p"; then
        echo "    (warning: $p falló — sigo con los demás)"
    fi
done
cd ..
T_PLOT=$((SECONDS - T0))
echo "      → plot: ${T_PLOT}s"

T_TOTAL=$((SECONDS - T_GLOBAL_START))

format_hms() { printf "%02d:%02d:%02d" $(($1/3600)) $((($1%3600)/60)) $(($1%60)); }

echo ""
echo "=================================================================="
echo "  RESUMEN — run_quick"
echo "=================================================================="
printf "  %-14s %10s\n" "stage"          "elapsed"
printf "  %-14s %10s\n" "--------------" "----------"
printf "  %-14s %10ss\n" "compile"       "$T_COMPILE"
printf "  %-14s %10ss\n" "s1"            "$T_S1"
printf "  %-14s %10ss\n" "energy"        "$T_ENERGY"
printf "  %-14s %10ss\n" "timing"        "$T_TIMING"
printf "  %-14s %10ss\n" "jvsn"          "$T_JVSN"
printf "  %-14s %10ss\n" "ksweep"        "$T_KSWEEP"
printf "  %-14s %10ss\n" "plot"          "$T_PLOT"
printf "  %-14s %10s\n"  "--------------" "----------"
printf "  %-14s %10ss   %s\n" "TOTAL"     "$T_TOTAL" "$(format_hms $T_TOTAL)"
echo "=================================================================="
echo "  Output:  results/s1/, results/s2/"
echo "  Figuras: figures/*.png"
echo "=================================================================="
