# Plan — TP4 SDS (Grupo 05)

## Context

Trabajo Práctico 4 de Simulación de Sistemas (ITBA, 72.25). Dos sistemas independientes en una única presentación de 13 min.

- **Sistema 1**: oscilador puntual amortiguado 1D. Comparar 4 integradores (Euler, Verlet original, Beeman PC, Gear PC orden 5) contra solución analítica vía ECM(Δt).
- **Sistema 2**: recinto circular con obstáculo central fijo. Dinámica molecular *time-driven* con fuerza elástica blanda. Items 1.1–1.4 (+1.5 opcional).

Entrega **18/05/2026 10:00**: presentación oral 13 min con animaciones embebidas, PDF de la presentación con frame + link, código zip < 100 KB (sólo motor de simulación). **No hay informe** explícito.

Estado del repo a 2026-05-06: vacío salvo LICENSE, README de una línea, `.idea/` (Java JDK 24), `.gitignore` y este `plan.md`. Salida esperada: simulación completa en Java + plotter en Python; sólo se entrega el código de simulación Java.

Fuentes primarias de la consigna y la teoría:
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/raw/enunciados/TP4_enunciado.pdf` (4 páginas).
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/raw/teoria/Teorica_4.pdf` (slide 36 = parámetros Sistema 1; slides 10/13/19–20/24–30 = integradores).
- Wiki resumida: `wiki/fuentes/tp4_enunciado.md`, `wiki/conceptos/{integradores,oscilador_amortiguado,dinamica_molecular_paso_temporal,fuerza_elastica_blanda,scanning_rate,perfiles_radiales}.md`, `wiki/metodos/{euler,verlet_original,beeman,gear_predictor_corrector}.md`.

## Consigna textual (extraída del enunciado y wiki)

**Sistema 1.** Sistema con solución analítica para evaluar integradores. Parámetros y CI: diapositiva 36 de la Teórica 4.
- 1.1 Integrar con Gear orden 5, Beeman, Verlet original, Euler.
- 1.2 Graficar analítica + numérica, calcular ECM.
- 1.3 ECM vs dt en log-log. ¿Cuál es mejor?
- En la presentación: sólo resultados, ~2 diapositivas, < 1 min, antes del Sistema 2. Nada de intro/integradores/animaciones/conclusiones.

**Sistema 2.** Mismo sistema físico que TP3, ahora con fuerza elástica blanda entre partículas: `F_ij = −k·ξ·ê_ij` si `ξ > 0`, `ξ = r_i + r_j − |x_i − x_j|`. k_ref = 10³ N/m.
- 1.1 Tiempo de ejecución vs N para t_f = 500 s. **Comparar con TP3.**
- 1.2 Cfc(t) → J. Sólo el primer dt del contacto cuenta. Guardar Cfc con resolución dt. **Comparar con TP3.**
- 1.3 Perfiles radiales (igual que TP3 1.4) con paleta gradual + colorbar. Detalle en S ∈ [1.5, 5] m. Promediar capas cercanas al obstáculo. **Comparar J^in con TP3.**
- 1.4 Variar k = {10², 10³, 10⁴, (10⁵)}. Validar dt en cada caso. Comparar ⟨J⟩(N) y ⟨J^in|S~2⟩(N). Identificar escalar característico vs k.
- 1.5 (opcional) Tiempo de la primera partícula usada en alcanzar el borde, en función de k y N.
- Para todos: **validar dt graficando energía total**.

## Decisiones zanjadas

| Tema | Decisión |
|---|---|
| Lenguaje simulación | **Java 24** + Maven. Sin dependencias externas. |
| Lenguaje plotter | Python (numpy + matplotlib + pandas). No es entregable. |
| Gear-5 α₀ con f dependiente de v | **3/16** (verificado contra Teórica 4 slide 29; el wiki tiene los rótulos invertidos en `metodos/gear_predictor_corrector.md`). |
| Realizaciones Sistema 2 | M = 10 por (N, k). |
| Integradores Sistema 2 | Beeman (default) + Verlet original (validación cruzada). |
| Verlet original con f(x,v) | `v` lageada: usar la `v` centrada del paso anterior; `v(0) = v₀` para el primer paso. |
| ECM Sistema 1 | Reportar `MSE = (1/N)·Σe²` y `RMSE = √MSE`. Plot principal usa **RMSE** (pendiente log-log = orden global del método). |
| Cell index method (CIM) | Lado de celda = 2·r_max = 2 m; para N ≤ 1000 baja O(N²) → O(N). |
| Δt₂ (sampling) para perfiles | 0.05 s (50× el dt típico). Cfc se guarda con resolución dt completa. |
| Reproducibilidad | Semilla por realización: `seed = baseSeed + 1000·N + realizationIdx`. |

## Estructura del proyecto

```
TP4_SDS/
├── pom.xml
├── README.md, LICENSE, CLAUDE.md, plan.md
├── .gitignore                             # agregar target/ results/ figures/ plotter/__pycache__/
├── src/main/java/ar/edu/itba/sds/
│   ├── sistema1/
│   │   ├── Oscillator.java                # parámetros + force(r,v) + analytical(t)
│   │   ├── Trajectory.java                # record { double[] t, r, v }
│   │   ├── Simulator.java                 # run(Integrator, dt, tf) -> Trajectory
│   │   ├── integrators/
│   │   │   ├── Integrator.java
│   │   │   ├── EulerIntegrator.java
│   │   │   ├── VerletIntegrator.java
│   │   │   ├── BeemanIntegrator.java
│   │   │   └── GearPC5Integrator.java
│   │   ├── analysis/ECMSweep.java
│   │   └── Main1.java
│   ├── sistema2/
│   │   ├── Vec2.java, Particle.java, Geometry.java
│   │   ├── ConfigSeeder.java              # rejection sampling
│   │   ├── CellIndexMethod.java           # vecinos O(N)
│   │   ├── ForceModel.java                # pares + obstáculo + borde
│   │   ├── EnergyTracker.java
│   │   ├── Simulator2D.java
│   │   ├── integrators/{Integrator2D, BeemanIntegrator2D, VerletIntegrator2D}.java
│   │   ├── observables/{CfcTracker, RadialProfileAccumulator, FirstBorderTracker}.java
│   │   ├── experiments/{TimingExperiment, JvsNExperiment, KSweepExperiment}.java
│   │   ├── io/CsvWriter.java
│   │   └── Main2.java                     # CLI con --experiment, --N, --k, etc.
│   └── common/{DeterministicRandom, Stopwatch}.java
├── plotter/                               # Python — NO entregable
│   ├── requirements.txt
│   ├── _style.py                          # rcParams: dpi=300, gradient cmap
│   ├── plot_s1_r_vs_t.py
│   ├── plot_s1_ecm_vs_dt.py
│   ├── plot_s2_energy.py                  # validación interna
│   ├── plot_s2_timing.py
│   ├── plot_s2_cfc_t.py
│   ├── plot_s2_j_vs_n.py
│   ├── plot_s2_radial.py
│   └── plot_s2_k_sweep.py
├── results/                               # gitignored; CSVs producidos por Java
├── figures/                               # gitignored; PNGs producidos por Python
├── tp3_data/                              # CSVs externos del TP3 para superponer (ver §dependencias)
└── scripts/run_*.sh                       # opcional: wrappers para barridos
```

## Sistema 1 — oscilador amortiguado

### Parámetros (constantes en `Oscillator.java`)
**Confirmar contra slide 36 de la Teórica 4 antes de codear.** Valores del CLAUDE.md del grupo: m = 70 kg, k = 10⁴ N/m, γ = 100 kg/s, A = 1 m, t_f = 5 s, r(0) = 1 m, v(0) = −A·γ/(2m) = −5/7 ≈ −0.7143 m/s.

Derivados: ω₀² = k/m ≈ 142.857; ω = √(k/m − γ²/(4m²)) ≈ 11.93 rad/s; T = 2π/ω ≈ 0.527 s; ζ = γ/(2√(km)) ≈ 0.0598 (subamortiguado).

### Solución analítica
Como v(0) = −A·ζ·ω₀ y r(0) = A, la fase φ = 0 y la amplitud es A. Por lo tanto:
```
r(t) = A · exp(−γ·t / (2m)) · cos(ω·t)
v(t) = −A · exp(−γ·t / (2m)) · [(γ/(2m))·cos(ω·t) + ω·sin(ω·t)]
```

### Interfaz `Integrator`
```java
public interface Integrator {
    String name();
    void step();
    double r();
    double v();
}
```
Constructor `(Oscillator sys, double r0, double v0, double dt)`. Cada implementación maneja su historia interna.

### Pseudocódigo de los 4 integradores

**Euler modificado** (Teórica slide 10):
```
a   = (−k·r − γ·v) / m
v  ← v + dt·a
r  ← r + dt·v             (usa v ya actualizada)
```

**Verlet original** (Teórica slide 13). Init: `rPrev = r₀ − dt·v₀ + dt²·a(0)/2`; `vLag = v₀`.
```
loop:
  a       = (−k·r − γ·vLag) / m
  rNext   = 2·r − rPrev + dt²·a
  vNew    = (rNext − rPrev) / (2·dt)
  rPrev, r, vLag = r, rNext, vNew
```
Nota: Verlet original asume f = f(x); para f(x, v) la `v` lageada degrada parcialmente el orden. Se acepta como variante didáctica; la Teórica lo incluye igual.

**Beeman PC** (Teórica slides 19–20). Init `aPrev` por Taylor backward a t = −dt: `r(−dt) = r₀ − dt·v₀ + dt²·a(0)/2`, `v(−dt) = v₀ − dt·a(0)`, `aPrev = (−k·r(−dt) − γ·v(−dt))/m`.
```
loop:
  rNext  = r + v·dt + (2/3)·a·dt² − (1/6)·aPrev·dt²
  vPred  = v + (3/2)·a·dt − (1/2)·aPrev·dt
  aNext  = (−k·rNext − γ·vPred) / m            # entra vPred (v-dep)
  vCorr  = v + (1/3)·aNext·dt + (5/6)·a·dt − (1/6)·aPrev·dt
  aPrev, a, v, r = a, aNext, vCorr, rNext
```

**Gear PC orden 5 con v-dep** (Teórica slides 24–30, slide 29 para los α). Estado en derivadas escaladas `R_k = r^(k)·dt^k / k!`, k = 0..5. Coeficientes **(3/16, 251/360, 1, 11/18, 1/6, 1/60)** — α₀ = 3/16 para `r₂ = f(r, r₁)` (caso v-dep). Nota: el wiki de Obsidian tiene los rótulos cruzados; la fuente autoritaria es slide 29.

Init analítico (de la EDO `r̈ = (−k·r − γ·v)/m`):
```
r₂₀ = (−k·r₀  − γ·v₀ ) / m
r₃₀ = (−k·v₀  − γ·r₂₀) / m
r₄₀ = (−k·r₂₀ − γ·r₃₀) / m
r₅₀ = (−k·r₃₀ − γ·r₄₀) / m
R0=r₀; R1=v₀·dt; R2=r₂₀·dt²/2; R3=r₃₀·dt³/6; R4=r₄₀·dt⁴/24; R5=r₅₀·dt⁵/120
```

Step:
```
# Predictor (Pascal)
R0p = R0+R1+R2+R3+R4+R5
R1p = R1+2R2+3R3+4R4+5R5
R2p = R2+3R3+6R4+10R5
R3p = R3+4R4+10R5
R4p = R4+5R5
R5p = R5

# Fuerza con r y v predichos (acá entra la v-dep)
rPred = R0p; vPred = R1p / dt
aReal = (−k·rPred − γ·vPred) / m
ΔR2   = aReal·dt²/2 − R2p

# Corrector
R0 = R0p + (3/16)    · ΔR2
R1 = R1p + (251/360) · ΔR2
R2 = R2p + 1         · ΔR2
R3 = R3p + (11/18)   · ΔR2
R4 = R4p + (1/6)     · ΔR2
R5 = R5p + (1/60)    · ΔR2
```
Salida: `r = R0`, `v = R1/dt`.

### Análisis (`ECMSweep`)
- dt ∈ {10⁻², 10⁻³, 10⁻⁴, 10⁻⁵, 10⁻⁶}. Para cada (integrador, dt), correr `Simulator.run` y comparar con `Oscillator.analytical(t)`.
- Producir `results/s1/ecm_sweep.csv` (`integrator, dt, mse, rmse, n_steps`).
- Producir `results/s1/trajectory_<integrator>_dt1e-3.csv` (`t, r_num, r_ana`) sólo para el plot de superposición.

### Validación interna
1. A dt = 10⁻³ las 4 trayectorias se superponen visualmente con la analítica. Si Euler diverge a dt = 10⁻³, hay bug (ω·dt ≈ 0.012, muy estable).
2. Pendiente log-log de **RMSE** vs dt: Euler ≈ 1, Verlet ≈ 2, Beeman ≈ 2, Gear-5 ≈ 4–5. Equivalente para MSE: el doble.
3. Gear-5 < resto en RMSE en todo el rango, salvo posible piso de redondeo a dt = 10⁻⁶.

### Slides Sistema 1 (~2 slides, < 1 min)
- (a) `r(t)` numérico (4 colores) + analítica con dt = 10⁻³.
- (b) log-log RMSE vs dt; 4 curvas con pendientes anotadas.

## Sistema 2 — recinto circular con obstáculo

### Parámetros (constantes en `Geometry.java`)
- L = 80 m **diámetro** ⇒ R = 40 m. r₀ = 1 m. r_partícula = 1 m. m = 1 kg. |v₀| = 1 m/s, dirección uniforme en [0, 2π).
- N ∈ {10, 20, 50, 100, 200, 400, 800, 1000}. La consigna dice N ∈ [100, 1000]; extendemos hacia abajo {10, 20, 50} para mostrar el comportamiento de J(N) en el régimen de N pequeño. Hay que **aclarar la extensión en el slide** ("rango ampliado vs. consigna para visualizar el régimen de N bajo").
- k_ref = 10³ N/m. Para 1.4: k ∈ {10², 10³, 10⁴} obligatorio, **10⁵ condicional** a tiempo de máquina.
- t_f = 500 s. M = 10 realizaciones por (N, k).

### Estado inicial (`ConfigSeeder`)
Rejection sampling: ubicar partícula i de a una en el disco abierto de radio R − r_partícula evitando solape (>2r) con obstáculo y con previas. Velocidad: módulo 1, ángulo uniforme.

### Fuerzas (`ForceModel`) — F = −k·ξ·ê con ξ > 0
- Pares: ξ = 2r − |x_i − x_j|, ê = (x_i − x_j)/|x_i − x_j|. Tercera ley: calcular una vez por par.
- Obstáculo (masa infinita en origen): ξ = (r + r₀) − |x_i|, ê = x_i / |x_i|.
- Borde (masa infinita): ξ = |x_i| − (R − r). Sólo si ξ > 0. Dirección: −x_i / |x_i| (empuja hacia adentro).

### Cell Index Method (`CellIndexMethod`)
Cuadrícula sobre el bounding-box del recinto (lado L = 80 m). Lado de celda = 2·r = 2 m → 40×40 celdas. Para cada partícula, sólo se inspeccionan las 9 celdas vecinas. Lleva el cálculo de fuerzas pares de O(N²) a O(N). Reconstruir las celdas en cada paso (caro pero simple) o cada k pasos si es necesario optimizar después.

### Validación de dt (energía)
- τ_col = π·√(m/k); regla práctica del wiki: dt ≲ T/100 = 2π·√(m/k)/100.
  - k = 10²: dt ≲ 6.3·10⁻³ → uso 5·10⁻³.
  - k = 10³: dt ≲ 2.0·10⁻³ → uso 1·10⁻³.
  - k = 10⁴: dt ≲ 6.3·10⁻⁴ → uso 5·10⁻⁴.
  - k = 10⁵: dt ≲ 2.0·10⁻⁴ → uso 1·10⁻⁴.
- `EnergyTracker` exporta `t, E_kin, E_pot, E_total` por realización. E_pot = Σ_pares ½·k·ξ² (pares en contacto + contactos con obstáculo + contactos con borde, todos con la misma forma).
- Validación gating: si la deriva relativa de E_total en una corrida de prueba (N = 100, k = 10³, t_f = 5 s) supera 1%, bajar dt antes de lanzar barridos.

### Integradores 2D (`BeemanIntegrator2D`, `VerletIntegrator2D`)
- Default: Beeman 2D (componente a componente, `aPrev` por partícula).
- Validación cruzada: Verlet original 2D en N = 200, k = 10³, M = 3 — confirmar que ⟨J⟩ es compatible (sirve internamente, no necesariamente para el slide).
- Beeman es preferible cuando dt se acerca a τ_col (k = 10⁵).

### Item 1.1 — Tiempo de ejecución vs N
- N ∈ {50, 100, 200, 400, 800, 1000}.
- t_f reducido para escalado puro (50 s; el escalado no necesita 500 s).
- Una sola realización por N (lo que se mide es escalado, no media).
- CSV: `results/s2/timing.csv` (N, t_exec_s).
- Plot **log-log** y comparación con datos de TP3 (ver §Dependencias externas).

### Item 1.2 — Cfc(t) y J(N)
- N ∈ {10, 20, 50, 100, 200, 400, 800}, M = 10.
- `CfcTracker` por partícula:
  ```
  if ξ_obs > 0:
      if not inContactObs:                   # primer dt del contacto
          inContactObs = true
          if state == FRESH:
              state = USED
              cfc[step] += 1
  else:
      inContactObs = false
  ```
  Lógica simétrica para borde: state ← FRESH al primer dt del contacto con borde.
- Cfc(t) se persiste con resolución dt completa (no dt₂).
- J(N) = pendiente del régimen estacionario por mínimos cuadrados sobre `t ∈ [t_f/2, t_f]`. Ventana visible en el slide.
- CSVs: `results/s2/cfc_N{N}_real{r}.csv`, `results/s2/j_vs_n.csv` (`N, J_mean, J_std`).

### Item 1.3 — Perfiles radiales
- Sobre las mismas corridas de 1.2 (no re-correr).
- `RadialProfileAccumulator`: cada dt₂ = 0.05 s, para cada partícula con `state == FRESH` y `x·v < 0`:
  - bin S = floor(|x| / 0.2) · 0.2 + 0.1 (centro del bin); rango S ∈ [r₀ + r, R − r].
  - sumar contador `n[S]` y `sum_v_radial[S]` con `v_radial = (x·v)/|x|` (negativo).
- Al final, sobre todos los snapshots y realizaciones:
  - ⟨ρ_f^in⟩(S) = (n[S] / N_snapshots) / (2π·S·dS).
  - ⟨v_f^in⟩(S) = −sum_v_radial[S] / n[S]   (si n[S] = 0 → NaN, omitir en plots).
  - J^in(S) = ⟨ρ_f^in⟩ · |⟨v_f^in⟩|.
- CSV: `results/s2/radial_N{N}.csv` (`S, rho, v_in, j_in, n_samples`).
- Plot principal: J^in(S) para todos los N en una figura, **paleta gradual + colorbar**, doble panel:
  - rango completo S ∈ [r₀ + r, R − r];
  - zoom S ∈ [1.5, 5] m.
- Plot complementario: promediar capas S ∈ [1.5, 3] m → ⟨J^in⟩_cerca, ⟨ρ⟩_cerca, ⟨v⟩_cerca; graficar **vs N** en una sola figura con doble eje y. Superponer J(N) del 1.2 y de TP3.

### Item 1.4 — Variación de k
- k ∈ {10², 10³, 10⁴} obligatorios; 10⁵ si la máquina banca. Validar dt para cada k (ver tabla).
- Para cada (k, N) ∈ {10², 10³, 10⁴} × {10, 20, 50, 100, 200, 400, 800}: M = 10 realizaciones.
- Output: `results/s2/k_sweep.csv` (`k, N, J_mean, J_std, J_in_S2_mean, J_in_S2_std`).
- Plot: 2 paneles (uno por observable). Eje x = N, una curva por k con colorbar.
- Escalar característico vs k: `max_N⟨J⟩(N, k)` y `argmax_N⟨J⟩(N, k) =: N*(k)`. Plot de cada uno vs k en log-log.

### Item 1.5 (opcional) — primer borde
Registrar `t_borde` = primer instante en que cualquier partícula USED toca el borde, en función de (N, k). `FirstBorderTracker` lo registra una sola vez por realización. Plot t_borde(N, k) si hay tiempo.

### Slides Sistema 2 (≈10 slides)
1 esquema/parámetros + 1 implementación (sólo motor) + 1 simulaciones (parámetros físicos, M, ventana de promediado) + 1 animaciones + 1 series temporales (E(t) y Cfc(t)) + 1 timing 1.1 + 1 J(N) 1.2 + 1 perfiles 1.3 (panel completo + zoom) + 1 J cerca + ⟨ρ⟩ + ⟨v⟩ vs N (1.3) + 1 J(N, k) y J^in(N, k) (1.4) + 1 escalar característico vs k (1.4) + 1 conclusiones.

## Plotter (Python)

`plotter/_style.py`:
```python
import matplotlib as mpl
mpl.rcParams.update({
    "figure.dpi": 300, "savefig.dpi": 300,
    "font.size": 12, "axes.titlesize": 13, "legend.fontsize": 10,
})
GRADIENT_CMAP = "viridis"
```

Cada `plot_s*.py` se ejecuta standalone (`python plotter/plot_s1_ecm_vs_dt.py`), lee CSVs de `results/`, escribe PNGs a `figures/`. `requirements.txt`: numpy, pandas, matplotlib.

## Build y ejecución

`pom.xml` minimal (Java 24, sin deps externas, plugin `exec-maven-plugin`).

```
mvn compile
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema1.Main1
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="--experiment timing"
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="--experiment jvsn  --N 100 --realizations 10"
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 -Dexec.args="--experiment ksweep --tf 500 --realizations 10"
```

`scripts/run_all_s2.sh`: corre 1.1 → 1.2/1.3 → 1.4 secuencial; volcando logs a `results/log/`.

## Guía de presentación (principios generales, Beamer)

Sólo lo universal y aplicable a TP4. No es lista de errores pasados.

- Tema **Warsaw + miniframes** (`\useoutertheme[subsection=false,footline=empty]{miniframes}`); numeración de slides activada; sin diapositiva-índice (presentación de 13 min).
- **Sin redacción**: keywords. "Inputs" ≠ "Los inputs utilizados fueron:".
- **Parámetros nunca en el título**. Van al costado o como texto del frame.
- **Notación**: θ(t+Δt), no θ(t+1). atan2, no atan. 10⁵, no 1E5. Vectores en negrita sin itálica; escalares en itálica; unidades rectas.
- **Figuras**: título dentro del frame; ejes con unidades; barras de error visibles; aclarar que son sobre realizaciones (no sobre tiempo).
- **Escalas**: log-log para análisis de escalado (lineal en log-log ⇒ ley de potencia, no exponencial). Etiquetar pendientes ajustadas en la figura.
- **Color**: variables numéricas continuas (N, k) → paleta gradual + **colorbar**, no leyenda categórica con sombreado.
- **Combinar plots** cuando sea posible (dos observables relacionados → doble eje y, no dos figuras separadas).
- **Animaciones**: máximo 2 por sistema; embebidas en la presentación oral; en el PDF entregable, frame + link (revisar que el link no tenga caracteres pegados al copiar). El paquete `movie15` + visor `pympress` autoplay.
- **Citas**: en la esquina del frame específico donde se usa el resultado, **no** en una diapositiva final de bibliografía.
- **Dos PDFs**: `ppt.pdf` (oral, animaciones embebidas) y `ppt_entregable.pdf` (frame + link).
- **Defensa oral**: practicar con cronómetro antes; el orden recomendado por la guía de la cátedra es animación → serie temporal → input vs observable.
- **Lenguaje**: "valor promedio durante el período estacionario", no "valor asintótico". No mencionar "estado estacionario" antes de mostrar las figuras que lo evidencian.

## Plan de trabajo

1. **Sistema 1 entero**. ~1 día. Cierra los 2 slides; valida la arquitectura `integrators/`.
2. **Sistema 2 esqueleto**: `Vec2`, `Particle`, `Geometry`, `ConfigSeeder`, `ForceModel`, `BeemanIntegrator2D`, `Simulator2D`, `EnergyTracker`. Sin CIM todavía.
3. **Animación rápida** y plot de E(t) para k = 10³ → confirmar dt.
4. **CIM** y benchmark: confirmar que N = 1000 corre razonable.
5. **Item 1.1** (timing) — barato.
6. **Item 1.2 + 1.3** sobre las mismas corridas (M = 10).
7. **Item 1.4** con k ∈ {10², 10³, 10⁴}; 10⁵ sólo si hay tiempo.
8. **Verlet 2D** y validación cruzada en N = 200, k = 10³, M = 3.
9. **Plotter** y **slides** en paralelo desde el paso 1.
10. **Item 1.5** opcional al final.

## Dependencias externas no controladas por este plan

1. **Datos del TP3** para superponer en plots de 1.1, 1.2 y 1.3. La consigna lo exige. Necesitamos del repo de TP3 del grupo:
   - `tp3_data/timing.csv` (N, t_exec).
   - `tp3_data/j_vs_n.csv` (N, J_mean, J_std).
   - `tp3_data/j_in_S2.csv` o equivalente.
   Si no se exportan a tiempo, los plots se entregan sin la curva del TP3 y se aclara en el slide.
2. **Slide 36 de la Teórica 4** para confirmar exactamente los parámetros del oscilador. Antes de codear `Oscillator.java`, abrir el PDF y verificar.

## Java vs C++ (sidebar; queda Java)

| | Java 24 | C++ |
|---|---|---|
| Velocidad de desarrollo | + | − |
| Performance numérica raw | razonable (HotSpot) | máxima (-O3, SIMD, OMP) |
| Cuello previsto | 1.4 con k = 10⁵ | C++ ganaría 2–3× ahí |
| Tooling | Maven + IntelliJ ya instalados | CMake + cuidado con UB |
| Riesgo a 12 días | bajo | alto |
| Reproducibilidad inter-OS | trivial | depende del compilador |

**Veredicto:** Java. Si k = 10⁵ resulta inviable, portar **sólo** `ForceModel` + integrador 2D vía JNI o ejecutable subproceso. No vale la pena planificarlo de entrada.

## Verificación end-to-end

1. Sistema 1: `mvn compile && mvn exec:java -Dexec.mainClass=...sistema1.Main1`. Inspeccionar `results/s1/`. Correr `python plotter/plot_s1_*.py`. Verificar (a) las 4 trayectorias se superponen a dt = 10⁻³, (b) pendientes 1, 2, 2, 4–5 en log-log de RMSE.
2. Sistema 2 sanity: `--experiment energy --N 100 --k 1000 --tf 5`. Plot de E(t) constante salvo fluctuación. Si deriva > 1%, bajar dt.
3. Timing: `--experiment timing`. Plot log-log con pendiente esperada (lineal en log-log si CIM funciona).
4. J(N): `--experiment jvsn`. Cfc(t) lineal en régimen estacionario; ⟨J⟩(N) reproduce orden y forma de la curva del TP3.
5. Perfiles: J^in(S) decrece con S y tiene un pico cerca del obstáculo.
6. k-sweep: ⟨J⟩(N, k) creciente con k (rebote más rígido), tendiendo al límite EDMD del TP3 cuando k → ∞.
7. Slides: revisar §Guía de presentación antes de exportar PDF.
8. Zip entregable: `zip -r entregable.zip src pom.xml` (sin `target/`, sin `plotter/`, sin `results/`, sin `figures/`). Verificar < 100 KB.

## Critical files (a crear)

- `pom.xml`
- `src/main/java/ar/edu/itba/sds/sistema1/{Oscillator, Trajectory, Simulator, Main1}.java`
- `src/main/java/ar/edu/itba/sds/sistema1/integrators/{Integrator, EulerIntegrator, VerletIntegrator, BeemanIntegrator, GearPC5Integrator}.java`
- `src/main/java/ar/edu/itba/sds/sistema1/analysis/ECMSweep.java`
- `src/main/java/ar/edu/itba/sds/sistema2/{Vec2, Particle, Geometry, ConfigSeeder, CellIndexMethod, ForceModel, EnergyTracker, Simulator2D, Main2}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/integrators/{Integrator2D, BeemanIntegrator2D, VerletIntegrator2D}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/observables/{CfcTracker, RadialProfileAccumulator, FirstBorderTracker}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/experiments/{TimingExperiment, JvsNExperiment, KSweepExperiment}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/io/CsvWriter.java`
- `src/main/java/ar/edu/itba/sds/common/{DeterministicRandom, Stopwatch}.java`
- `plotter/{requirements.txt, _style.py, plot_s1_r_vs_t.py, plot_s1_ecm_vs_dt.py, plot_s2_energy.py, plot_s2_timing.py, plot_s2_cfc_t.py, plot_s2_j_vs_n.py, plot_s2_radial.py, plot_s2_k_sweep.py}`
- Actualizar `.gitignore`: `target/`, `results/`, `figures/`, `plotter/__pycache__/`, `.DS_Store`.
- `CLAUDE.md` §2.3 ya tenía α₀ = 3/16 para v-dep — confirmado. Sin cambios necesarios. (El wiki de Obsidian, en cambio, tiene los rótulos invertidos y debería corregirse fuera de este repo.)

## Open questions (no bloqueantes)

1. **Confirmar slide 36 Teórica 4** antes de codear Oscillator.
2. **CIM con dt₂ adaptativo** para corridas largas: si los CSVs de Cfc explotan en tamaño, comprimir o cambiar a snapshots.
3. **Realización fallida** (energía diverge mid-run): retentar con seed siguiente y registrar.
4. **k = 10⁵**: si una realización tarda > 4 h, se cae del barrido y se anota "(N/A)" en plots.
