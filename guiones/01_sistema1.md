# Sistema 1 — Oscilador amortiguado (~1 min, 2 slides)

## Slide 1 — Trayectorias r(t) (~30 s)

> "Oscilador amortiguado con solución analítica conocida. Comparamos cuatro integradores: Euler PC, Verlet original, Beeman PC y Gear orden 5. A Δt = 10⁻³ s, Gear-5 y Beeman superponen la analítica perfectamente; Euler y Verlet ya muestran desviación visible."

## Slide 2 — ECM vs Δt (~30 s)

> "ECM en log-log para ver el orden de cada integrador. Euler PC pendiente 2 — orden 1. Verlet pendiente 2 también: la fuerza depende de la velocidad y Verlet original tiene v lageada, lo que baja su orden efectivo. Beeman PC pendiente 4 — orden 2 con predictor de v. Gear-5 con α₀ = 3/16 alcanza el piso numérico para Δt ≤ 10⁻³ s. Gear-5 minimiza el ECM."
