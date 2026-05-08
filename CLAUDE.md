# CLAUDE.md — TP4 Simulación de Sistemas (72.25)

> **Contexto del proyecto.** Trabajo Práctico 4 del curso *Simulación de Sistemas* (ITBA, 72.25), Grupo 05.
> **Integrantes:** Sebastián Caules, Jerónimo Esquivel, Andrés Cortese.
> **Entrega:** 18/05/2026.
> **Tema:** Dinámica Molecular regida por el paso temporal (Time-Driven MD).

---

## 0. Prioridad para Claude

Antes de escribir código o slides, releer las secciones **§7 (Errores a no repetir)** y **§8 (Estilo de presentación)**. La nota del TP3 fue **4,5/10** y la del TP2 fue **6/10**: la mayor parte del puntaje perdido vino de errores de presentación e interpretación, no de la simulación en sí. La consigna del TP4 está bien acotada; el desafío es no repetir lo de antes.

---

## 1. Resumen ejecutivo de la consigna

El TP tiene **dos sistemas independientes** que se entregan en una única presentación de **13 minutos** y un único informe.

| | **Sistema 1** | **Sistema 2** |
|---|---|---|
| Modelo | Oscilador amortiguado 1D | Recinto circular con obstáculo |
| Objetivo | Comparar 4 integradores (ECM vs Δt) | Reproducir el TP3 con DM por paso temporal |
| Slides asignados | ~2, solo resultados, < 1 min | El resto de la presentación |
| Sin… | introducción, implementación, animaciones | — |

---

## 2. Sistema 1 — Oscilador amortiguado

### 2.1 Parámetros (de Teórica 4, slide 36)

| Símbolo | Valor | Unidad |
|---|---|---|
| m | 70 | kg |
| k | 10⁴ | N/m |
| γ | 100 | kg/s |
| t_f | 5 | s |
| r(0) | 1 | m |
| v(0) | −A·γ/(2m) con A = 1 m | m/s |

### 2.2 Solución analítica (régimen subamortiguado)

```
r(t) = A · exp(−γ·t / (2m)) · cos(√(k/m − γ²/(4m²)) · t)
```

Con estos valores: ω₀² ≈ 142,35 rad²/s², ω ≈ 11,93 rad/s, T ≈ 0,527 s.

### 2.3 Integradores a comparar

Los cuatro deben implementarse **respetando que la fuerza depende de la velocidad** (`f = −k·r − γ·v`). Esto cambia los detalles de Beeman y Gear (ver abajo).

#### Euler modificado (Teórica 4, slide 10)

```
v(t+Δt) = v(t) + Δt · a(t)
r(t+Δt) = r(t) + Δt · v(t+Δt)        ← usa v ya actualizada
```

#### Verlet original (slide 13)

```
r(t+Δt) = 2·r(t) − r(t−Δt) + Δt² · a(t)
v(t)     = (r(t+Δt) − r(t−Δt)) / (2·Δt)
```

Inicialización: `r(−Δt) ≈ r(0) − Δt·v(0) + Δt²·a(0)/2`.

#### Beeman predictor-corrector (slides 19–20, fuerzas dependientes de v)

```
1. r(t+Δt) = r(t) + v(t)·Δt + (2/3)·a(t)·Δt² − (1/6)·a(t−Δt)·Δt²
2. v_pred  = v(t) + (3/2)·a(t)·Δt − (1/2)·a(t−Δt)·Δt
3. a(t+Δt) = f(r(t+Δt), v_pred) / m
4. v_corr  = v(t) + (1/3)·a(t+Δt)·Δt + (5/6)·a(t)·Δt − (1/6)·a(t−Δt)·Δt
```

#### Gear PC orden 5 (slides 24–30)

Coeficientes para fuerza dependiente de v: **α₀ = 3/16** (no 3/20), α₁ = 251/360, α₂ = 1, α₃ = 11/18, α₄ = 1/6, α₅ = 1/60.

Inicialización para `f = −k·r − γ·v`:

```
r₂(0) = (−k·r(0) − γ·v(0))   / m
r₃(0) = (−k·v(0) − γ·r₂(0))  / m
r₄(0) = (−k·r₂(0) − γ·r₃(0)) / m
r₅(0) = (−k·r₃(0) − γ·r₄(0)) / m
```

### 2.4 Métrica

```
ECM = (1 / N_pasos) · Σ_n  (r_num(t_n) − r_ana(t_n))²
```

Probar `Δt ∈ {10⁻², 10⁻³, 10⁻⁴, 10⁻⁵, 10⁻⁶} s`. Pendientes esperadas en log-log **del ECM** (recordar: `ECM ~ Δt^(2·orden)`; RMSE = √ECM tiene la mitad del slope):

| Integrador | slope ECM teórico | slope ECM empírico (CSV) | nota |
|---|---|---|---|
| Euler PC | 2 | 2.0 | orden 1 ✅ |
| Verlet original | 4 | **2.0** | la `v` lageada para evaluar `f(r,v)` baja el orden efectivo a 1 — aclarar en el slide |
| Beeman PC | 4 | 4.0 | orden 2 con predicción de `v` ✅ |
| Gear-5 (α₀ = 3/16) | 10 | 10 hasta el piso numérico (~10⁻²² a Δt = 10⁻³) | el CSV muestra el floor para Δt < 10⁻³ |

**El slide grafica ECM** (lo pide la consigna y la teórica slide 36). Las pendientes a anotar son las de la columna empírica.

### 2.5 Slides a entregar (~2 slides)

1. **r(t) numérico vs analítico** para los 4 integradores con un Δt razonable (ej. 10⁻³).
2. **log-log ECM vs Δt** con las 4 curvas. Conclusión en keywords (ej. "Gear-5: menor ECM y mayor pendiente").

---

## 3. Sistema 2 — Recinto circular con obstáculo fijo

### 3.1 Geometría

- **L = 80 m es el DIÁMETRO** del recinto (en TP3 era el radio — atención al cambio).
  → Radio del recinto R = 40 m.
- Obstáculo fijo central, radio **r₀ = 1 m**.
- Partículas: N ∈ [100, 1000], r = 1 m, v₀ = 1 m/s, m = 1 kg.
- Constante elástica: **k = 10³ N/m** (referencia, se varía en 1.4).

### 3.2 Fuerza entre pares (contacto, no choque instantáneo)

```
ξ = r_i + r_j − |x_i − x_j|             (solapamiento, sólo si ξ > 0)
F_ij = k · ξ · ê_ij                     (ê_ij dirección normal i→j)
```

Para el obstáculo y el borde se usa la misma forma, tomándolos como entes de masa infinita.

### 3.3 Validación del Δt (CRÍTICO)

- Período típico de colisión: τ_col ≈ 2π·√(m/k).
    - k = 10³ → τ_col ≈ 0,20 s → Δt ≤ 4·10⁻³ s.
    - k = 10⁵ → τ_col ≈ 0,02 s → Δt ≤ 4·10⁻⁴ s.
- **Validar siempre con conservación de energía**: graficar E(t) = E_cin + E_pot. Si oscila o deriva, Δt es muy grande.
- Δt₂ (sampling) puede ser mayor que Δt (integración) para mantener archivos manejables.

### 3.4 Ítems de análisis

#### 1.1 — Tiempos de ejecución vs N

- Plot t_exec vs N comparado con el TP3 (EDMD).
- DM por paso temporal: O(N²) por par de fuerzas, pero sin scheduling de eventos.
- Esperado: EDMD escalaba peor con N porque el nº de eventos crece más que linealmente; DM debería tener una pendiente más predecible.
- **Hacer el plot en log-log para identificar la ley de escalado correctamente.** Si es lineal en log-log → ley de potencia (no exponencial).

#### 1.2 — Cfc(t) y J(N) — comparación con TP3

- Estados: 0 = fresca, 1 = usada. Fresca → usada al chocar con obstáculo central; usada → fresca al chocar con borde.
- Cfc(t) = nº acumulado de partículas frescas que tocaron el obstáculo por primera vez.
- **Detalle clave**: ahora los contactos duran muchos Δt. Sólo contar el **primer Δt del contacto** (transición fresca→usada).
- Guardar Cfc(t) con resolución Δt completa (no Δt₂).
- J(N) = pendiente de Cfc(t) por interpolación lineal sobre el régimen estacionario, promediada sobre **múltiples realizaciones** (indicar cuántas) con barras de error sobre realizaciones.

#### 1.3 — Perfiles radiales

Sobre las simulaciones de 1.2:
- Capas concéntricas dS = 0,2 m sobre S = |x_j| (distancia al centro).
- Filtro: sólo partículas **frescas con velocidad radial entrante** (x_j · v_j < 0).
- Tres perfiles para todos los N (con escala de color tipo **gradiente** + colorbar):
    - ⟨ρ_f^in⟩(S) = nº partículas en capa / área de la capa, promediado en tiempo y realizaciones.
    - |⟨v_f^in⟩(S)| con v_f^in_j = (x_j · v_j) / |x_j|.
    - J^in(S) = ⟨ρ_f^in⟩(S) · |⟨v_f^in⟩(S)|.
- **Zoom de J^in en S ∈ [1,5; 5] m** para identificar el régimen del obstáculo.
- Promediar capas cercanas al obstáculo y graficar J^in, ⟨ρ⟩, ⟨v⟩ vs N (combinarlas en un mismo plot con doble eje y; **no hacer un plot por observable**).
- **Comparar J^in(N) con J(N) del TP3** y con J(N) propio del 1.2 — el corrector del TP3 marcó explícitamente que faltaba comparar el scanning rate con J^in en anillos pequeños.

#### 1.4 — Variación de k

- k ∈ {10², 10³, 10⁴, 10⁵} N/m. **Validar el Δt para cada k** (ver §3.3).
- Comparar ⟨J⟩(N) y ⟨J^in|S~2⟩(N) entre los cuatro k.
- Identificar un escalar característico (ej. N* que maximiza ⟨J⟩, o max⟨J⟩ vs k) y graficarlo vs k.

---

## 4. Teoría de fondo (Teórica 4)

- **MD time-driven vs event-driven**: TP3 fue event-driven; TP4 es time-driven. La distinción es importante en la presentación: las animaciones acá *sí* son por paso temporal y eso es correcto.
- Los integradores se diferencian por:
    - **Orden global del error** (Euler 1, Verlet/Beeman 2, Gear-5 4–5).
    - **Costo por paso** (Gear-5 hace muchas evaluaciones por paso, pero con Δt grande es competitivo).
    - **Conservación de energía a largo plazo** (Verlet es simpléctico para sistemas hamiltonianos; Gear no, pero con Δt chico no se nota).
- Para fuerzas dependientes de la velocidad, hay que usar la **variante predictor-corrector** de Beeman y la **fila modificada de coeficientes** de Gear.

---

## 5. Bibliografía

| Ref | Descripción | Uso |
|---|---|---|
| **[1]** Gear, C.W. (1966), *The Numerical Integration of Ordinary Differential Equations* | Paper original del método PC. Tabla de Pascal del predictor coincide con la teórica. | Cita teórica para Sistema 1. |
| **[2]** Hou & Mišković (2008), *A Gear-like Predictor-Corrector Method for Brownian Dynamics Simulation* | Variante del Gear PC para BD; en límite γ→0 coincide con Gear estándar. Confirma α₀ = 3/16. | Citar sólo si se discute energy drift; **no es directamente aplicable** (es BD, no MD). |
| **[3]** Heermann, *Computer Simulation Methods* | Libro de referencia general. | Cita teórica genérica. |
| **[4]** Teórica 4 (cátedra) | 45 slides con la deducción de los integradores. | Referencia operativa principal. |
| **[5]** TP3 propio del grupo (referencia para 1.1, 1.2, 1.3) | Comparativas requeridas explícitamente por la consigna. | Comparación directa de J(N) y t_exec. |

**Importante:** las citas van en la **esquina del frame específico** donde se usan, no en una diapositiva final de bibliografía.

---

## 6. Estructura del proyecto

Esquema paralelo al TP3, con un nuevo paquete de integradores.

```
src/
├── simulation/
│   ├── Particle.java
│   ├── SimulationResult.java
│   ├── Simulator.java          ← acepta un Integrator
│   └── Simulate.java           ← entry point por sistema
├── integrators/
│   ├── Integrator.java         ← interfaz (step(state) → state')
│   ├── EulerIntegrator.java
│   ├── VerletIntegrator.java
│   ├── BeemanIntegrator.java
│   └── GearPC5Integrator.java
├── analysis/
│   ├── SimulationAnalyzer.java
│   └── Analyze.java
├── visualization/
│   └── Visualizer.java         ← Swing, NO se entrega como código
└── Main.java
```

**Flujo:** `Simulate → simulations/*.txt → Analyze → results/{cfc,radial,timing,k_sweep}*.txt → plotter.py → figures/`.

**Build:** Maven, scripts auxiliares para correr barridos.

---

## 7. Errores a no repetir (tomados de las correcciones de TP2 y TP3)

### 7.1 Errores estructurales del trabajo

> **Importancia máxima.** Revisar antes de subir cualquier entregable.

- **No referenciar variables (Cfc, Fu, J, te, M, T, n_k^in, etc.) antes de definirlas.** Esto vino del TP3 y costó varios puntos.
- **Diagramas simplificados** que se enfoquen en lo que se quiere mostrar. No incluir cajas extra que no aportan.
- **Aclarar fórmulas explícitas** y casos borde:
    - Cómo se actualizan posiciones y velocidades en cada integrador (fórmula completa).
    - Cómo se calcula la fuerza con un objeto fijo (masa infinita).
    - Qué pasa si el número de partículas en una capa es cero (denominador en ρ, v).
- **Aclarar cómo se promedia cada observable temporal**: ventana, # realizaciones, desvío estándar.

### 7.2 Errores de análisis

- **Usar log-log o semi-log para análisis de escalado.** Lineal en log-log ⇒ **ley de potencia**, NO exponencial. Confundir esto fue un error puntual del TP3.
- **Evitar "valor asintótico"**: usar **"valor promedio durante el período estacionario"**.
- **Barras de error visibles** y aclarar que son sobre realizaciones (no sobre tiempo, salvo que se indique).
- **Comparar J con J^in en anillos pequeños** (corrección explícita del TP3 para la comparación TP3↔TP4).
- **No promediar antes de t = 500** (o el equivalente que corresponda); mostrar la ventana de promediado en los plots.
- **No concluir sin datos suficientes**: solo afirmar lo que esté explícitamente respaldado por una figura mostrada.

### 7.3 Errores de presentación / figuras

- **Sin diapositiva de índice** (presentación corta).
- **Figuras sin títulos ni captions dentro del frame** — los parámetros van al costado o como texto del frame.
- **Parámetros NO van en títulos de slides.** "Animación" no es título válido.
- **Notación**:
    - θ(t+Δt), no θ(t+1).
    - atan2, no atan.
    - Notación científica con superíndices (10⁵), no 1E5.
    - Vectores en negrita sin itálica; escalares en itálica; unidades rectas.
- **No redactar en slides**: keywords, no oraciones. ("Inputs", no "Los inputs utilizados fueron:").
- **Aumentar la letra en esquemas** (etiquetas tipo r_c, L). En TikZ usar al menos `\footnotesize`.
- **Citar referencias en la esquina del frame específico**, nunca en una diapositiva final.
- **Usar escalas tipo gradiente** para variables numéricas continuas (N, k) — NO sombreado, NO colores discretos saturados — para que el color asocie inequívocamente con la curva.
- **Combinar plots cuando sea posible** (ej. ρ y v en doble eje vertical) o repartirlos en más slides. No saturar una slide con muchos subplots.
- **Tiempo final NO es parámetro variable** — no listarlo como variable de barrido.
- Para N basta dar el rango N ∈ [a, b], no enumerar cada valor.
- Incluir el **número de realizaciones** como información explícita; **no incluir la semilla** como parámetro físico.
- **Aclarar cuándo se guarda el output** (qué Δt₂ se usa para muestrear).

### 7.4 Errores de animaciones

- **Máximo 2 animaciones por sistema** (en TP3 se pusieron 5 y se penalizó).
- **No es necesario aclarar tf en cada animación.**
- En presentación en vivo: **animaciones reales embebidas** (videos).
- En PDF entregable: **fotograma + link**.
- **Revisar los links pegados**: al copiar a veces se cuelan caracteres no alfabéticos que rompen la URL.

### 7.5 Errores del informe

- **Sección Implementación: NO incluir valores numéricos** de los parámetros (esos van en Simulaciones).
- **Sección Simulaciones**: parámetros físicos del sistema. La semilla y el tiempo de simulación NO son parámetros físicos.
- **Sección Resultados**: no mencionar "estado estacionario" antes de introducir las figuras que lo evidencian.

### 7.6 Errores de defensa oral

- **Practicar la presentación con anticipación.** No pensar en el momento qué decir sobre material preparado previamente. Esta crítica fue explícita y recurrente.

### 7.7 Errores de entrega de código

- **Enviar SOLO el código de simulación.** Sin Visualizer, sin código de análisis, sin plotter Python.

---

## 8. Estilo de presentación (Beamer)

- Tema **Warsaw con miniframes** (dots de progreso). Sin `\subsection{}` para evitar filas extra:
  ```latex
  \useoutertheme[subsection=false,footline=empty]{miniframes}
  ```
- Numeración de diapositivas activada (corrección 1.2 de la guía).
- Para videos embebidos en presentación en vivo: paquete **movie15** + visor **pympress** para autoplay.
- Dos PDFs entregables:
    - `ppt.pdf` — versión de presentación en vivo (animaciones embebidas).
    - `ppt_entregable.pdf` — versión de entrega (fotograma + link en cada slide de animación).
- Estructura sugerida según GuiaPresentaciones:
    - Sin sección "Bibliografía".
    - Diapositiva-título por sección, secciones sin numerar.
    - Introducción: máx. 3 slides, sin sistema particular.
    - Implementación: solo motor, sin I/O.
    - Simulaciones: esquema particular + observables + nº de repeticiones.
    - Resultados: por escenario, **animación → serie temporal → input vs observable**.
    - Conclusiones: 1 slide, solo basadas en lo mostrado.

---

## 9. Plan de trabajo sugerido (orden recomendado)

1. **Sistema 1 primero** (es chico y autocontenido): implementar los 4 integradores en Java o Python, validar contra solución analítica, generar el log-log ECM vs Δt. Cierra los 2 slides.
2. **Sistema 2 — esqueleto de simulación**: Particle, Simulator con un solo integrador (Verlet o Beeman; Gear-5 es overkill acá), forzado del solapamiento ξ.
3. **Validación de Δt** con conservación de energía para k = 10³.
4. **1.1 Tiempos vs N** — barrido rápido, con pocos pasos, solo para escalado.
5. **1.2 Cfc(t) y J(N)** — corridas largas con múltiples realizaciones.
6. **1.3 Perfiles radiales** — sobre las mismas corridas de 1.2.
7. **1.4 Variación de k** — el más caro, dejar para el final. Reusar pipeline de 1.2/1.3.
8. **Plotter** y **slides** en paralelo, releyendo §7 antes de cada export.

---

## 10. Estilo de trabajo con Claude (preferencias del usuario)

- Iterativo y conciso. Correcciones del estilo *"borrá X"*, *"más sencillo"*, *"damelo listo para copiar y pegar"*.
- Preferencia por **código y LaTeX listos para copiar y pegar** directamente, sin reestructurar partes que no se pidieron tocar.
- En las respuestas, evitar excesivo preámbulo y meta-comentario.