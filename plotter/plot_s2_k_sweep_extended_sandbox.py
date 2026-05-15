"""Combina main (k=100, 1000, 10000) + sandbox (k=500, 5000) y dibuja el
k_sweep en la franja [400, 600] con los 5 ks. Las cinco curvas comparten
el mismo rango N porque es el único donde todos tienen datos.

Lee:  results/s2/k_sweep.csv  +  <indir>/k_sweep.csv
Escribe: --out (o figures/sandbox_09_s2_k_sweep_extended.png)
"""
import argparse
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP

get_cmap = plt.get_cmap
ZOOM_LO, ZOOM_HI = 400, 600


def k_label(k):
    e = np.log10(k)
    if abs(e - round(e)) < 1e-3:
        return rf"$10^{{{int(round(e))}}}$"
    mantissa = k / 10 ** np.floor(e)
    return rf"${mantissa:.0f}\!\cdot\!10^{{{int(np.floor(e))}}}$"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--indir", default=str(RESULTS / "sb_kscalar"))
    parser.add_argument("--out", default=None)
    parser.add_argument("--label", default=None)
    args = parser.parse_args()

    main_csv = RESULTS / "s2" / "k_sweep.csv"
    sb_csv = Path(args.indir) / "k_sweep.csv"

    main_df = pd.read_csv(main_csv) if main_csv.exists() else pd.DataFrame()
    sb_df = pd.read_csv(sb_csv) if sb_csv.exists() else pd.DataFrame()
    if main_df.empty and sb_df.empty:
        raise SystemExit("No hay datos en main ni en sandbox.")
    if not sb_df.empty and not main_df.empty:
        sb_ks = set(sb_df["k"].unique())
        main_df = main_df[~main_df["k"].isin(sb_ks)]
    df = pd.concat([main_df, sb_df], ignore_index=True)
    df = df[(df["N"] >= ZOOM_LO) & (df["N"] <= ZOOM_HI)].sort_values(["k", "N"])

    ks = sorted(df["k"].unique())
    print(f"ks en el plot: {ks}")
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(ks), vmax=max(ks))

    fig, axes = plt.subplots(1, 2, figsize=(13.0, 5.0))

    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        c = cmap(norm(k))
        M_row = int(sub["realizations"].iloc[0])
        axes[0].errorbar(sub["N"], sub["J_mean"], yerr=sub["J_std"] / np.sqrt(M_row),
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
        ax.set_xticks(np.arange(ZOOM_LO, ZOOM_HI + 1, 50))

    axes[0].set_ylabel(r"$\langle J \rangle$  [partículas / s]  (barras: SEM)")
    axes[0].set_title(r"$\langle J \rangle (N, k)$  —  5 valores de $k$")
    axes[1].set_ylabel(r"$\langle J^{in} | S\!\sim\!2 \rangle$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_title(r"$\langle J^{in} | S\!\sim\!2 \rangle (N, k)$")

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("k [N/m]")
    cbar.set_ticks(ks)
    cbar.set_ticklabels([k_label(k) for k in ks])
    cbar.ax.minorticks_off()

    fig.suptitle(rf"k-sweep extendido en franja [{ZOOM_LO}, {ZOOM_HI}]  —  main + sandbox",
                 y=0.995, fontsize=14)

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
    else:
        out = FIGURES / "sandbox" / "ksweep_franja_extendido.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
