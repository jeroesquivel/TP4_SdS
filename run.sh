#!/usr/bin/env bash
# =============================================================================
#  TP4 SDS — Pipeline canónico
#  Corre TODO lo necesario para reproducir la presentación (latex/ppt.tex):
#    1. Sistema 1 — ECM vs Δt para 4 integradores (figuras 01, 02).
#    2. Sistema 2 — barrido Δt para validación de energía (figuras 03d, 03f).
#    3. Sistema 2 — timing vs N (figura 04).
#    4. Sistema 2 — Cfc(t) y ⟨J⟩ vs N con perfiles radiales
#       (figuras 05, 06, 07, 08, 13).
#    5. Sistema 2 — k-sweep ⟨J⟩ y ⟨J^in|S~2⟩ vs N por k (figuras 09, 10).
#    6. Animaciones N=100, N=1000, k=10², k=10⁴ (videos mp4 + pósters PNG).
#    7. Compila ppt.tex y ppt_entregable.tex.
#
#  Parámetros canónicos (matchean exactamente la presentación):
#    N ∈ {100,...,1000} paso 100         tf = 500 s
#    k ∈ {10², 10³, 10⁴, 10⁵} N/m        Δt₂ = 0.05 s
#    M = 10 realizaciones                Ventana ajuste J = [tf/2, tf]
#    Δt: Geometry.dtForK(k) — 5e-3, 1e-3, 1e-3, 5e-4 respectivamente.
#
#  Paralelismo:
#    Java usa 90% de los núcleos automáticamente (ForkJoinPool global).
#    Override con TP4_PARALLELISM=N bash run.sh (ej. TP4_PARALLELISM=4).
#    Animaciones se renderizan en paralelo (4 simultáneas).
#    Timing experiment NO se paraleliza (mide wall-clock t_exec(N)).
#
#  Uso:    bash run.sh
#  Opcional, skip etapas pesadas: SKIP_KSWEEP=1 bash run.sh
# =============================================================================
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p results/s1 results/s2 results/log figures latex/figures latex/videos

# Limpia outputs de corridas previas (CSVs por realización y figuras), para
# evitar contaminar el dataset con M residual de runs viejas.
# Por defecto NO borra los energy_dtscan_*.csv (es el stage más caro y ya
# corrió). Para forzar limpieza incluida: CLEAN_ENERGY=1 bash run.sh.
if [[ "${SKIP_CLEAN:-0}" != "1" ]]; then
  rm -f results/s2/cfc_N*_real*.csv \
        results/s2/energy_N*_real*.csv \
        results/s2/energy_k*_N*.csv \
        results/s2/radial_N*.csv \
        results/s2/j_vs_n.csv \
        results/s2/k_sweep.csv \
        results/s2/timing.csv \
        results/s1/*.csv \
        figures/0*_s*.png figures/1*_s*.png || true
  rm -rf results/s2/_backup_intermediate results/s2/*.tar.gz || true
  if [[ "${CLEAN_ENERGY:-0}" == "1" ]]; then
    rm -f results/s2/energy_dtscan_*.csv || true
  fi
fi

T0=$SECONDS
TS=$(date '+%Y%m%d_%H%M%S')

hms() { printf "%02d:%02d:%02d" $(($1/3600)) $((($1%3600)/60)) $(($1%60)); }

stage() {
  local name="$1"; shift
  echo
  echo "=================================================================="
  echo "  [$name]  $(date '+%H:%M:%S')"
  echo "=================================================================="
  local t0=$SECONDS
  "$@"
  local dt=$((SECONDS - t0))
  echo "  → $name: $(hms $dt)"
  eval "T_${name}=$dt"
}

# ---------------------------------------------------------------------------
# 0. Compile
# ---------------------------------------------------------------------------
stage COMPILE mvn -q compile

mvn_main() { mvn -q exec:java -Dexec.mainClass="$1" -Dexec.args="$2"; }

# ---------------------------------------------------------------------------
# 1. SISTEMA 1 — ECM sweep + trayectoria de referencia
# ---------------------------------------------------------------------------
stage S1 mvn_main ar.edu.itba.sds.sistema1.Main1 ""

# ---------------------------------------------------------------------------
# 2. SISTEMA 2 — barrido Δt para validación energética (k = 10³, N = 800, tf = 2000)
#    Produce energy_dtscan_k1e+03_dt*.csv → plot 03d, 03e, 03f.
#    COMENTADO: ya corrió (9 corridas en 23:20). Los CSVs están en results/s2/.
#    Si querés regenerar:  ENERGY=1 bash run.sh   (o descomentá el if).
# ---------------------------------------------------------------------------
if [[ "${ENERGY:-0}" == "1" ]]; then
  stage S2_ENERGY mvn_main ar.edu.itba.sds.sistema2.Main2 \
    "--experiment energy_dt_scan --ks 1000 --dts 5e-5,1e-4,5e-4,1e-3,2e-3,5e-3,1e-2,2e-2,5e-2 --tf 2000 --N 800"
fi

# ---------------------------------------------------------------------------
# 3. SISTEMA 2 — timing vs N (k = 10³, tf = 500, 1 realización)
# ---------------------------------------------------------------------------
NS="100,200,300,400,500,600,700,800,900,1000"
stage S2_TIMING mvn_main ar.edu.itba.sds.sistema2.Main2 \
  "--experiment timing --Ns $NS --tf 500 --k 1000"

# ---------------------------------------------------------------------------
# 4. SISTEMA 2 — j_vs_n + perfiles radiales (k = 10³, M = 10)
# ---------------------------------------------------------------------------
stage S2_JVSN mvn_main ar.edu.itba.sds.sistema2.Main2 \
  "--experiment jvsn --Ns $NS --tf 500 --k 1000 --realizations 10"

# ---------------------------------------------------------------------------
# 5. SISTEMA 2 — k-sweep (k = 10²..10⁵, M = 10)
# ---------------------------------------------------------------------------
if [[ "${SKIP_KSWEEP:-0}" != "1" ]]; then
  stage S2_KSWEEP mvn_main ar.edu.itba.sds.sistema2.Main2 \
    "--experiment ksweep --ks 100,1000,10000,100000 --Ns $NS --tf 500 --realizations 10"
fi

# ---------------------------------------------------------------------------
# 6. Plots — todos los figures/*.png del ppt
# ---------------------------------------------------------------------------
stage PLOT_S1 bash -c 'cd plotter && python3 plot_s1_r_vs_t.py && python3 plot_s1_ecm_vs_dt.py'
stage PLOT_S2 bash -c '
  cd plotter && \
  python3 plot_s2_energy_dt_scan.py && \
  python3 plot_s2_timing.py && \
  python3 plot_s2_cfc_t.py && \
  python3 plot_s2_j_vs_n.py && \
  python3 plot_s2_radial.py && \
  python3 plot_s2_jin_vs_tp3.py && \
  python3 plot_s2_k_sweep.py'

# Mirror a latex/figures/ para que el .tex compile sin --output-directory raro.
rsync -a --include='*.png' --exclude='*' figures/ latex/figures/

# ---------------------------------------------------------------------------
# 7. Animaciones (N=100, N=1000) y (k=100, k=10000) con N=500
#    Las 4 se renderizan en paralelo: cada una usa ~1 core de Java + ffmpeg.
# ---------------------------------------------------------------------------
render_anim() {
  local label="$1" N="$2" K="$3" TF="$4" OUT_PNG_DIR="$5"
  local POSTER="latex/figures/${label}.png"
  local MP4="latex/videos/${label}.mp4"
  mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 \
    -Dexec.args="--experiment animate --N ${N} --k ${K} --tf ${TF} --fps 25 --width 480 --out ${OUT_PNG_DIR}"
  rm -f "${MP4}"
  ffmpeg -hide_banner -loglevel error -y -framerate 25 \
    -i "${OUT_PNG_DIR}/frame_%d.png" \
    -c:v libx264 -pix_fmt yuv420p -movflags +faststart \
    -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2" "${MP4}"
  cp "${OUT_PNG_DIR}/frame_0.png" "${POSTER}"
  rm -rf "${OUT_PNG_DIR}"
}
export -f render_anim

if [[ "${SKIP_ANIMS:-0}" != "1" ]]; then
  mkdir -p latex/videos latex/figures
  T_ANIM_START=$SECONDS
  echo
  echo "=================================================================="
  echo "  [ANIMS] renderizando 4 animaciones en paralelo  $(date '+%H:%M:%S')"
  echo "=================================================================="
  # Cada animación corre con TP4_PARALLELISM=1 para no oversubscribir cores
  # (4 anims × ffmpeg ya satura el sistema).
  (TP4_PARALLELISM=1 render_anim "N=100"   100  1000  30 latex/figures/anim_N100)   &
  (TP4_PARALLELISM=1 render_anim "N=1000"  1000 1000  30 latex/figures/anim_N1000)  &
  (TP4_PARALLELISM=1 render_anim "k=100"   500  100   30 latex/figures/anim_k100)   &
  (TP4_PARALLELISM=1 render_anim "k=10000" 500  10000 30 latex/figures/anim_k10000) &
  wait
  T_ANIMS=$((SECONDS - T_ANIM_START))
  echo "  → ANIMS: $(hms $T_ANIMS)"
fi

# ---------------------------------------------------------------------------
# 8. Compile presentación (oral + entregable)
# ---------------------------------------------------------------------------
if command -v pdflatex >/dev/null 2>&1; then
  stage PPT bash -c 'cd latex && pdflatex -interaction=nonstopmode ppt.tex >/dev/null && pdflatex -interaction=nonstopmode ppt.tex >/dev/null'
  stage PPT_ENTREGABLE bash -c 'cd latex && pdflatex -interaction=nonstopmode ppt_entregable.tex >/dev/null && pdflatex -interaction=nonstopmode ppt_entregable.tex >/dev/null'
fi

T_TOTAL=$((SECONDS - T0))
echo
echo "=================================================================="
echo "  RESUMEN — pipeline completo"
echo "=================================================================="
printf "  %-22s %s\n" "compile"               "$(hms ${T_COMPILE:-0})"
printf "  %-22s %s\n" "s1 (ECM sweep)"        "$(hms ${T_S1:-0})"
printf "  %-22s %s\n" "s2 energy_dt_scan"     "$(hms ${T_S2_ENERGY:-0})"
printf "  %-22s %s\n" "s2 timing"             "$(hms ${T_S2_TIMING:-0})"
printf "  %-22s %s\n" "s2 jvsn"               "$(hms ${T_S2_JVSN:-0})"
printf "  %-22s %s\n" "s2 ksweep"             "$(hms ${T_S2_KSWEEP:-0})"
printf "  %-22s %s\n" "plot s1"               "$(hms ${T_PLOT_S1:-0})"
printf "  %-22s %s\n" "plot s2"               "$(hms ${T_PLOT_S2:-0})"
printf "  %-22s %s\n" "anims (paralelo x4)"   "$(hms ${T_ANIMS:-0})"
printf "  %-22s %s\n" "ppt"                   "$(hms ${T_PPT:-0})"
printf "  %-22s %s\n" "ppt entregable"        "$(hms ${T_PPT_ENTREGABLE:-0})"
echo "  ------------------------------------------------"
printf "  %-22s %s\n" "TOTAL"                 "$(hms $T_TOTAL)"
echo "=================================================================="
echo "  Resultados : results/{s1,s2}/"
echo "  Figuras    : figures/  →  latex/figures/"
echo "  Videos     : latex/videos/"
echo "  PDFs       : latex/ppt.pdf  +  latex/ppt_entregable.pdf"
echo "=================================================================="
