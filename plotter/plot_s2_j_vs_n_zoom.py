"""Zoom de ⟨J⟩(N) en N ∈ [400, 600] usando todos los puntos."""
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA

ZOOM_LO, ZOOM_HI = 400, 600


def main():
    df = pd.read_csv(RESULTS / "s2" / "j_vs_n.csv").sort_values("N")
    df = df[(df["N"] >= ZOOM_LO) & (df["N"] <= ZOOM_HI)]
    fig, ax = plt.subplots(figsize=(8.4, 5.0))

    ax.errorbar(df["N"], df["J_mean"], yerr=df["J_std"],
                fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                capsize=4, lw=1.8, markersize=8, markerfacecolor="white",
                markeredgewidth=1.8,
                label=r"TP4 (DM, k=10$^3$ N/m, M=10)")

    tp3 = TP3_DATA / "j_vs_n.csv"
    if tp3.exists():
        d3 = pd.read_csv(tp3).sort_values("N")
        d3 = d3[(d3["N"] >= ZOOM_LO) & (d3["N"] <= ZOOM_HI)]
        if not d3.empty:
            yerr3 = d3.get("J_std", pd.Series([0] * len(d3)))
            ax.errorbar(d3["N"], d3["J_mean"], yerr=yerr3,
                        fmt="s--", color="#d62728", ecolor="#d62728",
                        capsize=4, lw=1.6, markersize=8, markerfacecolor="white",
                        markeredgewidth=1.8, label="TP3 (EDMD)")

    ax.set_xlabel("N")
    ax.set_ylabel(r"$\langle J \rangle$  [partículas / s]")
    ax.set_title(r"Zoom  $\langle J \rangle$  vs.  N  —  $N \in [400, 600]$")
    ax.set_xlim(ZOOM_LO - 15, ZOOM_HI + 15)
    ax.set_xticks(np.arange(ZOOM_LO, ZOOM_HI + 1, 25))
    ax.margins(y=0.10)
    ax.legend(loc="lower center", frameon=True, framealpha=0.95, handlelength=2.2)

    out = FIGURES / "12_s2_j_vs_n_zoom.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
