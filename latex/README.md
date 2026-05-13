# LaTeX entregables — TP4 SDS (G05)

Carpeta con la fuente LaTeX de los dos PDFs que pide la cátedra para el TP4.

## Entregables

| Archivo | Para qué | Contiene |
|---|---|---|
| `ppt.tex` | **Presentación oral** (13 min) | Animaciones embebidas via `\movie` (paquete `multimedia`). Proyectar con `pympress` para autoplay. |
| `ppt_entregable.tex` | **PDF entregable** | Frame estático de cada animación + link a YouTube (en lugar del video embebido). |

> **No hay informe.** El enunciado de TP4 sólo pide presentación + código zip (< 100 KB, sólo motor de simulación, **no LaTeX**).

## Compilar

```bash
# Versión entregable (PDF puro)
pdflatex ppt_entregable.tex
pdflatex ppt_entregable.tex   # 2da pasada para miniframes/refs

# Versión oral (animaciones embebidas)
pdflatex ppt.tex
pdflatex ppt.tex
```

## Estructura esperada

```
latex/
├── ppt.tex                  # versión oral
├── ppt_entregable.tex       # versión entregable
├── figures/                 # PNGs producidos por el plotter (copiados al crear la carpeta)
│   ├── 01_s1_r_vs_t.png
│   ├── 02_s1_ecm_vs_dt.png
│   ├── 03a_s2_energy_validation.png
│   ├── 04_s2_timing.png
│   ├── 05_s2_cfc_t.png
│   ├── 06_s2_j_vs_n.png
│   ├── 07_s2_radial.png
│   ├── 08_s2_radial_near.png
│   ├── 09_s2_k_sweep.png
│   ├── 10_s2_k_scalar.png
│   ├── N=100.png            # frame de animación (pendiente — copiar del Visualizer)
│   └── N=1000.png           # frame de animación (pendiente)
└── videos/                  # mp4 con las animaciones (sólo para ppt.tex)
    ├── N=100.mp4            # pendiente — exportar del Visualizer
    └── N=1000.mp4           # pendiente
```

## Pendientes antes de imprimir

1. **Subir los videos** `N=100.mp4` y `N=1000.mp4` a YouTube y reemplazar los `TBD` por el ID en `ppt_entregable.tex` (función `\simvideo`).
2. **Generar frames de animación** (`figures/N=100.png`, `figures/N=1000.png`) con el `AnimationViewer` del repo.
3. **Copiar mp4s** a `videos/` para la versión oral.
4. **Verificar links pegados**: la corrección del TP3 marcó caracteres no alfabéticos colados al copiar.
5. **Practicar la presentación con cronómetro** antes del 18/05/2026 10:00.

## Decisiones de diseño (referencia rápida)

- Tema **Warsaw** + `miniframes` (puntos de progreso por sección).
- **Sin diapositiva de índice** (presentación corta).
- **Sin diapositiva final de bibliografía**: citas inline con `\framecite` en la esquina del frame específico.
- **Sistema 1**: solo 2 slides, antes del Sistema 2 (consigna explícita).
- **Sistema 2**: 2 animaciones máximo (TP3 fue penalizado por 5).
- Paleta gradual + colorbar para $N$ y $k$ (variables numéricas).
- Vectores en negrita sin itálica (`\vect{x}`); unidades con `\si{}`.
- Captions dentro del frame, no como `\caption{}`.
