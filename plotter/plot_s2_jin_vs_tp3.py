"""Comparación J^in(N) TP4 (anillo cercano al obstáculo) vs J(N) TP3 y TP4.

Responde a la corrección explícita del TP3: "comparar el scanning rate con
J^in en anillos pequeños". Tres curvas en el mismo plot:
  · J(N) TP3 (EDMD, leído de tp3_data/j_vs_n.csv)
  · J(N) TP4 (DM time-driven, leído de results/s2/j_vs_n.csv)
  · J^in(N) TP4 en S ∈ [1.5, 3.0] m (leído de results/s2/radial_N*.csv)

Para que las escalas se vean en el mismo eje, J^in se grafica con un factor
de escala constante (el ratio promedio J/J^in del TP4).
"""
import re
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA, plain_log_axis

S_NEAR_LO, S_NEAR_HI = 1.5, 3.0


def parse_n(name: str) -> int:
    m = re.search(r"radial_N(\d+)\.csv", name)
    return int(m.group(1)) if m else -1


def jin_near(radial_csv: Path) -> float:
    """Promedio ponderado por n_samples de J^in en S ∈ [S_NEAR_LO, S_NEAR_HI]."""
    df = pd.read_csv(radial_csv)
    mask = (df["S"] >= S_NEAR_LO) & (df["S"] <= S_NEAR_HI)
    sub = df[mask]
    if sub.empty or sub["n_samples"].sum() == 0:
        return np.nan
    return float(np.average(sub["j_in"], weights=sub["n_samples"]))


def main():
    # --- TP4 J global ---
    tp4_j = pd.read_csv(RESULTS / "s2" / "j_vs_n.csv").sort_values("N")
    tp4_j = tp4_j[tp4_j["N"] % 100 == 0]

    # --- TP4 J^in en anillo cercano ---
    radial_files = sorted((RESULTS / "s2").glob("radial_N*.csv"),
                          key=lambda f: parse_n(f.name))
    rows = []
    for f in radial_files:
        N = parse_n(f.name)
        if N < 0:
            continue
        rows.append({"N": N, "J_in": jin_near(f)})
    tp4_jin = pd.DataFrame(rows).sort_values("N").dropna()

    # --- TP3 J ---
    tp3 = TP3_DATA / "j_vs_n.csv"
    if not tp3.exists():
        raise SystemExit(f"Falta {tp3}. Corré primero: python3 scripts/build_tp3_data.py")
    tp3_j = pd.read_csv(tp3).sort_values("N")

    # Conservación de flujo radial estacionario:
    #   J_scan (particulas/s)  ==  J^in(S) * 2 pi S   (en cualquier anillo S).
    # Usamos S_REF = centro del anillo cercano al obstáculo para convertir
    # J^in (TP4) → particulas/s y compararlo en el mismo eje que J de TP3/TP4.
    S_REF = (S_NEAR_LO + S_NEAR_HI) / 2.0
    scale = 2.0 * np.pi * S_REF
    print(f"factor físico 2*pi*S (S={S_REF:.2f}) = {scale:.2f}")

    # =========== Figura ===========
    fig, ax = plt.subplots(figsize=(9.0, 5.4))

    ax.errorbar(tp3_j["N"], tp3_j["J_mean"], yerr=tp3_j.get("J_std", 0),
                fmt="s--", color="#d62728", ecolor="#d62728",
                capsize=4, lw=1.6, markersize=8, markerfacecolor="white",
                markeredgewidth=1.8, label="EDMD $J$ (scanning rate)")

    ax.errorbar(tp4_j["N"], tp4_j["J_mean"], yerr=tp4_j["J_std"],
                fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                capsize=4, lw=1.8, markersize=8, markerfacecolor="white",
                markeredgewidth=1.8, label="DM $J$ (scanning rate)")

    ax.plot(tp4_jin["N"], scale * tp4_jin["J_in"], "^:", color="#2ca02c",
            lw=1.8, markersize=9, markerfacecolor="white", markeredgewidth=1.8,
            label=rf"DM $J^{{in}}\!\cdot\!2\pi S$ ($S={S_REF:.2f}$ m)")

    ax.set_xlabel("N")
    ax.set_ylabel(r"$J$  [partículas / s]")
    plain_log_axis(ax.xaxis, sorted(set(list(tp4_j["N"]) + list(tp3_j["N"]) + list(tp4_jin["N"]))))
    ax.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.6)
    ax.margins(y=0.10)

    out = FIGURES / "13_s2_jin_vs_tp3.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
