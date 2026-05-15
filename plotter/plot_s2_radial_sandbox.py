"""Sandbox: reproduce el plot 07_s2_radial leyendo los radial_N*.csv de un
sandbox arbitrario. Útil para comparar la versión "t>=tf/2" (main) con una
versión "todo el tiempo" (sandbox con --tmin 0)."""
import argparse
import re
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP, plain_n_colorbar

get_cmap = plt.get_cmap

S_NEAR_LO = 1.5
S_NEAR_HI = 3.0


def parse_n(name: str) -> int:
    m = re.search(r"radial_N(\d+)\.csv", name)
    return int(m.group(1)) if m else -1


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--indir", default=str(RESULTS / "sb_radial_t0"))
    parser.add_argument("--out", default=None)
    parser.add_argument("--label", default=None,
                        help="texto del subtítulo (ej: 't ∈ [0, t_f]')")
    args = parser.parse_args()

    indir = Path(args.indir)
    files = sorted(indir.glob("radial_N*.csv"), key=lambda f: parse_n(f.name))
    if not files:
        raise SystemExit(f"No hay radial_N*.csv en {indir}")

    Ns = [parse_n(f.name) for f in files]
    cmap = get_cmap(GRADIENT_CMAP)
    norm = Normalize(vmin=min(Ns), vmax=max(Ns))
    label = args.label or f"sandbox: {indir.name}"

    fig, axes = plt.subplots(1, 2, figsize=(13.0, 5.0))

    s_max = 0.0
    j_max_full = 0.0
    j_max_zoom = 0.0
    for f, N in zip(files, Ns):
        df = pd.read_csv(f).sort_values("S")
        c = cmap(norm(N))
        axes[0].plot(df["S"], df["j_in"], color=c, lw=1.3, alpha=0.95)
        zoom = df[(df["S"] >= 1.5) & (df["S"] <= 5.0)]
        axes[1].plot(zoom["S"], zoom["j_in"], color=c, lw=1.8, alpha=0.95)
        s_max = max(s_max, df["S"].max())
        j_max_full = max(j_max_full, df["j_in"].max())
        j_max_zoom = max(j_max_zoom, zoom["j_in"].max())

    axes[0].axvspan(S_NEAR_LO, S_NEAR_HI, color="0.85", alpha=0.45, zorder=0)
    axes[0].set_xlabel("S [m]")
    axes[0].set_ylabel(r"$J^{in}(S)$  [m$^{-1}$ s$^{-1}$]")
    axes[0].set_title("Perfil radial completo")
    axes[0].set_xlim(2.0, s_max)
    axes[0].set_ylim(0, j_max_full * 1.06)

    axes[1].set_xlabel("S [m]")
    axes[1].set_ylabel(r"$J^{in}(S)$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_title(r"Detalle  $S \in [2,\,5]$ m  (régimen del obstáculo)")
    axes[1].set_xlim(2.0, 5.0)
    axes[1].set_ylim(0, j_max_zoom * 1.06)

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("N")
    plain_n_colorbar(cbar, Ns)

    fig.suptitle(rf"Perfiles radiales — k=10$^3$ N/m,  {label}",
                 y=0.995, fontsize=14)

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
    else:
        out = FIGURES / "sandbox" / f"radial_{indir.name}.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
