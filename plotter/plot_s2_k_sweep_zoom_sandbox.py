"""Sandbox: zoom de k_sweep en N ∈ [400, 600] usando los datos de un sandbox
aislado (results/<sandbox_name>/), sin tocar los plots principales.

Defaults compatibles con el sandbox original (results/s2_sandbox/)."""
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
    e = int(round(np.log10(k)))
    return rf"$10^{{{e}}}$"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--indir", default=str(RESULTS / "s2_sandbox"),
                        help="Directorio con k_sweep.csv (default: results/s2_sandbox)")
    parser.add_argument("--out", default=None,
                        help="Path del PNG de salida (default: auto desde label/M)")
    parser.add_argument("--label", default=None,
                        help="Etiqueta para título y nombre por defecto")
    args = parser.parse_args()

    sandbox_csv = Path(args.indir) / "k_sweep.csv"
    if not sandbox_csv.exists():
        raise SystemExit(f"No existe {sandbox_csv}.")
    df = pd.read_csv(sandbox_csv)
    df = df[(df["N"] >= ZOOM_LO) & (df["N"] <= ZOOM_HI)].sort_values(["k", "N"])
    if df.empty:
        raise SystemExit("Sandbox CSV vacío en la franja [400, 600].")
    M = int(df["realizations"].iloc[0])
    label = args.label or Path(args.indir).name
    ks = sorted(df["k"].unique())
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(ks), vmax=max(ks))

    fig, axes = plt.subplots(1, 2, figsize=(13.0, 5.0))

    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        c = cmap(norm(k))
        axes[0].errorbar(sub["N"], sub["J_mean"], yerr=sub["J_std"] / np.sqrt(M),
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

    axes[0].set_ylabel(r"$\langle J \rangle$  [partículas / s]  (barras: SEM)")
    axes[0].set_title(rf"{label}  M={M}  —  $\langle J \rangle (N, k)$")
    axes[1].set_ylabel(r"$\langle J^{in} | S\!\sim\!2 \rangle$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_title(rf"{label}  M={M}  —  $\langle J^{{in}} | S\!\sim\!2 \rangle (N, k)$")

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("k [N/m]")

    fig.suptitle(rf"{label} · zoom [400,600] · M={M} realizaciones",
                 y=0.995, fontsize=14)

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
    else:
        out = FIGURES / f"sandbox_11_s2_k_sweep_zoom_M{M}.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
