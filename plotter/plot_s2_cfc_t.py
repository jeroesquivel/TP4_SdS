import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP, plain_n_colorbar

get_cmap = plt.get_cmap

RE_CFC = re.compile(r"cfc_N(\d+)_real(\d+)\.csv")


def collect_by_n(s2):
    by_n = {}
    for f in sorted(s2.glob("cfc_N*_real*.csv")):
        m = RE_CFC.search(f.name)
        if not m: continue
        N = int(m.group(1))
        by_n.setdefault(N, []).append(f)
    return dict(sorted(by_n.items()))


def mean_over_realizations(files):
    """Devuelve t (común a partir del primer archivo) y la media + std de Cfc(t)
    sobre realizaciones, alineadas por índice (los CSVs son sub-muestreados al
    mismo dt2 por el motor, así que comparten el grid temporal)."""
    arrays = []
    t_ref = None
    for f in files:
        df = pd.read_csv(f)
        if t_ref is None:
            t_ref = df["t"].to_numpy()
        n = min(len(df), len(t_ref))
        arrays.append(df["cfc"].to_numpy()[:n])
    n_min = min(len(a) for a in arrays)
    arrays = np.vstack([a[:n_min] for a in arrays])
    return t_ref[:n_min], arrays.mean(axis=0), arrays.std(axis=0)


def main():
    s2 = RESULTS / "s2"
    by_n = collect_by_n(s2)
    if not by_n:
        print("no cfc CSVs"); return

    Ns = sorted(by_n.keys())
    cmap = get_cmap(GRADIENT_CMAP)
    norm = Normalize(vmin=min(Ns), vmax=max(Ns))

    fig, ax = plt.subplots(figsize=(9.4, 5.2))
    t_max = 0.0
    cfc_max = 0.0
    for N in Ns:
        t, mu, sd = mean_over_realizations(by_n[N])
        c = cmap(norm(N))
        ax.fill_between(t, mu - sd, mu + sd, color=c, alpha=0.18, lw=0)
        ax.plot(t, mu, color=c, lw=1.6)
        t_max = max(t_max, t.max())
        cfc_max = max(cfc_max, (mu + sd).max())

    # Ajuste J sobre toda la ventana [0, tf] — no se sombrea ninguna sub-banda.

    ax.set_xlabel("t [s]")
    ax.set_ylabel(r"$C_{fc}(t)$  (acumulado)")
    ax.set_xlim(0, t_max)
    ax.set_ylim(0, cfc_max * 1.05)
    ax.legend(loc="upper left", frameon=True, framealpha=0.92, handlelength=1.6)

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=ax, fraction=0.045, pad=0.015)
    cbar.set_label("N")
    plain_n_colorbar(cbar, Ns)

    out = FIGURES / "05_s2_cfc_t.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
