# LaTeX entregables — TP4 SDS (G05)

Carpeta con la fuente LaTeX de los PDFs que pide la cátedra para el TP4.

## Entregables

| Archivo | Para qué |
|---|---|
| `ppt.tex` → `ppt.pdf` | Presentación oral (13 min) |
| `ppt_entregable.tex` → `ppt_entregable.pdf` | PDF entregable |

> **No hay informe.** El enunciado de TP4 sólo pide presentación + código zip (< 100 KB, sólo motor de simulación, **no LaTeX**).

## Animaciones

Las animaciones se generan **automáticamente** como **mp4 H.264** y quedan embebidas en el PDF mediante el paquete `multimedia` (macro `\movie`).

Pipeline:

```
Java (FrameExporter)  →  PNG frames  →  ffmpeg (H.264, yuv420p)  →  videos/N=*.mp4  →  \movie en LaTeX
```

El `poster` (frame inicial) queda como `figures/N=*.png` y se ve en cualquier visor; el video se reproduce con **Adobe Acrobat** (Reader/Pro) y con **pympress** para autoplay durante la presentación oral.

### Regenerar las animaciones

```bash
# Defaults: tf=6s, fps=25, width=480px, k=1000
./scripts/render_animations.sh

# Con parámetros explícitos
FPS=20 TF=8 WIDTH=400 ./scripts/render_animations.sh

# Conservar los PNG intermedios
KEEP_PNG=1 ./scripts/render_animations.sh
```

Requisitos: `ffmpeg` en el PATH (`brew install ffmpeg`).

El script:
1. Compila el motor Java.
2. Para `N ∈ {100, 1000}` con `k = 10^3 N/m`:
   - corre `Main2 --experiment animate` para generar las PNG;
   - muxea con `ffmpeg` a `latex/videos/N=<N>.mp4` (H.264, `yuv420p`, `+faststart`);
   - copia `frame_0.png` como poster en `latex/figures/N=<N>.png`;
   - borra los PNG intermedios (a menos que `KEEP_PNG=1`).

## Compilar

```bash
pdflatex ppt_entregable.tex
pdflatex ppt_entregable.tex     # 2da pasada para miniframes/refs
pdflatex ppt.tex
pdflatex ppt.tex
```

> Las dos pasadas son necesarias por los `\inserttotalframenumber` y los puntos de progreso de `miniframes`.

## Estructura

```
latex/
├── ppt.tex                  # versión oral
├── ppt_entregable.tex       # versión entregable
├── figures/
│   ├── 01_..10_*.png        # plots del Python plotter
│   ├── N=100.png            # poster (frame inicial) de la animación N=100
│   └── N=1000.png           # poster de la animación N=1000
├── videos/
│   ├── N=100.mp4            # animación N=100, H.264
│   └── N=1000.mp4           # animación N=1000, H.264
└── README.md
```

## Pendientes antes de imprimir

1. **Practicar la presentación con cronómetro** antes del 18/05/2026 10:00.
2. Abrir `ppt.pdf` con **Adobe Acrobat** o **pympress** para verificar que las animaciones autoplay.

## Decisiones de diseño (referencia rápida)

- Tema **Warsaw** + `miniframes` (puntos de progreso por sección).
- **Sin diapositiva de índice** (presentación corta).
- **Sin diapositiva final de bibliografía**: citas inline con `\framecite` en la esquina del frame específico.
- **Sistema 1**: solo 2 slides, antes del Sistema 2 (consigna explícita).
- **Sistema 2**: 2 animaciones máximo (TP3 fue penalizado por 5).
- Paleta gradual + colorbar para $N$ y $k$ (variables numéricas).
- Vectores en negrita sin itálica (`\vect{x}`); unidades con `\si{}`.
- Captions dentro del frame, no como `\caption{}`.
