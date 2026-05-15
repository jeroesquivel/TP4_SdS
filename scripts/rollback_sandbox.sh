#!/usr/bin/env bash
# Rollback de sandboxes.
#
# Uso:
#   bash scripts/rollback_sandbox.sh                  # borra TODOS los sandboxes
#   bash scripts/rollback_sandbox.sh s2_sandbox_M50   # borra sólo ese
#
# Lo que toca:
#   results/s2_sandbox*       (todos los subdirs sandbox)
#   results/log/sb_*.log y s2_sandbox*.log
#   figures/sandbox_*.png
#
# Lo que NUNCA toca:
#   results/s2/, figures/*.png principales, código, scripts.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ $# -ge 1 ]]; then
    NAME="$1"
    DIR_GLOB="results/$NAME"
    FIG_GLOB="figures/sandbox_${NAME}*.png"
    LOG_GLOB="results/log/${NAME}.log"
    echo "Rollback sandbox específico: $NAME"
else
    DIR_GLOB="results/s2_sandbox*"
    FIG_GLOB="figures/sandbox_*.png"
    LOG_GLOB="results/log/s2_sandbox*.log results/log/sb_*.log"
    echo "Rollback sandbox: TODOS"
fi

shopt -s nullglob

# Dirs
DIRS=( $DIR_GLOB )
if [[ ${#DIRS[@]} -gt 0 ]]; then
    for d in "${DIRS[@]}"; do
        SIZE=$(du -sh "$d" 2>/dev/null | awk '{print $1}')
        N_FILES=$(find "$d" -type f | wc -l | tr -d ' ')
        rm -rf "$d"
        echo "  ✓ borrado $d  ($N_FILES archivos, $SIZE)"
    done
else
    echo "  · sin dirs sandbox que borrar"
fi

# Figures
FIGS=( $FIG_GLOB )
if [[ ${#FIGS[@]} -gt 0 ]]; then
    rm -f "${FIGS[@]}"
    echo "  ✓ borradas ${#FIGS[@]} figuras sandbox"
else
    echo "  · sin figuras sandbox que borrar"
fi

# Logs
LOGS=( $LOG_GLOB )
if [[ ${#LOGS[@]} -gt 0 ]]; then
    rm -f "${LOGS[@]}"
    echo "  ✓ borrados ${#LOGS[@]} logs sandbox"
else
    echo "  · sin logs sandbox que borrar"
fi

shopt -u nullglob
echo "Listo. results/s2/ y figures/*.png principales NO se tocaron."
