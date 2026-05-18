# Tips para defender en vivo

- **Si te preguntan por qué Verlet da pendiente 2 en lugar de 4**: "porque la fuerza depende de v y Verlet evalúa f con la v lageada del paso anterior, lo que baja el orden efectivo a 1".

- **Si preguntan por qué EDMD escala peor**: "priority queue O(N²) potenciales eventos, cada extracción log; sin lazy invalidation las invalidaciones se acumulan y el exponente crece a partir de cierto N".

- **Si preguntan por el α₀ = 3/16**: "es el coeficiente de Gear-5 para fuerzas que dependen de la velocidad; el caso F(r) usa 3/20".

- **Si preguntan por la elección de Velocity-Verlet en Sistema 2**: "F = -k·ξ·ê depende solo de la posición. Velocity-Verlet es simpléctico, conserva energía a largo plazo, y es simple. Gear sería overkill y sufre con la discontinuidad de derivada de F en ξ = 0".

- **Si preguntan por el cruce N ≈ 500**: "es el N donde el costo lineal de DM (CIM + integración fija) iguala el costo super-cúbico de EDMD por evento. Para N < 500 EDMD gana; para N > 500 DM gana y la ventaja crece".

- **Si preguntan por la conservación de energía**: "graficamos ΔE_tot/E_0 vs t para varios Δt. Pendiente del drift S_e como métrica. Para k = 10³ con Δt = 10⁻³ s el drift es < 0,1% en 2000 s — válido para t_f = 500 s con margen".

- **Si preguntan por el primer Δt del contacto**: "mantenemos un flag por partícula. Si ξ > 0 y antes era ξ ≤ 0, contamos la transición. Esto evita contar muchas veces el mismo contacto que dura múltiples Δt".

- **Si preguntan por la diferencia con TP3**: "TP3 usa EDMD — colisión instantánea con cambio de velocidad por la ley de choque elástico. TP4 usa DM por paso temporal — el contacto dura varios Δt y se modela como fuerza. Los observables (C_fc, J) están definidos para que sean directamente comparables".

- **Sobre |⟨v⟩| vs N**: con M=100 la curva es suave y monótonamente creciente hasta N ≈ 800. El dip aparente en N=500 que se veía con M=10 era ruido estadístico — se eliminó al subir M.
