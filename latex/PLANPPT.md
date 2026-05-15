# PLANPPT — Qué falta para cerrar la presentación

Estado al 2026-05-15. Ambos `.tex` (`ppt.tex` y `ppt_entregable.tex`) tienen la sección **Resultados** ya reestructurada según el orden pedido. La portada, el bloque introductorio (Sistema 1, Sistema 2 — Sistema particular / Motor / Observables / Parámetros) y las Conclusiones **no se tocaron en esta pasada** y quedan pendientes de revisión.

Los placeholders rojos visibles en el PDF compilado indican gráficos o animaciones que faltan. Cada uno tiene una nota con instrucciones.

---

## 1. Gráficos faltantes en `latex/figures/`

| Archivo esperado | Origen | Acción |
|---|---|---|
| `latex/figures/03d_s2_energy_dt_scan.png` | Generado por `plotter/plot_s2_energy_dt_scan.py` en `figures/` (raíz) | **Copiar** de `figures/` a `latex/figures/`, o cambiar `FIGURES` en `plotter/_style.py` para que apunte directamente a `latex/figures/`. |
| `latex/figures/03f_s2_energy_dt_slope.png` | Idem (gráfico nuevo de `\|S_e\|` vs `Δt`) | Idem. |

### Mejoras sugeridas en estos gráficos (opcionales pero recomendadas)

- **`03d_s2_energy_dt_scan.png`**: hoy es un mosaico con un panel por `k` (10², 10³, 10⁴). Para la slide del Sistema 2 sólo importa **el panel `k = 10³`**. Idealmente:
  - Generar una variante **single-panel** (solo `k = 10³`) y guardarla como `03d_s2_energy_dt_scan_k1e3.png`, o
  - Agregar una opción `--single-k 1000` al plotter que filtre el `discover()`.
- **`03f_s2_energy_dt_slope.png`**: hoy muestra `|S_e|` vs `Δt` con una curva por `k`. Para la slide:
  - Marcar con una **línea vertical** el `Δt` elegido para `k = 10³` (probablemente `Δt = 2·10⁻³ s`, a confirmar leyendo el plot).
  - Idealmente, un zoom sobre la curva `k = 10³`.

---

## 2. Animaciones faltantes

### Animaciones variando `N` (sección "Animaciones I")

✅ `latex/figures/N=100.png` y `videos/N=100.mp4` — listas.
✅ `latex/figures/N=1000.png` y `videos/N=1000.mp4` — listas.
- Si los `.mp4` no existen todavía, regenerar con `scripts/render_animations.sh`.

### Animaciones variando `k` (sección "Animaciones II", N fijo) — **FALTAN**

| Archivo esperado | Comando para generarlo |
|---|---|
| `latex/figures/k=100.png` + `videos/k=100.mp4` | `mvn -q exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="--experiment animate --k 100 --N <N> --tf 6 --fps 25 --width 480 --out latex/figures/anim_k1e2_N<N>"` + render con `scripts/render_animations.sh`. |
| `latex/figures/k=10000.png` + `videos/k=10000.mp4` | Idem con `--k 10000`. |

**Decidir** el `N` representativo para estas dos animaciones (sugerencia: `N = 500` para que muestre densidad media; el mismo `N` en ambos `k` para que la comparación sea fair). Actualizar el campo `N = ?` en las dos slides correspondientes (`ppt.tex` y `ppt_entregable.tex`) una vez decidido.

**Nota de naming**: las animaciones existentes usan `N=100.png` (literal `=` en el nombre). Para mantener consistencia se asume `k=100.png` y `k=10000.png`. Si se prefiere otro esquema (`anim_k1e2.png`, etc.), ajustar tanto el archivo como las referencias en los dos `.tex`.

---

## 3. Valores y observaciones a completar / revisar en las slides

### Slide "Pendiente $S_e$ del error de energía y $\Delta t$ elegido"

- El valor mostrado actualmente es `Δt = 2.0·10⁻³ s` para `k = 10³`. **Confirmar** este valor leyendo `figures/03f_s2_energy_dt_slope.png`: elegir el `Δt` más grande tal que `|S_e|` quede holgadamente por debajo del umbral (drift acumulado `< 1 %` a lo largo de `t_f = 500 s`, equivalente a `|S_e| ≲ 2·10⁻⁵ s⁻¹`).

### Slide "$\Delta t$ elegido por $k$" (tabla, sin gráfico)

- Tabla actual usa el criterio teórico `Δt ≈ τ_col / 100`. **Revisar** cada fila contra el análisis empírico de `|S_e|` para `k ∈ {10², 10³, 10⁴}`:
  - Para `k = 10²` se tiene `dt ∈ {3e-3, 5e-3, 1e-2, 5e-2}` en las corridas; ver cuál cumple drift `< 1 %`.
  - Idem para `k = 10³` (`dt ∈ {5e-4, 3e-3, 5e-3, 1e-2}`) y `k = 10⁴` (`dt ∈ {5e-4, 1e-3, 3e-3, 5e-3}`).
- Si los `Δt` empíricos difieren mucho de la regla `τ_col/100`, **reportar el valor empírico** y aclarar el criterio en la slide.

### Slide "Tiempo de ejecución vs. N — comparación con TP3"

- Confirmar que `04_s2_timing.png` incluye **la curva del TP3** (referencia explícita del corrector). Si solo tiene TP4, sumar la del TP3 desde `tp3_data/`.

### Slides de perfiles radiales (`07_s2_radial.png` y `08_s2_radial_near.png`)

- **Ventana de promediado: `t ∈ [0, t_f]`** (corrida completa). Es lo correcto para este TP — **no** se recorta el transitorio inicial. Cualquier slide o caption que diga `t ∈ [t_f/2, t_f]` o equivalente está mal y hay que corregirlo.
- **Acción**: regenerar los perfiles radiales con `--tmin 0` en el experimento `jvsn` (Main2 lo soporta; el default actual es `tf/2`, hay que pasarlo explícito).
- **En la slide**: dejar visible en `\framecite` la ventana efectiva (`t ∈ [0, t_f]`) y el número de snapshots promediados.
- Verificar que `08_s2_radial_near.png` ya combina `ρ`, `v` y `J_in` en **doble eje y** (no tres subplots separados), tal como pidió el corrector del TP3.

---

## 4. Pendientes fuera de Resultados (no tocados en esta iteración)

### Parte inicial (revisar / reestructurar)

- **Portada**: OK, sin cambios necesarios.
- **Sistema 1** (2 slides): revisar que las pendientes empíricas del ECM en log-log y la conclusión estén actualizadas a los últimos CSVs.
- **Sistema 2 -- Sistema particular**: el TikZ del recinto sigue siendo el mismo; revisar tamaños de fuente (los corrigieron en TP3, asegurarse de que `r_0`, `L` se lean cómodos).
- **Sistema 2 -- Motor de simulación**: hoy menciona Velocity-Verlet "sin dependencia en `v`". Como la fuerza elástica blanda **sí depende solo de posiciones**, está bien, pero revisar la línea final.
- **Sistema 2 -- Observables**: revisar que las definiciones de `C_fc`, `J`, `ρ_f^in`, `v_f^in`, `J_in` y `n_k^in` estén **todas** definidas antes de aparecer en cualquier figura (la slide actual las define todas).
- **Sistema 2 -- Parámetros de simulación**: alineá los `Δt` por `k` con los **valores definitivos** que decidamos en la slide "$\Delta t$ elegido por $k$" de Resultados.

### Conclusiones (revisar / reestructurar)

- Las dos conclusiones actuales hablan en términos de "satura" y "crece"; cambiar por **"valor promedio durante el período estacionario"** (corrección explícita del TP3).
- Confirmar que cada bullet corresponda a **una figura concreta** ya mostrada. Si una conclusión no tiene respaldo visual, sacarla o agregar la figura.
- Considerar separar Sistema 1 / Sistema 2 en dos slides si la conclusión queda densa.

---

## 5. Cómo regenerar todo el flujo de la sección Resultados

1. **Energy scan** (gráficos `03d` y `03f`):
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-25"
   $env:PYTHONIOENCODING = "utf-8"
   mvn -q compile
   mvn -q exec:java "-Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2" `
                    "-Dexec.args=--experiment energy_dt_scan"
   .\.venv\Scripts\python.exe plotter\plot_s2_energy_dt_scan.py
   # luego: copiar figures/03d_*.png y figures/03f_*.png → latex/figures/
   ```
2. **Timing / Cfc / J / radiales / k-sweep**: usar `bash scripts/run_all_s2.sh` y sus plotters respectivos.
3. **Animaciones**: ver §2.

---

## 6. Checklist final antes del 18/05/2026

- [ ] Las dos figuras del energy scan están en `latex/figures/` y no quedan placeholders rojos en las slides correspondientes.
- [ ] Las dos animaciones variando `k` están en `figures/` y `videos/` con sus `.mp4`.
- [ ] El `N` de las animaciones de `k` está definido en ambas slides (no queda `N = ?`).
- [ ] El `Δt` elegido para `k = 10³` está confirmado contra el plot de `|S_e|`.
- [ ] La tabla de `Δt` por `k` refleja los valores empíricamente validados.
- [ ] El plot de timing incluye la curva del TP3 sobreimpuesta.
- [ ] Parte inicial (Sistema 1 + introducción Sistema 2) revisada (ver §4).
- [ ] Conclusiones reescritas con respaldo de figura (ver §4).
- [ ] `ppt.pdf` (oral) y `ppt_entregable.pdf` compilan sin warnings de archivos faltantes.
