"""Perfiles radiales (Sistema 2 — ítem 1.3).

Genera dos figuras:
  · 07_s2_radial.png — J^in(S) para todos los N, panel completo + zoom S ∈ [2,5] m.
  · 08_s2_radial_near.png — capa cercana al obstáculo S ∈ [1.5, 3] m vs. N:
      - eje izquierdo: ⟨J^in⟩ (azul)
      - eje izquierdo (segunda curva, misma escala): ⟨ρ^{f,in}⟩ (verde)
      - eje derecho: |⟨v^{f,in}⟩| (rojo)
    Doble eje y, según corrección explícita de TP3 D28.
"""
import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import Normalize
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP, plain_n_colorbar, plain_log_axis

get_cmap = plt.get_cmap

S_NEAR_LO = 1.5
S_NEAR_HI = 3.0


def parse_n(name: str) -> int:
    m = re.search(r"radial_N(\d+)\.csv", name)
    return int(m.group(1)) if m else -1


def near_layer_avg(df, lo=S_NEAR_LO, hi=S_NEAR_HI):
    """Promedia los bins en S ∈ [lo, hi] ponderando por n_samples.

    Devuelve (rho_mean, rho_std, v_mean, v_std, j_mean, j_std). Si el CSV
    no tiene columnas std, devuelve 0 para las std.
    """
    mask = (df["S"] >= lo) & (df["S"] <= hi)
    sub = df[mask]
    if sub.empty:
        return tuple([np.nan] * 6)
    w = sub["n_samples"].to_numpy()
    if w.sum() == 0:
        return tuple([np.nan] * 6)
    rho = np.average(sub["rho"], weights=w)
    v = np.average(sub["v_in"], weights=w)
    j = np.average(sub["j_in"], weights=w)
    # std promediado: si los bins son ~independientes, std del promedio es
    # proporcional a sqrt(mean(std²)/N_bins). Usamos eso como cota razonable.
    def avg_std(col):
        if col not in sub.columns:
            return 0.0
        s2 = (sub[col].to_numpy() ** 2 * w).sum() / w.sum()
        return float(np.sqrt(s2) / np.sqrt(len(sub)))
    return rho, avg_std("rho_std"), v, avg_std("v_in_std"), j, avg_std("j_in_std")


def main():
    s2 = RESULTS / "s2"
    files = sorted(s2.glob("radial_N*.csv"), key=lambda f: parse_n(f.name))
    if not files:
        print("no radial CSVs"); return

    Ns = [parse_n(f.name) for f in files]
    cmap = get_cmap(GRADIENT_CMAP)
    norm = Normalize(vmin=min(Ns), vmax=max(Ns))

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
    axes[0].set_xlim(2.0, s_max)
    axes[0].set_ylim(0, j_max_full * 1.06)

    axes[1].set_xlabel("S [m]")
    axes[1].set_ylabel(r"$J^{in}(S)$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_xlim(2.0, 5.0)
    axes[1].set_ylim(0, j_max_zoom * 1.06)

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("N")
    plain_n_colorbar(cbar, Ns)

    out = FIGURES / "07_s2_radial.png"
    fig.savefig(out)
    print("→", out)

    # =========== Figura 2: capa cercana vs N (3 paneles, con barras de error) ===========
    rows = []
    for f, N in zip(files, Ns):
        df = pd.read_csv(f).sort_values("S")
        rho, rho_e, v, v_e, j, j_e = near_layer_avg(df)
        rows.append({"N": N, "rho": rho, "rho_e": rho_e,
                     "v": v, "v_e": v_e, "j": j, "j_e": j_e})
    near = pd.DataFrame(rows).sort_values("N").dropna()
    Ns_sorted = sorted(near["N"])

    # Tres paneles horizontales — eje X (N) compartido. Cada observable
    # con su propio eje Y para que las barras de error no se superpongan.
    fig2, (ax_j, ax_rho, ax_v) = plt.subplots(
        1, 3, figsize=(15.0, 5.2), sharex=True,
        gridspec_kw={"wspace": 0.28})

    ax_j.errorbar(near["N"], near["j"], yerr=near["j_e"],
                  fmt="o-", color="#1f77b4", ecolor="#1f77b4", capsize=3, lw=2.0,
                  markersize=8, markerfacecolor="white", markeredgewidth=1.8)
    ax_j.set_ylabel(r"$\langle J^{in} \rangle$  [m$^{-1}$ s$^{-1}$]")
    ax_j.set_xlabel("N")

    ax_rho.errorbar(near["N"], near["rho"], yerr=near["rho_e"],
                    fmt="s--", color="#2ca02c", ecolor="#2ca02c", capsize=3, lw=2.0,
                    markersize=8, markerfacecolor="white", markeredgewidth=1.8)
    ax_rho.set_ylabel(r"$\langle \rho^{f,in} \rangle$  [m$^{-2}$]")
    ax_rho.set_xlabel("N")

    ax_v.errorbar(near["N"], near["v"], yerr=near["v_e"],
                  fmt="^:", color="#d62728", ecolor="#d62728", capsize=3, lw=2.0,
                  markersize=8, markerfacecolor="white", markeredgewidth=1.8)
    ax_v.set_ylabel(r"$|\langle v^{f,in} \rangle|$  [m/s]")
    ax_v.set_xlabel("N")

    for ax in (ax_j, ax_rho, ax_v):
        plain_log_axis(ax.xaxis, Ns_sorted)


    out2 = FIGURES / "08_s2_radial_near.png"
    fig2.savefig(out2)
    print("→", out2)


if __name__ == "__main__":
    main()
