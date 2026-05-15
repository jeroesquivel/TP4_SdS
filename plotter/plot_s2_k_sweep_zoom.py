"""Zoom de k_sweep en la franja crítica N ∈ [400, 600] usando todos los puntos
(incluye los Ns intermedios de a 25)."""
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP

get_cmap = plt.get_cmap

ZOOM_LO, ZOOM_HI = 400, 600


def k_label(k):
    e = int(round(np.log10(k)))
    return rf"$10^{{{e}}}$"


def main():
    df = pd.read_csv(RESULTS / "s2" / "k_sweep.csv")
    df = df[(df["N"] >= ZOOM_LO) & (df["N"] <= ZOOM_HI)].sort_values(["k", "N"])
    ks = sorted(df["k"].unique())
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(ks), vmax=max(ks))

    # =========== Figura: ⟨J⟩(N,k) y ⟨J^in|S~2⟩(N,k) — ZOOM ===========
    fig, axes = plt.subplots(1, 2, figsize=(13.0, 5.0))

    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        c = cmap(norm(k))
        axes[0].errorbar(sub["N"], sub["J_mean"], yerr=sub["J_std"],
                         fmt="o-", color=c, ecolor=c, capsize=4, lw=1.8,
                         markersize=7, markerfacecolor="white",
                         markeredgewidth=1.6,
                         label=f"k = {k_label(k)} N/m")
        axes[1].plot(sub["N"], sub["J_in_S2_mean"], "s-", color=c, lw=1.8,
                     markersize=7, markerfacecolor="white", markeredgewidth=1.6)

    for ax in axes:
        ax.set_xlabel("N")
        ax.set_xlim(ZOOM_LO - 15, ZOOM_HI + 15)
        ax.margins(y=0.10)
        ax.set_xticks(np.arange(ZOOM_LO, ZOOM_HI + 1, 25))

    axes[0].set_ylabel(r"$\langle J \rangle$  [partículas / s]")
    axes[0].set_title(r"Zoom  $\langle J \rangle (N, k)$  —  $N \in [400,600]$")
    axes[1].set_ylabel(r"$\langle J^{in} | S\!\sim\!2 \rangle$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_title(r"Zoom  $\langle J^{in} | S\!\sim\!2 \rangle (N, k)$")

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("k [N/m]")

    fig.suptitle(r"Detalle franja crítica $N \in [400, 600]$  —  grilla cada 25,  M=10",
                 y=0.995, fontsize=14)

    out = FIGURES / "11_s2_k_sweep_zoom.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
