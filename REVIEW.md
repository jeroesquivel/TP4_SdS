# TP4 — Revisión técnica completa

> Revisión cruzando código (`src/main/java`), `plan.md`, `CLAUDE.md` y la wiki en `~/Desktop/ITBA/26-1C/SDS/SDS_Obsidian/wiki` (en particular `tps/TP4.md`, `conceptos/oscilador_amortiguado.md`, `metodos/{euler,verlet_original,beeman,gear_predictor_corrector}.md`, `conceptos/dinamica_molecular_paso_temporal.md`).
> Lectura como profesor de Simulación de Sistemas — busco huecos físicos y de implementación, no estilo.

---

## 0. De qué se trata el TP

TP4 (Grupo 05, ITBA 72.25). **Dinámica Molecular regida por paso temporal**, dos sistemas independientes en una sola entrega:

- **Sistema 1 — oscilador amortiguado 1D** (m = 70 kg, k = 10⁴ N/m, γ = 100 kg/s, A = 1, t_f = 5 s). Solución analítica conocida, `r(0)=1`, `v(0) = -A·γ/(2m) ≈ -0.7143`. Se comparan 4 integradores (Euler PC, Verlet original, Beeman PC, Gear PC orden 5) frente a la analítica vía **ECM(Δt) en log-log**. Como la fuerza depende de `v` (`F = -k·r - γ·v`), Beeman y Gear usan sus variantes para `f(r,v)` y Gear-5 lleva **α₀ = 3/16** (no 3/20). En la presentación: ~2 slides, < 1 min, antes del Sistema 2.
- **Sistema 2 — recinto circular con obstáculo central**. Mismo sistema físico que TP3 (recinto Ø = 80 m → R = 40 m, obstáculo r₀ = 1 m al centro, partículas r = 1 m, m = 1 kg, |v₀| = 1 m/s) pero ahora con **fuerza elástica blanda** `F_ij = -k·ξ·ê_ij` con `ξ = r_i + r_j - |x_i - x_j|`, sólo si ξ > 0. Integrador: Velocity-Verlet 2D (default) y Beeman 2D (validación cruzada). Items: 1.1 timing vs N (comparar TP3), 1.2 Cfc(t) y J(N) (comparar TP3), 1.3 perfiles radiales con paleta gradual, 1.4 variar k ∈ {10², 10³, 10⁴, (10⁵)} validando dt con energía, 1.5 opcional t_borde.
- **Entrega**: 18/05/2026 10:00. Presentación oral 13 min con animaciones embebidas, PDF (frame + link), zip < 100 KB con sólo el motor de simulación. Sin informe.

## 1. Puntos críticos del TP (no negociables)

1. **α₀ de Gear-5 = 3/16** (no 3/20) porque la fuerza del Sistema 1 depende de `v`. Verificado en `wiki/metodos/gear_predictor_corrector.md` y Teórica 4 slide 29. ✅ El código respeta esto.
2. **Beeman PC con predicción de v** (no Beeman simple) porque la fuerza depende de `v`. Sin la predicción, `aNew = F(rNew, v)` quedaría desfasado y degradaría el orden. ✅ El código respeta esto.
3. **L = 80 m es el DIÁMETRO** del recinto (en TP3 era el radio). R = 40 m. ✅ `Geometry.java` lo refleja.
4. **Cfc cuenta sólo el primer Δt del contacto** (transición fresca→usada), no cada paso de contacto. ✅ Implementado vía flag `inContactObs` en `CfcTracker`.
5. **Cfc se guarda con resolución Δt completa** (no Δt₂). ✅ El array `cfcPerStep` se llena en cada paso; el CSV se sub-muestrea sólo para el archivo, pero la pendiente J se ajusta sobre el array completo (`computeJ` usa `dt`, no `dt₂`).
6. **Validar Δt con conservación de energía total** para cada k. ⚠️ **Hueco** (ver §3.2).
7. **Comparar con TP3** en 1.1, 1.2 y 1.3 (J vs J^in en anillos pequeños). El código no carga TP3 — la comparación queda para el plotter, fuera del entregable. ⚠️ Verificar que `tp3_data/` esté presente al armar slides.
8. **Slope log-log = ley de potencia, no exponencial**. Fue corrección explícita del TP3. ⚠️ Asunto del plotter, no del motor.
9. **Barras de error sobre realizaciones** (no sobre tiempo) y aclararlo. ⚠️ El motor reporta `J_std` por realización; el plotter debe etiquetarlo.
10. **Promediar perfiles sólo en régimen estacionario**. ✅ `RadialProfileAccumulator.snapshot` se llama únicamente cuando `t ≥ tf/2`.

## 2. Edge cases que el motor maneja bien

- **CIM con celda = 2·r = 2 m**: el rango de contacto entre dos partículas es exactamente `r_i + r_j = 2 m`. Con celda de 2 m, el barrido de la media-vecindad cubre todos los pares `dist ≤ 2`. Pares en celdas a distancia ≥2 en X o Y tienen `dist ≥ 2 = r_i + r_j`, donde `ξ ≤ 0` y la fuerza es nula. ✅ Tamaño de celda al borde, pero válido.
- **Solapamiento exacto ξ = 0** o `dist = 0`: `ForceModel` filtra `xi <= 0.0 || dist == 0.0`. ✅ Sin división por cero.
- **Partícula entre obstáculo y borde simultáneamente**: imposible geométricamente con r = 1 m, R = 40 m, r₀ = 1 m, así que el orden de detección obstáculo/borde no introduce ambigüedad.
- **Seeding denso**: `ConfigSeeder` cae a un grid hexagonal cuando packing > 0.45 o agotó `1000·N` intentos. Para N = 1000, la fracción es ~0.20 — siempre cae en rejection sampling.
- **Verlet 1D con v dependiente**: el código usa `vLag` (la velocidad central del paso anterior) para evaluar `F(r, v)`. La consigna pide "Verlet original" y Verlet original no maneja `f(r,v)` por construcción; esta variante didáctica es lo aceptado por la cátedra (ver más adelante el efecto sobre el orden).
- **Floor numérico de Gear-5**: se ve a Δt = 10⁻³ con ECM ≈ 3·10⁻²² (RMSE ≈ 1.7·10⁻¹¹) — está al piso de doble precisión. Para Δt < 10⁻³ el ECM **sube** por roundoff; el plotter debe dejar de marcar pendiente ahí y aclararlo en el slide.

## 3. Findings (lo que conviene revisar / arreglar)

### 3.1 ⚠️ Pendiente empírica de Verlet en Sistema 1: ~2 (no ~4)

El CSV `results/s1/ecm_sweep.csv` ya generado muestra:

| Δt | ECM Verlet | ratio vs Δt anterior |
|---|---|---|
| 1e-2 | 5.73e-4 | — |
| 1e-3 | 4.90e-6 | × 10² |
| 1e-4 | 4.82e-8 | × 10² |
| 1e-5 | 4.81e-10 | × 10² |
| 1e-6 | 4.81e-12 | × 10² |

⇒ **slope ≈ 2** (Euler también da slope ≈ 2). En cambio, plan.md y wiki anticipan slope ≈ 4 para Verlet. Beeman sí da ~4 y Gear-5 ~10 (hasta el floor) — esos están OK.

**Por qué pasa**: la velocidad usada para evaluar `F(r,v)` es la central del paso anterior, así que tiene un desfasaje O(Δt). Eso introduce un error O(Δt) en `a`, que se propaga a un error O(Δt²) global en `r` (orden 1, no 2). Es coherente con que "Verlet original asume `f = f(x)`".

**Acción recomendada**:
- **No "arreglar" subiendo a Velocity-Verlet en Sistema 1**: la consigna pide Verlet original.
- **Sí** anotar en el slide del ECM: *"Verlet original ve `v` lageada un paso ⇒ pendiente ≈ 2 en lugar de 4 para `f(r,v)`."* Esto evita que el corrector marque "Verlet con orden incorrecto" porque la pendiente se ve igual a Euler.
- Alternativamente, reportar también la pendiente teórica en la curva de la analítica y discutir el contraste explícitamente. Es un punto del que la cátedra te puede preguntar oralmente: tener la respuesta lista.

### 3.2 ⚠️ Validación de Δt con energía no se hace en `KSweepExperiment` ni `JvsNExperiment`

`EnergyExperiment.java` graba `e_kin, e_pot, e_total` y se corre con `--experiment energy`, **pero sólo para una (N, k) puntual**. En `run_all_s2.sh` se invoca para `--N 100 --k 1000 --tf 5`. No se valida Δt para k = 10², 10⁴ (ni 10⁵). El enunciado pide explícitamente "validar el Δt para cada k" y la wiki marca este punto como **CRÍTICO** (`conceptos/dinamica_molecular_paso_temporal.md`, `tps/TP4.md`).

**Acción recomendada** (mínima):
- Antes del barrido `ksweep`, agregar al `run_all_s2.sh`:
  ```bash
  for k in 100 1000 10000; do
    run_stage "ENERGY_k${k}" "--experiment energy --N 100 --k $k --tf 5"
  done
  ```
- En `JvsNExperiment.runForN` ya se escribe `energy_N{N}_real{r}.csv` (línea 50) **pero sólo si `writePerRealization=true`**, que es el caso. ✅ Confirmado: hay `energy_N100_real0.csv` etc. en `results/s2/`. Revisar al menos esas trazas para k = 10³ antes del entregable.
- Para `KSweepExperiment` agregar logging similar (al menos para una realización por (k, N)) — actualmente no se exporta E(t) en ese flujo.

### 3.3 ⚠️ Inconsistencia entre `CLAUDE.md` y `plan.md` sobre las pendientes esperadas

`CLAUDE.md` §2.4 dice:
```
- Euler ≈ 1
- Verlet ≈ 2
- Beeman ≈ 2 (3 si la fuerza fuera independiente de v)
- Gear-5 ≈ 4–5
```

`plan.md` y wiki dicen 2 / 4 / 4 / 10. Las dos son verdaderas para distintas magnitudes:
- Slopes 1 / 2 / 2 / 5 corresponden a **RMSE = √ECM** (= orden global del método).
- Slopes 2 / 4 / 4 / 10 corresponden a **ECM** (= 2 × orden).

El motor exporta **ambas columnas** (`ecm`, `rmse`) — el plotter elige cuál graficar. **El slide y la curva tienen que coincidir**: si se grafica ECM, anotar pendientes 2 / 4 / 4 / 10; si se grafica RMSE, anotar 1 / 2 / 2 / 5.

**Acción recomendada**: actualizar `CLAUDE.md` §2.4 para deshacer la ambigüedad. La consigna y la teórica usan **ECM** (`(1/N)·Σe²`), así que los números a citar en el slide son **2 / 4 / 4 / 10** — y de hecho son los empíricos del CSV (excepto Verlet, ver §3.1).

### 3.4 ⚠️ `EnergyExperiment` mezcla la prueba de validación con argumentos

`Main2.java` hace:
```java
double tf = Double.parseDouble(opts.getOrDefault("tf", "500"));      // tf = 500
case "energy" -> {
    double tfShort = Double.parseDouble(opts.getOrDefault("tf", "5")); // tfShort = 500 si --tf
    EnergyExperiment.run(N, k, tfShort, ...);
}
```

Es funcional pero **shadowing del `tf` exterior**. Si alguien llama `--experiment energy --tf 100`, `tfShort=100` y se sobreescribe el default 5. Funciona, pero la lectura es confusa. No es bug — es deuda técnica menor.

### 3.5 ⚠️ Inicialización de `aPrev` en Beeman 2D

`BeemanIntegrator2D` en el constructor hace:
```java
forceModel.compute(particles);
for (Particle p : particles) p.accPrev.set(p.acc);
```

→ se asume `a(-Δt) = a(0)`. Es la opción "constante" (la wiki la lista como aceptable). Para t_f largos el efecto del primer paso se diluye, pero **estrictamente** se podría inicializar con un Taylor backward (como sí se hace en `BeemanIntegrator` 1D). Para el TP no es bloqueante; sólo notar que el primer paso tiene precisión reducida.

En cambio, `BeemanIntegrator` (Sistema 1) sí inicializa con Taylor backward — coherente entre los dos sólo si la cátedra acepta cualquiera. Lo común es decir: "para Beeman 2D arrancamos con `a(-Δt) = a(0)` y el primer paso queda con error O(Δt²)" — está bien si se lo aclara.

### 3.6 ℹ️ `FirstBorderTracker` está implementado pero ningún experimento lo usa

`observables/FirstBorderTracker.java` registra el primer t en que una partícula USED toca el borde — exactamente lo del item **1.5 opcional**. Pero ningún `Experiment` lo invoca. Si se decide hacer 1.5 hay que enchufarlo (idealmente como hook adicional en `JvsNExperiment.runForN`).

Si no se va a hacer 1.5, **borrar el archivo** para no inflar el zip entregable. El enunciado pide < 100 KB.

### 3.7 ℹ️ `JvsNExperiment.runForN` siempre escribe CSVs por realización

`writePerRealization=true` siempre que se lo llama desde `runSweep`. Para `Ns = {10, 20, 50, 100, 200, 400, 800, 1000}` con M = 10 son **160 CSVs** (cfc + energy). Está bien para análisis interno, pero estos archivos NO van al zip entregable. Verificar que el `.gitignore` y/o el script de empaquetado los excluyan.

### 3.8 ℹ️ Memoria de `CfcTracker` para Δt chico

`new int[totalSteps + 1]` con `totalSteps = round(tf/dt)`:
- k = 10⁴ ⇒ Δt ≈ 6.3·10⁻⁴, tf = 500 ⇒ ~793k entradas (~3 MB) por realización.
- k = 10⁵ ⇒ Δt ≈ 2·10⁻⁴ ⇒ 2.5M entradas (~10 MB).

Se libera al final de cada realización. No es problema con `realizations=10` secuenciales. Sólo notarlo si en algún momento se paraleliza.

### 3.9 ℹ️ Régimen J_in cerca del obstáculo: rango `[1.5, 3.0]` vs el zoom `[1.5, 5]` del enunciado

`KSweepExperiment.S_NEAR_LO/HI = {1.5, 3.0}` (línea 24-25). El enunciado y el plan piden mostrar el zoom de J^in en S ∈ [1.5, 5] m. Para el escalar "J^in cerca del obstáculo", `[1.5, 3.0]` es defendible (es el primer pico) pero **debe quedar explícito en el slide** qué rango se usa. Recomiendo:
- En el slide del J^in zoom: graficar todo [1.5, 5] m.
- En el escalar reportar exactamente el rango que usa el código (1.5 ≤ S ≤ 3.0). No mezclar.

### 3.10 ℹ️ `dt = τ/100` con τ = 2π√(m/k)

`Geometry.dtForK` usa τ = 2π·√(m/k) (período completo del oscilador de contacto). El plan.md y la wiki se refieren a "τ_col ≈ π·√(m/k)" o "τ ≈ 2π·√(m/k)" indistintamente. Lo importante: `dt/τ_col ≈ 1/100` está bien. La heurística del CLAUDE.md ("τ_col ≈ 0.20 s para k = 10³") corresponde a 2π·√(1/1000) ≈ 0.199 s — coincide. ✅

### 3.11 ℹ️ Reducción de dimensión para Verlet 1D y Beeman 1D — inicializaciones distintas

- `VerletIntegrator`: `rPrev = r₀ - dt·v₀ + ½·dt²·a(0)` — Taylor backward correcto.
- `BeemanIntegrator`: `rPrev` igual; pero `vPrev = v₀ - dt·a(0)` — Taylor backward truncado a primer orden, **inconsistente** con `rPrev` que sí lleva término cuadrático.

No es un bug grave (el primer paso siempre tiene error reducido y luego se diluye), pero si querés simetría conviene `vPrev = v₀ - dt·a(0) + ½·dt²·jerk(0)`. Diría que **lo dejes**, pero si la cátedra te pide derivar la inicialización, tener la inconsistencia identificada.

### 3.12 ℹ️ `ECM = sumSq / (totalSteps + 1)` — divisor

El divisor es `N+1` porque se incluye t=0 en la suma. La consigna escribe `(1/N_pasos)·Σ`. No afecta la pendiente, pero al reportar el ECM absoluto cuidá la consistencia con la analítica si te lo preguntan oralmente. Mi recomendación: dejar como está y, si te preguntan, decir "promedio sobre los N+1 puntos del muestreo, equivalente al promedio sobre N pasos al 0.X% de error".

## 4. Errores de presentación a vigilar (re-derivados de §7 de `CLAUDE.md`)

Estos son errores recurrentes del grupo (TP2 4.5/10, TP3 6/10) — el motor no los puede prevenir, pero conviene tenerlos a mano antes de exportar slides:

1. **Sin slide de bibliografía**: citar en la esquina del frame específico.
2. **Notación**: `θ(t+Δt)` no `θ(t+1)`. `atan2`. Notación científica con superíndices, no `1E5`.
3. **N como variable continua → paleta gradual + colorbar**, no leyenda categórica.
4. **No "valor asintótico"**: usar "valor promedio durante el período estacionario".
5. **Ventana de promediado visible** (en Cfc(t), marcar la mitad usada para el ajuste de J).
6. **Pendiente en log-log → ley de potencia**, no "exponencial". Corrección explícita del TP3.
7. **Combinar plots**: ⟨ρ⟩, ⟨v⟩, J^in cerca del obstáculo → un único plot con doble eje y, no tres separados.
8. **Animaciones**: máximo 2 por sistema; embebidas en oral; PDF con frame + link (probar el link copiado).
9. **Código entregado**: SOLO motor. Sin Visualizer, sin plotter Python, sin `results/`, sin `figures/`. Verificar zip < 100 KB.
10. **Defensa oral**: practicar con cronómetro. La crítica "no parecen haberlo practicado" fue explícita en el TP3.

## 5. Resumen ejecutivo

| Sistema | Estado | Bloqueantes |
|---|---|---|
| Sistema 1 — Euler PC | ✅ slope 2 (orden 1) | — |
| Sistema 1 — Verlet original con v lageada | ⚠️ slope **2 empírico**, no 4 | Aclarar en slide (§3.1) |
| Sistema 1 — Beeman PC | ✅ slope 4 (orden 2) | — |
| Sistema 1 — Gear-5 con α₀ = 3/16 | ✅ slope 10 hasta el floor numérico | Marcar floor en slide |
| Sistema 2 — Velocity Verlet 2D + CIM | ✅ | Validar E(t) para k = 10², 10⁴ (§3.2) |
| Sistema 2 — Cfc primer Δt + estado fresca/usada | ✅ | — |
| Sistema 2 — Perfiles radiales en régimen estacionario | ✅ | Aclarar rango del escalar (§3.9) |
| Sistema 2 — KSweep | ⚠️ sin energía por k | (§3.2) |
| Sistema 1 — `CLAUDE.md` vs `plan.md` pendientes | ⚠️ inconsistencia | Reescribir §2.4 de CLAUDE.md (§3.3) |
| Entregable zip < 100 KB | ⚠️ | Excluir `plotter/`, `results/`, `tp3_data/`, `target/` |

**Acciones priorizadas para el día previo a la entrega**:
1. Validar E(t) para k = 10² y k = 10⁴ (correr `--experiment energy` con cada k).
2. Anotar la pendiente real de Verlet (≈2) en el slide ECM y explicar el v-lag.
3. Reescribir `CLAUDE.md` §2.4 para eliminar la ambigüedad ECM vs RMSE.
4. Si no se hace 1.5, eliminar `FirstBorderTracker.java`.
5. Verificar que el zip entregable no incluya `plotter/`, `tp3_data/`, ni los CSVs por realización.
