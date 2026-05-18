# Sistema 2 — Resultados (~7 min)

## Animaciones N (~45 s, 2 slides)

> "Mismo k = 10³. N = 100: pocas colisiones, las usadas vuelven rápido al borde. N = 1000: alta densidad, muchas usadas atrapadas cerca del obstáculo. El sistema entra en régimen estacionario rápidamente en ambos."

## Conservación de energía vs Δt (~45 s, 2 slides)

> "Para k = 10³ barremos Δt. Curvas con gradiente viridis: a Δt grande la energía deriva claramente; a Δt chico se mantiene dentro del 1%. Definimos S_e como la pendiente del drift relativo. Elegimos Δt = 10⁻³ s — drift < 0.1% en 2000 s, equivalente a τ_col/200."

## Tiempo de ejecución vs N (~45 s)

> "Log-log: ambas siguen ley de potencia en el rango común [100, 800]. TP4 con DM y CIM: α = 1.06 — escala lineal con N. TP3 con EDMD: α = 3.62, escala muy peor porque la priority queue de eventos crece más que cuadrática. Cruce en N ≈ 500: DM gana para N grande."

## Evolución Cfc(t) (~30 s)

> "C_fc(t) crece linealmente tras un transitorio corto. J es la pendiente del ajuste lineal sobre [0, t_f] completo — el transitorio es despreciable. Más N implica más eventos por segundo, pero también más bloqueo cerca del obstáculo."

## ⟨J⟩ vs N (~30 s)

> "⟨J⟩ crece con N, satura cerca de N* ≈ 500–600, y decae. Barras de error sobre 10 realizaciones. La forma reproduce la J(N) del TP3 en el mismo rango."

## Perfiles radiales J^in(S) (~45 s, 2 slides)

> "Una curva por N con colorbar. J^in es chico en S grande, crece hacia el obstáculo y muestra estructura en S ∈ [1.5, 5] m por la zona de partículas usadas que retornan. En la franja S ∈ [1.5, 3]: la densidad ⟨ρ⟩ tiene la misma forma que J^in — crece, satura y decae con máximo en N ≈ 500. |⟨v⟩| crece suavemente con N, alcanza máximo en N ≈ 800 (≈0.68 m/s) y luego baja. El máximo de J^in viene dominado por la densidad."

## Comparación J^in vs J(N) TP3 (~30 s)

> "El flujo en el anillo cercano al obstáculo, escalado por un factor constante, reproduce la forma de ⟨J⟩(N) del TP4 y la J(N) del TP3. El scanning rate global es esencialmente el flujo de partículas frescas atravesando la cáscara cerca del obstáculo."

## Animaciones k (~30 s, 2 slides)

> "Mismo N = 500. k = 10² (blando): solapamientos profundos, contactos largos. k = 10⁴ (rígido): contactos breves, dinámica más cercana a EDMD. La constante elástica regula el tiempo de permanencia."

## Δt elegido por k (~30 s)

> "Criterio Δt ≤ τ_col/40, con τ_col = 2π√(m/k). Validado empíricamente con drift < 1% en 2000 s. Δt₂ común de 0,05 s para el muestreo, independiente del Δt de integración."

## ⟨J⟩ y ⟨J^in⟩ vs N — barrido en k (~30 s)

> "Cuatro curvas por observable, una por k. La forma cualitativa se mantiene: el N* del máximo se corre con k."

## Escalar característico vs k (~30 s)

> "Tomamos max_N ⟨J⟩(N,k) como escalar. Decrece levemente con k y satura — contacto más rígido reduce el tiempo de permanencia y por ende el flujo máximo. N* tiende a aumentar con k."
