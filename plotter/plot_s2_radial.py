import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP

get_cmap = plt.get_cmap

S_NEAR_LO = 1.5
S_NEAR_HI = 3.0


def parse_n(name: str) -> int:
    m = re.search(r"radial_N(\d+)\.csv", name)
    return int(m.group(1)) if m else -1


def near_layer_avg(df, lo=S_NEAR_LO, hi=S_NEAR_HI):
    """Promedia los bins en S ∈ [lo, hi] usando n_samples como peso (inversamente
    proporcional a la varianza muestral) — más robusto que un promedio simple
    cuando los bins lejos del obstáculo tienen muchas más muestras."""
    mask = (df["S"] >= lo) & (df["S"] <= hi)
    sub = df[mask]
    if sub.empty:
        return np.nan, np.nan, np.nan
    w = sub["n_samples"].to_numpy()
    if w.sum() == 0:
        return np.nan, np.nan, np.nan
    rho = np.average(sub["rho"], weights=w)
    v = np.average(sub["v_in"], weights=w)
    j = np.average(sub["j_in"], weights=w)
    return rho, v, j


def main():
    s2 = RESULTS / "s2"
    files = sorted(s2.glob("radial_N*.csv"), key=lambda f: parse_n(f.name))
    if not files:
        print("no radial CSVs"); return

    Ns = [parse_n(f.name) for f in files]
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(Ns), vmax=max(Ns))

    # =========== Figura 1: perfiles J^in(S) ===========
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
    axes[1].set_title(r"Detalle  $S \in [1.5,\,5]$ m  (régimen del obstáculo)")
    axes[1].set_xlim(1.5, 5.0)
    axes[1].set_ylim(0, j_max_zoom * 1.06)

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("N")

    fig.suptitle(r"Perfiles radiales — k=10$^3$ N/m, t $\geq$ t$_f$/2",
                 y=0.995, fontsize=14)

    out = FIGURES / "s2_radial.png"
    fig.savefig(out)
    print("→", out)

    # =========== Figura 2: capa cercana vs N (doble eje y) ===========
    rows = []
    for f, N in zip(files, Ns):
        df = pd.read_csv(f).sort_values("S")
        rho, v, j = near_layer_avg(df)
        rows.append({"N": N, "rho": rho, "v": v, "j": j})
    near = pd.DataFrame(rows).sort_values("N")

    fig2, ax = plt.subplots(figsize=(9.0, 5.4))
    ax.set_xscale("log")
    ax.set_xlabel("N")

    l1 = ax.plot(near["N"], near["j"], "o-", color="#1f77b4", lw=2.0, markersize=8,
                 label=r"$\langle J^{in} \rangle$  [m$^{-1}$ s$^{-1}$]")
    ax.set_ylabel(r"$\langle J^{in} \rangle$  [m$^{-1}$ s$^{-1}$]", color="#1f77b4")
    ax.tick_params(axis="y", labelcolor="#1f77b4")

    ax2 = ax.twinx()
    ax2.spines["top"].set_visible(False)
    l2 = ax2.plot(near["N"], near["rho"], "s--", color="#2ca02c", lw=1.6, markersize=7,
                  label=r"$\langle \rho^{f,in} \rangle$  [m$^{-2}$]")
    l3 = ax2.plot(near["N"], near["v"], "^:", color="#d62728", lw=1.6, markersize=7,
                  label=r"$|\langle v^{f,in} \rangle|$  [m/s]")
    ax2.set_ylabel(r"$\langle \rho \rangle$,  $|\langle v \rangle|$", color="0.2")

    ax.set_title(r"Capa cercana al obstáculo  $S\!\in\![{:.1f},\,{:.1f}]$ m  vs. N".format(
        S_NEAR_LO, S_NEAR_HI))

    lines = l1 + l2 + l3
    labels = [ln.get_label() for ln in lines]
    ax.legend(lines, labels, loc="upper left", frameon=True, framealpha=0.95,
              handlelength=2.2)

    out2 = FIGURES / "s2_radial_near.png"
    fig2.savefig(out2)
    print("→", out2)


if __name__ == "__main__":
    main()
