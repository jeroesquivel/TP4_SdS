#!/usr/bin/env bash
# Sandbox parametrizado para correr ksweep en aislamiento.
# Diseñado para lanzarse VARIAS VECES EN PARALELO con SANDBOX_NAME distinto.
#
# Pre-requisito (una sola vez antes de spawnear):
#   bash scripts/prep_sandbox_parallel.sh
#
# Configuración via env vars (todas opcionales):
#   EXPERIMENT    "ksweep" o "jvsn"              (default: ksweep)
#   SANDBOX_NAME  subdir dentro de results/      (default: s2_sandbox)
#   NS            lista de N separada por coma   (default: franja [400,600] cada 25)
#   KS            (ksweep) lista de k            (default: 100,1000,10000)
#   K             (jvsn)   un solo k             (default: 1000)
#   M             realizaciones                  (default: 30)
#   TF            tiempo final                   (default: 500)
#   SEED          semilla base                   (default: 42)
#
# Ejemplos:
#   SANDBOX_NAME=sb_M30   M=30  SEED=42   bash scripts/run_s2_sandbox.sh
#   SANDBOX_NAME=sb_M50   M=50  SEED=100  bash scripts/run_s2_sandbox.sh
#   SANDBOX_NAME=sb_k5    KS=100000  M=30  SEED=500  bash scripts/run_s2_sandbox.sh
#   EXPERIMENT=jvsn  SANDBOX_NAME=sb_jvsn_M30  K=1000  M=30  SEED=42  bash scripts/run_s2_sandbox.sh
set -euo pipefail
cd "$(dirname "$0")/.."

EXPERIMENT="${EXPERIMENT:-ksweep}"
SANDBOX_NAME="${SANDBOX_NAME:-s2_sandbox}"
NS="${NS:-400,425,450,475,500,525,550,575,600}"
KS="${KS:-100,1000,10000}"
K="${K:-1000}"
M="${M:-30}"
TF="${TF:-500}"
SEED="${SEED:-42}"
PLOTTER="${PLOTTER:-}"   # vacío = elegir según EXPERIMENT

if [[ ! -f "target/cp.txt" || ! -d "target/classes" ]]; then
    echo "ERROR: falta prep. Corré una vez:  bash scripts/prep_sandbox_parallel.sh" >&2
    exit 1
fi

SANDBOX_DIR="results/$SANDBOX_NAME"
FIG_OUT="figures/sandbox_${SANDBOX_NAME}.png"
LOG_FILE="results/log/${SANDBOX_NAME}.log"

mkdir -p "$SANDBOX_DIR" "results/log"

CP_DEPS="$(cat target/cp.txt)"
if [[ -n "$CP_DEPS" ]]; then
    CP="target/classes:$CP_DEPS"
else
    CP="target/classes"
fi

T0=$SECONDS
{
    echo "=================================================================="
    echo "  SANDBOX  $SANDBOX_NAME  ($EXPERIMENT)"
    echo "  $(date '+%Y-%m-%d %H:%M:%S')"
    echo "  outdir = $SANDBOX_DIR"
    echo "  Ns     = $NS"
    if [[ "$EXPERIMENT" == "ksweep" ]]; then
        echo "  ks     = $KS"
    else
        echo "  k      = $K"
    fi
    echo "  M      = $M"
    echo "  tf     = $TF"
    echo "  seed   = $SEED"
    echo "=================================================================="
} | tee "$LOG_FILE"

if [[ "$EXPERIMENT" == "ksweep" ]]; then
    java -cp "$CP" ar.edu.itba.sds.sistema2.Main2 \
        --experiment ksweep \
        --ks "$KS" \
        --Ns "$NS" \
        --tf "$TF" \
        --realizations "$M" \
        --outdir "$SANDBOX_DIR" \
        --seed "$SEED" 2>&1 | tee -a "$LOG_FILE"

    PLOT_SCRIPT="${PLOTTER:-plotter/plot_s2_k_sweep_zoom_sandbox.py}"
    python3 "$PLOT_SCRIPT" \
        --indir "$SANDBOX_DIR" --out "$FIG_OUT" --label "$SANDBOX_NAME" 2>&1 | tee -a "$LOG_FILE"

elif [[ "$EXPERIMENT" == "jvsn" ]]; then
    java -cp "$CP" ar.edu.itba.sds.sistema2.Main2 \
        --experiment jvsn \
        --k "$K" \
        --Ns "$NS" \
        --tf "$TF" \
        --realizations "$M" \
        --outdir "$SANDBOX_DIR" \
        --seed "$SEED" 2>&1 | tee -a "$LOG_FILE"

    PLOT_SCRIPT="${PLOTTER:-plotter/plot_s2_j_vs_n_sandbox.py}"
    python3 "$PLOT_SCRIPT" \
        --indir "$SANDBOX_DIR" --out "$FIG_OUT" --label "$SANDBOX_NAME" 2>&1 | tee -a "$LOG_FILE"

else
    echo "ERROR: EXPERIMENT='$EXPERIMENT' no soportado (esperado: ksweep | jvsn)" | tee -a "$LOG_FILE"
    exit 1
fi

T=$((SECONDS - T0))
printf "\n[%s] TOTAL: %02d:%02d:%02d\n" "$SANDBOX_NAME" $((T/3600)) $(((T%3600)/60)) $((T%60)) | tee -a "$LOG_FILE"
if [[ "$EXPERIMENT" == "ksweep" ]]; then
    echo "[$SANDBOX_NAME] CSV:    $SANDBOX_DIR/k_sweep.csv"
else
    echo "[$SANDBOX_NAME] CSV:    $SANDBOX_DIR/j_vs_n.csv"
fi
echo "[$SANDBOX_NAME] figura: $FIG_OUT"
echo "[$SANDBOX_NAME] log:    $LOG_FILE"
