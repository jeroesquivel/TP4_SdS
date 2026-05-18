# Sistema 2 — Implementación (~45 s)

## Motor de simulación (~45 s)

> "Sembramos N partículas por rejection sampling sin solapamiento. Vecinos en Cell Index Method con celda de 2·r_max. El loop principal: calcular fuerzas O(N), integrar un paso de Velocity-Verlet 2D, detectar flancos de contacto para actualizar fresca↔usada, y acumular C_fc con resolución Δt. El estado se muestrea cada Δt₂ para los perfiles radiales. Usamos Velocity-Verlet por ser simpléctico — la fuerza acá solo depende de la posición."
