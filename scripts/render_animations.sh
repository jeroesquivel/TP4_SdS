#!/usr/bin/env bash
# Renderiza las animaciones del Sistema 2 como mp4 listos para LaTeX
# (paquete `multimedia`, macro \movie):
#   - Corre el motor Java para producir frames PNG (latex/figures/anim_N<N>/).
#   - Usa ffmpeg para mux PNG → H.264 mp4 en latex/videos/N=<N>.mp4.
#   - Copia el primer frame como poster (latex/figures/N=<N>.png).
#
# Uso:
#   ./scripts/render_animations.sh                 # default: tf=6s, 25fps, 480px
#   FPS=20 TF=8 WIDTH=400 ./scripts/render_animations.sh
#
# Requisitos: ffmpeg en el PATH (brew install ffmpeg).
set -euo pipefail

cd "$(dirname "$0")/.."

CAPTURE_FPS="${CAPTURE_FPS:-${FPS:-25}}"  # frames por segundo de simulación capturados
PLAY_FPS="${PLAY_FPS:-${FPS:-25}}"        # fps de reproducción del mp4
TF="${TF:-30}"
WIDTH="${WIDTH:-480}"
K="${K:-1000}"
KEEP_PNG="${KEEP_PNG:-0}"   # poner KEEP_PNG=1 para no borrar los frames sueltos
SPEEDUP=$(awk "BEGIN { print $PLAY_FPS / $CAPTURE_FPS }")
echo "  capture=${CAPTURE_FPS} fps_sim,  play=${PLAY_FPS} fps  →  speedup ${SPEEDUP}×"

if ! command -v ffmpeg >/dev/null 2>&1; then
  echo "ERROR: ffmpeg no encontrado. Instalá con 'brew install ffmpeg'." >&2
  exit 1
fi

mvn -q compile
mkdir -p latex/videos

for N in 100 1000; do
  PNG_DIR="latex/figures/anim_N${N}"
  MP4_OUT="latex/videos/N=${N}.mp4"
  POSTER="latex/figures/N=${N}.png"

  echo "→ Renderizando frames N=${N} (capture=${CAPTURE_FPS} fps_sim, tf=${TF}s, width=${WIDTH}px)"
  rm -rf "${PNG_DIR}"
  mvn -q exec:java \
    -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 \
    -Dexec.args="--experiment animate --N ${N} --k ${K} --tf ${TF} --fps ${CAPTURE_FPS} --width ${WIDTH} --out ${PNG_DIR}"

  echo "→ Mux PNG → mp4 a ${PLAY_FPS} fps (${MP4_OUT})"
  rm -f "${MP4_OUT}"
  # -pix_fmt yuv420p para compatibilidad amplia (Acrobat, pympress, VLC).
  # -movflags +faststart para que arranque rápido al abrir el PDF.
  ffmpeg -hide_banner -loglevel error -y \
    -framerate "${PLAY_FPS}" \
    -i "${PNG_DIR}/frame_%d.png" \
    -c:v libx264 -pix_fmt yuv420p -movflags +faststart \
    -vf "pad=ceil(iw/2)*2:ceil(ih/2)*2" \
    "${MP4_OUT}"

  cp "${PNG_DIR}/frame_0.png" "${POSTER}"

  if [ "${KEEP_PNG}" = "0" ]; then
    rm -rf "${PNG_DIR}"
  fi
done

echo
echo "Listo. Videos generados en latex/videos/:"
ls -lh latex/videos/*.mp4
