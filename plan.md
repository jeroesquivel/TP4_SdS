# Plan — TP4 SDS (Grupo 05): implementación completa

## Context

Trabajo Práctico 4 de Simulación de Sistemas (ITBA, 72.25). Dos sistemas independientes en una única presentación de 13 min, entrega **18/05/2026 10:00**:

- **Sistema 1**: oscilador amortiguado 1D, comparar 4 integradores contra solución analítica vía ECM(Δt). ~2 slides, < 1 min.
- **Sistema 2**: recinto circular con obstáculo central, dinámica molecular *time-driven* con fuerza elástica blanda. Reproduce el sistema físico del TP3 con esquema MD por paso temporal. Items 1.1–1.4 (+1.5 opcional).

Por qué este plan: el TP4 está bien acotado pero el grupo perdió puntos en TP2 (6/10) y TP3 (4.5/10) por presentación, escalado mal interpretado (log-log vs semi-log) y faltantes de comparación con TP3. La codebase del proyecto está vacía hoy. Salida esperada: simulación completa en Java + plotter en Python; sólo se entrega el código de simulación Java (zip < 100 KB).

Fuentes consultadas y críticas para releer al implementar:
- `~/Desktop/ITBA/26-1C/SDS/TP4_SDS/CLAUDE.md` — directrices del grupo, errores a no repetir.
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/wiki/fuentes/tp4_enunciado.md` — consigna oficial.
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/wiki/metodos/{euler,verlet_original,beeman,gear_predictor_corrector}.md` — fórmulas.
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/raw/correcciones/TP3_correccion.txt` y `TP2_correccion.txt` — feedback histórico.
- `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/raw/teoria/Teorica_4.pdf` slide 36 — parámetros de Sistema 1 (no leídos directamente; el CLAUDE.md ya los volcó).

## Decisiones zanjadas durante planificación

| Tema | Decisión | Origen |
|---|---|---|
| Lenguaje simulación | Java | usuario, 2026-05-06 |
| Lenguaje plotter | Python (matplotlib + numpy) | usuario |
| Build | Maven; Java 24 (consistente con `.idea/misc.xml`) | inferido del repo |
| Gear-5 α₀ con v-dep | **3/20** (wiki cátedra) — actualizar CLAUDE.md §2.3 | usuario, 2026-05-06 |
| Realizaciones Sistema 2 | **M = 10** (igual a TP3) | usuario |
| Integradores Sistema 2 | **Beeman + Verlet original** (doble validación) | usuario |
| ECM Sistema 1 | Reportar **dos métricas**: `MSE = (1/N)Σe²` (como en CLAUDE.md) y `RMSE = √MSE`. Las pendientes en log-log de RMSE vs Δt deberían dar 1, 2, 2, 4–5; las de MSE el doble. Plot principal usa RMSE para que las pendientes coincidan con la "orden aparente" del CLAUDE.md. | mío, flaggeado |
| Verlet con v-dep | `v` lageada (centrada del paso anterior); `v(0) = v₀` para el primer paso | mío |

## Estructura del proyecto (Maven, módulo único)

```
TP4_SDS/
├── pom.xml                                # Java 24, sin dependencias externas
├── README.md, LICENSE, CLAUDE.md          # ya existen
├── .gitignore                             # actualizar: agregar target/, results/, figures/
├── src/main/java/ar/edu/itba/sds/
│   ├── sistema1/
│   │   ├── Oscillator.java                # parámetros + force(r,v) + analytical(t)
│   │   ├── Trajectory.java                # record { double[] t, r, v }
│   │   ├── Simulator.java                 # run(Integrator, dt, tf) -> Trajectory
│   │   ├── integrators/
│   │   │   ├── Integrator.java            # interface { name, step, r, v }
│   │   │   ├── EulerIntegrator.java
│   │   │   ├── VerletIntegrator.java
│   │   │   ├── BeemanIntegrator.java
│   │   │   └── GearPC5Integrator.java     # α₀ = 3/20, parametrizable
│   │   ├── analysis/
│   │   │   └── ECMSweep.java              # 4 integradores × 5 Δt → CSV
│   │   └── Main1.java                     # entry point Sistema 1
│   ├── sistema2/
│   │   ├── Vec2.java                      # record (x, y) + ops básicas
│   │   ├── Particle.java                  # id, pos, vel, accel, accelPrev, state, inContactObs
│   │   ├── Geometry.java                  # R=40, r₀=1, r=1, helpers (dentroRecinto, distanciaCentro)
│   │   ├── ConfigSeeder.java              # init posiciones sin solapamiento (rejection sampling)
│   │   ├── CellList.java                  # linked-cell para fuerzas O(N) en lugar de O(N²)
│   │   ├── ForceModel.java                # F_ij = -k·ξ·ê (pares + obstáculo + borde)
│   │   ├── EnergyTracker.java             # E_kin + E_pot por contacto
│   │   ├── Simulator2D.java               # acepta Integrator2D, corre tf
│   │   ├── integrators/
│   │   │   ├── Integrator2D.java          # interface { name, step }
│   │   │   ├── BeemanIntegrator2D.java    # default
│   │   │   └── VerletIntegrator2D.java    # validación cruzada
│   │   ├── observables/
│   │   │   ├── CfcTracker.java            # state machine fresca↔usada, primer dt del contacto
│   │   │   ├── RadialProfile.java         # ⟨ρ_f^in⟩(S), ⟨v_f^in⟩(S), J^in(S), dS=0.2
│   │   │   └── ContactCounter.java        # opcional, para 1.5
│   │   ├── experiments/
│   │   │   ├── TimingExperiment.java      # 1.1: t_exec vs N
│   │   │   ├── JvsNExperiment.java        # 1.2 + 1.3 sobre las mismas corridas
│   │   │   └── KSweepExperiment.java      # 1.4: barrido k
│   │   ├── io/CsvWriter.java
│   │   └── Main2.java                     # entry point Sistema 2 (acepta args: experiment, N, k, ...)
│   └── common/
│       ├── DeterministicRandom.java       # wrapper de java.util.Random con semilla explícita
│       └── Stopwatch.java
├── plotter/                               # Python — NO se entrega (§7.7 CLAUDE.md)
│   ├── requirements.txt                   # numpy, pandas, matplotlib
│   ├── _style.py                          # rcParams: dpi=300, fonts, gradient cmap
│   ├── plot_s1_r_vs_t.py                  # → figures/s1_r_vs_t.png
│   ├── plot_s1_ecm_vs_dt.py               # → figures/s1_ecm_vs_dt.png
│   ├── plot_s2_energy.py                  # validación de Δt (no slide, interno)
│   ├── plot_s2_timing_vs_n.py             # 1.1
│   ├── plot_s2_cfc_t.py                   # 1.2 panel A
│   ├── plot_s2_j_vs_n.py                  # 1.2 panel B (con TP3 superpuesto)
│   ├── plot_s2_radial_profiles.py         # 1.3 (ρ, v, J^in con colorbar; doble eje y)
│   └── plot_s2_k_sweep.py                 # 1.4
├── results/                               # gitignored; CSVs producidos por Java
├── figures/                               # gitignored; PNGs producidos por Python
├── tp3_baselines/                         # CSVs exportados del TP3 para superponer en plots
└── scripts/                               # opcional: bash para correr barridos
```

Cuando llegue Sistema 2 al `1.4`, los barridos largos los dispara un wrapper (`scripts/run_k_sweep.sh`) que invoca `Main2` con args. Sistema 1 corre todo de un solo `mvn exec:java` por defecto.

## Sistema 1 — oscilador amortiguado

### Parámetros (constantes en `Oscillator.java`)
m=70 kg, k=10⁴ N/m, γ=100 kg/s, A=1 m, t_f=5 s, r(0)=1, v(0) = -A·γ/(2m) ≈ -0.7143 m/s.
ω₀² = k/m ≈ 142.857; ω = √(k/m - γ²/(4m²)) ≈ 11.93 rad/s; T ≈ 0.527 s.
Solución: `r(t) = A·exp(-γt/(2m))·cos(ωt)`.

### Interfaz `Integrator`
```java
public interface Integrator {
    String name();
    void step();          // muta estado interno, avanza Δt
    double r();
    double v();
}
```
Cada implementación: constructor `(Oscillator sys, double r0, double v0, double dt)` que setea estado interno (incluyendo historia: `rPrev` para Verlet, `aPrev` para Beeman, R₀..R₅ para Gear).

### Integradores — pseudocódigo (la física, no el Java)

**Euler modificado.**
```
a   = (-k·r - γ·v) / m
v  ← v + Δt·a
r  ← r + Δt·v          (usa v ya actualizada)
```

**Verlet original.** Init: `rPrev = r0 - Δt·v0 + Δt²·a(0)/2`; `vLag = v0`.
```
a       = (-k·r - γ·vLag) / m
rNext   = 2r - rPrev + Δt²·a
vNew    = (rNext - rPrev) / (2Δt)
rPrev, r, vLag = r, rNext, vNew
```

**Beeman PC.** Init: aPrev de Taylor backward: `r(-Δt) = r0 - Δt·v0 + Δt²·a(0)/2`, `v(-Δt) = v0 - Δt·a(0)`, `aPrev = (-k·r(-Δt) - γ·v(-Δt))/m`.
```
rNext  = r + v·Δt + (2/3)·a·Δt² - (1/6)·aPrev·Δt²
vPred  = v + (3/2)·a·Δt - (1/2)·aPrev·Δt
aNext  = (-k·rNext - γ·vPred) / m
vCorr  = v + (1/3)·aNext·Δt + (5/6)·a·Δt - (1/6)·aPrev·Δt
aPrev, a, v, r = a, aNext, vCorr, rNext
```

**Gear PC orden 5 con v-dep.** Estado en derivadas escaladas `R_k = r^(k)·Δt^k / k!`, k = 0..5.
Coeficientes **(3/20, 251/360, 1, 11/18, 1/6, 1/60)** — α₀ = 3/20 zanjado.

Init analítico (de la EDO `r̈ = (-k·r - γ·v)/m`):
```
r₂₀ = (-k·r0 - γ·v0) / m
r₃₀ = (-k·v0  - γ·r₂₀) / m
r₄₀ = (-k·r₂₀ - γ·r₃₀) / m
r₅₀ = (-k·r₃₀ - γ·r₄₀) / m
R0 = r0;  R1 = v0·Δt;  R2 = r₂₀·Δt²/2;  R3 = r₃₀·Δt³/6;  R4 = r₄₀·Δt⁴/24;  R5 = r₅₀·Δt⁵/120
```

Step:
```
# Predictor (triángulo de Pascal)
R0p = R0+R1+R2+R3+R4+R5
R1p = R1+2R2+3R3+4R4+5R5
R2p = R2+3R3+6R4+10R5
R3p = R3+4R4+10R5
R4p = R4+5R5
R5p = R5

# Fuerza con r predicho y v predicha (acá entra la v-dep)
rPred = R0p; vPred = R1p / Δt
aReal = (-k·rPred - γ·vPred) / m
ΔR2   = aReal·Δt²/2 - R2p

# Corrector
R0 = R0p + (3/20)    · ΔR2
R1 = R1p + (251/360) · ΔR2
R2 = R2p + 1         · ΔR2
R3 = R3p + (11/18)   · ΔR2
R4 = R4p + (1/6)     · ΔR2
R5 = R5p + (1/60)    · ΔR2
```
Salida: `r = R0`, `v = R1/Δt`.

### Análisis (`ECMSweep`)
- Para `dt ∈ {1e-2, 1e-3, 1e-4, 1e-5, 1e-6}` y los 4 integradores → corre `Simulator.run()`, calcula `MSE` y `RMSE` contra `Oscillator.analytical(t)`.
- CSV: `results/s1/ecm_sweep.csv` (`integrator, dt, mse, rmse, n_steps`).
- Trayectorias para el gráfico de superposición sólo a `dt = 1e-3`: `results/s1/trajectory_<integrator>_dt1e-3.csv` (`t, r_num, r_ana`).

### Validación interna antes de slide
1. A `dt=1e-3`, las 4 trayectorias deben superponerse visualmente con la analítica. Si Euler diverge a `dt=1e-3`, hay bug.
2. Pendientes log-log de RMSE: Euler ≈ 1, Verlet ≈ 2, Beeman ≈ 2, Gear-5 ≈ 4–5.
3. Gear-5 < resto en RMSE en todo el rango (excepto floor numérico a `dt=1e-6`).

### Slides (~2 slides)
1. `r(t)` numérico (4 colores) + analítica para `dt = 1e-3`. Sin parámetros en el título; van en la esquina o margen.
2. log-log RMSE vs Δt; 4 curvas con pendientes anotadas. Conclusión en keywords ("Gear-5: menor error y mayor pendiente").

## Sistema 2 — recinto circular con obstáculo

### Parámetros y geometría (constantes en `Geometry.java`)
- L = 80 m **diámetro** → R = 40 m. r = 1 m, r₀ = 1 m, m = 1 kg, |v₀| = 1 m/s, direcciones uniformes en [0, 2π).
- N ∈ {10, 20, 50, 100, 200, 400, 800, 1000} (mismos puntos que TP3 + 1000 para extender el rango).
- k_ref = 10³ N/m. Para 1.4: k ∈ {10², 10³, 10⁴, 10⁵}; **(10⁵) condicional** a tiempo de máquina.
- t_f = 500 s (mismo que TP3 para comparar).
- M = 10 realizaciones por (N, k).

### Estado inicial (`ConfigSeeder`)
Rejection sampling: ubicar partícula i de a una en el círculo R=40 evitando solape con obstáculo (centro) y con todas las anteriores (>2r). Velocidad: módulo 1, ángulo uniforme. Semilla por realización: `seed = baseSeed + 1000·N + realizationIdx`. Reproducible.

### Fuerzas (`ForceModel`) — F = -k·ξ·ê con ξ > 0
- Pares partícula-partícula: `ξ = 2r - |x_i - x_j|`, `ê = (x_i - x_j)/|x_i - x_j|`.
- Obstáculo (masa infinita en origen): `ξ = r + r₀ - |x_i|`, `ê = x_i / |x_i|`.
- Borde recinto (masa infinita): `ξ = r - (R - |x_i|)` cuando `|x_i| > R - r`, dirección hacia adentro: `ê = -x_i / |x_i|`.
- Cell list (`CellList`): celda lateral 2r = 2 m, sólo busco vecinos en celdas adyacentes. Reduce O(N²) → O(N) para N ≤ 1000.

### Validación de Δt (energía)
- τ_col ≈ π·√(m/k) (medio período del oscilador de contacto).
- Heurística cátedra: Δt ≲ T/100 = 2π·√(m/k)/100. Implementadas guardas en `Main2`:
  - k=10²: Δt ≤ ~6e-3 → uso 5e-3
  - k=10³: Δt ≤ ~2e-3 → uso 1e-3
  - k=10⁴: Δt ≤ ~6e-4 → uso 5e-4
  - k=10⁵: Δt ≤ ~2e-4 → uso 1e-4
- Δt₂ (sampling para CSVs grandes) ≥ Δt; Cfc se guarda con resolución Δt completa, perfiles radiales con Δt₂ = 0.05 s.
- `EnergyTracker` exporta `t, E_kin, E_pot, E_total` por realización; el plot de energía es validación interna (no va a slide salvo que haya algo raro). E_total debe ser estable salvo fluctuación esperada.

### Integración (`BeemanIntegrator2D`, `VerletIntegrator2D`)
- Default Beeman; usar Verlet sólo para corridas de validación cruzada en N=200 con M=3 (no es para el slide principal — es para confirmar internamente que ambos integradores dan J(N) compatible).
- Beeman 2D = Beeman escalar aplicado componente a componente, con `aPrev` por partícula.
- Verlet 2D igual con `rPrev` por partícula. Para 1.4 con k=10⁵ usar Beeman (más estable con dt grande).

### Item 1.1 — Tiempo de ejecución vs N
- N ∈ {50, 100, 200, 400, 800, 1000} (suficiente para escalado).
- Una sola realización por N (corridas largas son ruidosas pero el escalado se ve igual). t_f = 50 s para escalado (no hace falta 500 s para medir tiempo).
- CSV: `results/s2/timing.csv` (N, t_exec_s, integrator).
- Plot **log-log**, superpuesto con TP3 desde `tp3_baselines/timing.csv`. NUNCA semi-log para escalado (corrección D22 del TP3).

### Item 1.2 — Cfc(t) y J(N)
- N ∈ {10, 20, 50, 100, 200, 400, 800}. M = 10 realizaciones.
- `CfcTracker` por partícula: `state ∈ {FRESH, USED}`, flag `inContactObs`. Lógica:
  ```
  if ξ_obs > 0:
      if not inContactObs:                 # primer Δt del contacto
          inContactObs = true
          if state == FRESH: state = USED; cfc_t[step] += 1
  else:
      inContactObs = false
  ```
  Reverso en borde: `state = FRESH` al **primer Δt** del contacto con borde.
- Cfc se guarda con resolución Δt completa.
- J(N) = pendiente del régimen estacionario (lineal en t) por mínimos cuadrados, ventana `t ∈ [500/2, 500]` (no promediar antes de t=500/2; corrección TP3 implícita). Ventana visible en el slide.
- Promedio sobre realizaciones: ⟨J⟩(N) ± std/√M.
- CSVs: `results/s2/cfc_N{N}_real{r}.csv`, `results/s2/j_vs_n.csv` (N, J_mean, J_err, integrator).

### Item 1.3 — Perfiles radiales
- Sobre las mismas corridas de 1.2 (no re-correr).
- Cada Δt₂ = 0.05 s, para cada partícula con `state == FRESH` y `x·v < 0`:
  - bin S = floor(|x| / 0.2) · 0.2
  - acumular: contador `n[S]`, suma de `v_radial = (x·v)/|x|` (negativo) → `sum_v[S]`
- Al final: `⟨ρ_f^in⟩(S) = (n[S] / N_samples) / area_anillo(S)`, `⟨v_f^in⟩(S) = -sum_v[S]/n[S]` (positivo: módulo entrante), `J^in(S) = ⟨ρ⟩·|⟨v⟩|`.
- Edge case: capas con `n[S] = 0` reportan `ρ = 0`, `v = NaN` (el plotter las omite). Aclararlo en slide.
- Promediar realizaciones por capa antes de ploteear.
- CSV: `results/s2/radial_N{N}.csv` (S, rho, v, j_in, n_samples).
- Plot principal: `J^in(S)` para todos los N en una figura, **paleta gradual + colorbar** (no leyenda categórica, corrección D24/D26 del TP3). Doble plot:
  - Izquierda: rango completo S ∈ [r₀, R-r].
  - Derecha: zoom S ∈ [1.5, 5] m.
- Plot complementario: para cada N, promediar capas cercanas al obstáculo (S ∈ [1.5, 3]) y graficar `J^in_cerca`, `⟨ρ⟩_cerca`, `⟨v⟩_cerca` vs N en el mismo plot con doble eje y. Comparar con `J(N)` del 1.2 y `J(N)` del TP3 — esta comparación fue corrección explícita del TP3 (D31).

### Item 1.4 — Variación de k
- k ∈ {10², 10³, 10⁴, 10⁵}; (10⁵) condicional. Validar Δt para cada k (ver tabla arriba).
- Para cada (k, N): correr M=10 realizaciones, calcular `⟨J⟩(N, k)` y `⟨J^in|S~2⟩(N, k)`.
- Plot: 2 paneles (uno por observable). Eje x = N, una curva por k con colorbar. Ambos en log-log si la pendiente es ley de potencia.
- Identificar escalar característico vs k: usar `max_N⟨J⟩(N, k)` y `argmax_N⟨J⟩(N, k) =: N*(k)`. Plot de cada uno vs k en log-log.
- CSV: `results/s2/k_sweep.csv` (k, N, J_mean, J_err, J_in_S2_mean, J_in_S2_err).

### Item 1.5 (opcional) — primer borde
- Si queda tiempo, registrar `t_borde[real]` = primer instante en que alguna partícula USED toca el borde, en función de (N, k). No es prioridad.

### Slides Sistema 2 (~10 slides)
- 1 intro + esquema + parámetros (con esquema TikZ tipo TP3, etiquetas con `\footnotesize` mínimo). **Sin "Animación" como título.**
- 1 implementación: solo motor (interfaz `Integrator2D`, ForceModel, Cfc state machine). Sin valores numéricos (corrección TP2).
- 1 simulaciones: parámetros físicos, M=10, ventana de promediado.
- 1 animaciones (máximo 2 — corrección TP3 D16-D21): k=10³ con N pequeño y N grande. Embebidas en oral; frame + link en PDF.
- 1 series temporales: E(t), Cfc(t).
- 1 1.1 timing log-log con TP3 superpuesto.
- 1 1.2 J vs N con TP3.
- 1 1.3 perfiles radiales (panel completo + zoom).
- 1 1.3 J^in cerca + ⟨ρ⟩ + ⟨v⟩ vs N.
- 1 1.4 J(N, k) y J^in(N, k).
- 1 1.4 escalar característico vs k.
- 1 conclusiones (keywords, basadas SOLO en lo mostrado).

## Plotter (Python)

`plotter/_style.py` define rcParams una sola vez:
```python
mpl.rcParams.update({"figure.dpi": 300, "savefig.dpi": 300, "font.size": 12, ...})
GRADIENT_CMAP = "viridis"   # gradiente para N, k
```

Cada script `plot_s*.py` lee CSVs de `results/`, escribe PNG a `figures/`, y se invoca standalone (`python plot_s1_ecm_vs_dt.py`). Sin entry point único — querés iterar uno a la vez.

`requirements.txt`: `numpy`, `pandas`, `matplotlib`. No es entregable; el `.gitignore` debe permitir el directorio.

## Build y ejecución

`pom.xml` mínimo (Java 24, sin deps externas, plugin exec):
```xml
<project>
  <groupId>ar.edu.itba.sds</groupId>
  <artifactId>tp4</artifactId>
  <version>1.0</version>
  <properties>
    <maven.compiler.source>24</maven.compiler.source>
    <maven.compiler.target>24</maven.compiler.target>
  </properties>
</project>
```

Ejecución:
```
mvn compile
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema1.Main1
mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema2.Main2 \
              -Dexec.args="--experiment timing"
mvn exec:java ... -Dexec.args="--experiment jvsn --N 100 --realizations 10"
mvn exec:java ... -Dexec.args="--experiment ksweep --k 1000 --N 100 ..."
```

`scripts/run_all_s2.sh` corre el barrido completo (1.1 + 1.2/1.3 + 1.4) en orden y volcando logs a `results/log/`.

## Plan de trabajo (orden recomendado)

1. **Sistema 1 entero** (estimado: 1 día). Cierra los 2 slides; sirve para validar la arquitectura del paquete `integrators`.
2. **Sistema 2 esqueleto**: `Vec2`, `Particle`, `Geometry`, `ConfigSeeder`, `ForceModel` (sin cell list todavía), `BeemanIntegrator2D`, `Simulator2D`, `EnergyTracker`. Validar Δt para k=10³ con animación rápida y plot de E(t).
3. **CellList** y benchmark: confirmar que la simulación de N=1000 corre en tiempo razonable.
4. **Item 1.1** (timing) — barato.
5. **Item 1.2 + 1.3** sobre las mismas corridas con M=10, N ∈ {10..800}. Más caro.
6. **Item 1.4** con k ∈ {10², 10³, 10⁴} primero; agregar 10⁵ si hay tiempo.
7. **VerletIntegrator2D** y corrida de validación cruzada en N=200, k=10³, M=3.
8. **Plotter** y **slides** en paralelo desde el paso 1, releyendo §7 del CLAUDE.md antes de cada export.
9. **Item 1.5** opcional si sobra tiempo después de slides.

## Errores a no repetir — checklist auto-aplicable

Antes de marcar cada item como hecho, verificar:
- [ ] log-log para escalados (1.1, 1.4); etiquetar pendientes ajustadas.
- [ ] Decir "valor promedio durante el período estacionario", **nunca** "valor asintótico".
- [ ] Barras de error visibles, aclarar que son sobre realizaciones.
- [ ] Comparar `J` con `J^in` en anillos pequeños (corrección TP3 D31).
- [ ] No promediar antes de t = 500/2; mostrar ventana de promediado.
- [ ] Definir cada variable (Cfc, J, J^in, etc.) **antes** de usarla.
- [ ] Diagramas con etiquetas `\footnotesize` mínimo.
- [ ] Notación: θ(t+Δt) no θ(t+1); atan2 no atan; 10⁵ no 1E5; vectores en negrita sin itálica.
- [ ] Sin redacción en slides; keywords.
- [ ] Citas en la esquina del frame, **nunca** en slide final de bibliografía.
- [ ] Paleta gradiente + colorbar para N y k.
- [ ] Combinar plots (doble eje y) cuando se pueda.
- [ ] t_f no es parámetro variable; semilla no es parámetro físico.
- [ ] Máximo 2 animaciones por sistema; revisar links en PDF.
- [ ] Sin diapositiva de índice (presentación corta).
- [ ] Beamer Warsaw + miniframes; sin `\subsection{}`; numeración activada.
- [ ] Zip entregable < 100 KB; **sólo** simulación Java; sin Visualizer ni plotter ni `target/`.

## Java vs C++ — comparación breve (sidebar; queda Java)

| | Java 24 | C++ |
|---|---|---|
| Velocidad de desarrollo | + | − |
| Performance numérica raw | razonable (HotSpot) | máxima (-O3, SIMD, OMP) |
| Cuello de botella TP4 | el bottleneck es 1.4 con k=10⁵ → C++ gana 2-3× ahí | beneficio sólo si Java se queda corto |
| Tooling | Maven + IntelliJ ya instalados | CMake/Make + cuidado con UB |
| Riesgo | bajo (familiar al grupo, TP3 fue Java) | alto (poco margen para debug en 12 días) |
| Reproducibilidad inter-OS | trivial | depende del compilador |
| Cell list / SoA | viable con `double[]` | natural |
| Memory safety | GC | manual (riesgos a 12 días) |

**Veredicto:** Java es la elección correcta. El TP no se decide por velocidad de cómputo (la cátedra ya espera Java/Python para el 80% de los grupos), y lo que importa es entregar a tiempo y bien. C++ sólo se justificaría si descubrís que k=10⁵ es inviable con Java; en ese caso, podés portar **sólo** `ForceModel` + `Simulator2D` a C++ vía JNI o subproceso. No vale la pena planificarlo de entrada.

## Verificación end-to-end

1. **Sistema 1**: `mvn compile && mvn exec:java -Dexec.mainClass=ar.edu.itba.sds.sistema1.Main1`. Inspeccionar `results/s1/`. Correr `python plotter/plot_s1_*.py`. Validar visualmente (a) las 4 trayectorias se superponen a `dt=1e-3`, (b) pendientes 1, 2, 2, 4–5 en log-log de RMSE.
2. **Sistema 2 sanity**: `--experiment energy --N 100 --k 1000 --tf 5`. Plot de E(t) constante salvo fluctuación. Si deriva, bajar Δt.
3. **Sistema 2 timing**: `--experiment timing`. Plot log-log con pendiente esperada (~lineal, dado cell list).
4. **Sistema 2 J(N)**: `--experiment jvsn`. Cfc(t) crece linealmente en régimen estacionario. ⟨J⟩(N) reproduce la curva del TP3 cualitativamente (mismo orden de magnitud y forma).
5. **Sistema 2 perfiles**: `J^in(S)` cae con S y tiene un pico cerca del obstáculo. Comparar con TP3.
6. **Sistema 2 k-sweep**: `⟨J⟩(N, k)` para k=10² muy bajo (rebote suave), k=10⁵ alto (rebote casi rígido, debería parecerse al TP3 EDMD).
7. **Sin slides**: revisar checklist § "Errores a no repetir" antes de exportar PDF.
8. **Zip entregable**: `zip -r entregable.zip src pom.xml` y verificar que pesa < 100 KB.

## Critical files (a crear)

Sistema 1:
- `pom.xml`
- `src/main/java/ar/edu/itba/sds/sistema1/Oscillator.java`
- `src/main/java/ar/edu/itba/sds/sistema1/Trajectory.java`
- `src/main/java/ar/edu/itba/sds/sistema1/Simulator.java`
- `src/main/java/ar/edu/itba/sds/sistema1/integrators/{Integrator,EulerIntegrator,VerletIntegrator,BeemanIntegrator,GearPC5Integrator}.java`
- `src/main/java/ar/edu/itba/sds/sistema1/analysis/ECMSweep.java`
- `src/main/java/ar/edu/itba/sds/sistema1/Main1.java`

Sistema 2:
- `src/main/java/ar/edu/itba/sds/sistema2/{Vec2,Particle,Geometry,ConfigSeeder,CellList,ForceModel,EnergyTracker,Simulator2D,Main2}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/integrators/{Integrator2D,BeemanIntegrator2D,VerletIntegrator2D}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/observables/{CfcTracker,RadialProfile,ContactCounter}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/experiments/{TimingExperiment,JvsNExperiment,KSweepExperiment}.java`
- `src/main/java/ar/edu/itba/sds/sistema2/io/CsvWriter.java`
- `src/main/java/ar/edu/itba/sds/common/{DeterministicRandom,Stopwatch}.java`

Plotter (no entregable):
- `plotter/{requirements.txt,_style.py,plot_s1_r_vs_t.py,plot_s1_ecm_vs_dt.py,plot_s2_energy.py,plot_s2_timing_vs_n.py,plot_s2_cfc_t.py,plot_s2_j_vs_n.py,plot_s2_radial_profiles.py,plot_s2_k_sweep.py}`

Misc:
- `.gitignore` actualizado (target/, results/, figures/, plotter/__pycache__/, .DS_Store).
- `CLAUDE.md` §2.3: cambiar "α₀ = 3/16 (no 3/20)" por "α₀ = 3/20 para v-dep; 3/16 para fuerza sólo posicional" (decisión zanjada con el usuario).
- `tp3_baselines/{timing.csv,j_vs_n.csv,j_in_S2.csv}` exportados manualmente del TP3 anterior.

## Open questions (no bloqueantes; resolver durante implementación)

1. **¿Cell list redondeado a celdas cuadradas o circulares?** Default cuadradas (más simple). Si performance no alcanza para N=1000 k=10⁵, evaluar.
2. **¿Realmente plotear validación cruzada Beeman vs Verlet en slide?** Probable que no; queda como evidencia interna en `results/`.
3. **Δt₂ exacto para perfiles radiales**: 0.05 s arranca; ajustar si los perfiles son ruidosos.
4. **k = 10⁵**: si tarda > 4 h por realización, se cae del barrido; flagear "(N/A)" en plots.
5. **TP3 baselines**: hace falta que alguien del grupo exporte los CSVs del repo de TP3. Sin esto, los plots de comparación no se hacen.
