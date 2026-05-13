import re
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP

get_cmap = plt.get_cmap

RE = re.compile(r"energy_dtscan_k([\d.+\-eE]+)_dt([\d.+\-eE]+)\.csv")

DRIFT_TOL = 1e-2
Y_CLIP = 8e-2


def k_label(k):
    e = int(round(np.log10(k)))
    return rf"$10^{{{e}}}$"


def dt_label(dt):
    e = int(np.floor(np.log10(dt)))
    mant = dt / 10 ** e
    if abs(mant - 1.0) < 1e-9:
        return rf"$10^{{{e}}}$"
    return rf"${mant:g}\!\times\!10^{{{e}}}$"


def discover(s2):
    by_k = {}
    for f in sorted(s2.glob("energy_dtscan_*.csv")):
        m = RE.search(f.name)
        if not m:
            continue
        k = float(m.group(1))
        dt = float(m.group(2))
        by_k.setdefault(k, []).append((dt, f))
    return {k: sorted(v) for k, v in sorted(by_k.items())}


def main():
    s2 = RESULTS / "s2"
    by_k = discover(s2)
    if not by_k:
        print("no energy_dtscan CSVs in", s2)
        return

    all_dts = sorted({dt for v in by_k.values() for (dt, _) in v})
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(all_dts), vmax=max(all_dts))

    # =========== Fig 1: una panel por k ===========
    ks = sorted(by_k.keys())
    n = len(ks)
    cols = 2 if n > 1 else 1
    rows = int(np.ceil(n / cols))
    fig, axs = plt.subplots(rows, cols, figsize=(6.8 * cols, 4.2 * rows),
                            squeeze=False, sharex=True, sharey=False,
                            constrained_layout=True)
    axs_flat = axs.ravel()

    max_drift_rows = []
    for ax, k in zip(axs_flat, ks):
        t_max = 0.0
        for dt, f in by_k[k]:
            df = pd.read_csv(f)
            e0 = df["e_total"].iloc[0]
            rel = (df["e_total"] - e0) / e0
            c = cmap(norm(dt))
            ax.plot(df["t"], rel, color=c, lw=1.4, alpha=0.95)
            t_max = max(t_max, df["t"].max())
            max_drift_rows.append({
                "k": k, "dt": dt,
                "max_abs": float(np.abs(rel).max()),
            })

        ax.axhline(+DRIFT_TOL, color="0.35", lw=0.9, ls=":", alpha=0.75)
        ax.axhline(-DRIFT_TOL, color="0.35", lw=0.9, ls=":", alpha=0.75)
        ax.axhline(0.0, color="black", lw=0.7, alpha=0.4)
        ax.set_xlabel("t [s]")
        ax.set_ylabel(r"$\Delta E_{tot}/E_0$")
        ax.set_title(rf"k = {k_label(k)} N/m")
        ax.set_xlim(-100, t_max if t_max > 0 else 5.0) #el primer valor es de donde arranca x
        ax.set_ylim(-0.01, 0.2) #cambiar esto para la escala en y

    # esconder paneles sobrantes (cuando n no es múltiplo de cols)
    visible_axes = list(axs_flat[:n])
    for ax in axs_flat[n:]:
        ax.set_visible(False)
    # con sharex/sharey, ocultar labels y ticks redundantes de paneles internos
    for ax in visible_axes:
        ax.label_outer()

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cb = fig.colorbar(sm, ax=visible_axes,
                      fraction=0.040, pad=0.018)
    cb.set_label(r"$\Delta t$  [s]")
    cb.set_ticks(all_dts)
    cb.set_ticklabels([f"{dt:.0e}" for dt in all_dts])
    cb.ax.minorticks_off()

    fig.suptitle(r"Conservación de energía vs. $\Delta t$  —  N=100,  $t_f$=2000 s",
                 fontsize=14)

    out = FIGURES / "03d_s2_energy_dt_scan.png"
    fig.savefig(out)
    print("→", out)

    # =========== Fig 2: max|ΔE/E_0| vs Δt, una curva por k ===========
    md = pd.DataFrame(max_drift_rows).sort_values(["k", "dt"])

    fig2, ax = plt.subplots(figsize=(8.4, 5.0))
    cmap_k = get_cmap(GRADIENT_CMAP)
    norm_k = LogNorm(vmin=min(ks), vmax=max(ks))

    for k in ks:
        sub = md[md["k"] == k].sort_values("dt")
        # las divergencias quedan como inf; nos protegemos para el log-log
        y = sub["max_abs"].replace([np.inf, -np.inf], np.nan)
        ax.loglog(sub["dt"], y, "o-",
                  color=cmap_k(norm_k(k)), lw=1.8, markersize=7,
                  markerfacecolor="white", markeredgewidth=1.5,
                  label=f"k = {k_label(k)} N/m")

    ax.set_ylim(1e-6, 10)

    ax.axhline(DRIFT_TOL, color="0.35", lw=1.0, ls="--", alpha=0.85)
    ax.text(min(all_dts), DRIFT_TOL * 1.15, " 1 %  (umbral)",
            color="0.35", fontsize=10, va="bottom")

    ax.set_xlabel(r"$\Delta t$  [s]")
    ax.set_ylabel(r"$\max_t\,|\Delta E_{tot}/E_0|$")
    ax.set_title(r"Umbral de $\Delta t$ vs. k  —  N=100,  $t_f$=2000 s")
    ax.legend(loc="upper left", frameon=True, framealpha=0.95,
              handlelength=2.2)

    out2 = FIGURES / "03e_s2_energy_dt_max.png"
    fig2.savefig(out2)
    print("→", out2)


if __name__ == "__main__":
    main()
