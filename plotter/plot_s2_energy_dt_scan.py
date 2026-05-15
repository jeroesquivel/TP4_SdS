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

            t_arr = df["t"].to_numpy()
            rel_arr = rel.to_numpy()
            mask = np.isfinite(t_arr) & np.isfinite(rel_arr)
            if mask.sum() >= 2:
                slope, _ = np.polyfit(t_arr[mask], rel_arr[mask], 1)
            else:
                slope = np.nan

            max_drift_rows.append({
                "k": k, "dt": dt,
                "max_abs": float(np.abs(rel).max()),
                "slope": float(slope),
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

    fig.suptitle(r"Conservación de energía vs. $\Delta t$  —  N=800,  $t_f$=2000 s",
                 fontsize=14)

    out = FIGURES / "03d_s2_energy_dt_scan.png"
    fig.savefig(out)
    print("→", out)

    # =========== Fig 1b: single-panel k = 10^3 ===========
    K_FOCUS = 1000.0
    k_match = next((k for k in ks if np.isclose(k, K_FOCUS)), None)
    if k_match is not None:
        figk, axk = plt.subplots(figsize=(8.4, 5.0), constrained_layout=True)
        t_max_k = 0.0
        for dt, f in by_k[k_match]:
            df = pd.read_csv(f)
            e0 = df["e_total"].iloc[0]
            rel = (df["e_total"] - e0) / e0
            axk.plot(df["t"], rel, color=cmap(norm(dt)), lw=1.6, alpha=0.95)
            t_max_k = max(t_max_k, df["t"].max())
        axk.axhline(+DRIFT_TOL, color="0.35", lw=0.9, ls=":", alpha=0.75)
        axk.axhline(-DRIFT_TOL, color="0.35", lw=0.9, ls=":", alpha=0.75)
        axk.axhline(0.0, color="black", lw=0.7, alpha=0.4)
        axk.set_xlabel("t [s]")
        axk.set_ylabel(r"$\Delta E_{tot}/E_0$")
        axk.set_xlim(-100, t_max_k if t_max_k > 0 else 5.0)
        axk.set_ylim(-0.01, 0.2)
        axk.set_title(rf"Conservación de energía vs. $\Delta t$  —  k = {k_label(k_match)} N/m,  N=800,  $t_f$=2000 s")
        smk = ScalarMappable(norm=norm, cmap=cmap); smk.set_array([])
        cbk = figk.colorbar(smk, ax=axk, fraction=0.040, pad=0.018)
        cbk.set_label(r"$\Delta t$  [s]")
        cbk.set_ticks(all_dts)
        cbk.set_ticklabels([f"{dt:.0e}" for dt in all_dts])
        cbk.ax.minorticks_off()
        out_k = FIGURES / "03d_s2_energy_dt_scan_k1e3.png"
        figk.savefig(out_k)
        print("→", out_k)
    else:
        print(f"[warn] no hay corridas con k={K_FOCUS:g}; salteo 03d single-k")

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
    ax.set_title(r"Umbral de $\Delta t$ vs. k  —  N=800,  $t_f$=2000 s")
    ax.legend(loc="upper left", frameon=True, framealpha=0.95,
              handlelength=2.2)

    out2 = FIGURES / "03e_s2_energy_dt_max.png"
    fig2.savefig(out2)
    print("→", out2)

    # =========== Fig 3: |Se| vs Δt, una curva por k ===========
    # Se = pendiente de (ΔE_tot/E_0) vs t (regresión lineal), en s^{-1}.
    fig3, ax3 = plt.subplots(figsize=(8.4, 5.0))

    for k in ks:
        sub = md[md["k"] == k].sort_values("dt")
        y = sub["slope"].abs().replace([np.inf, -np.inf], np.nan)
        ax3.loglog(sub["dt"], y, "o-",
                   color=cmap_k(norm_k(k)), lw=1.8, markersize=7,
                   markerfacecolor="white", markeredgewidth=1.5,
                   label=f"k = {k_label(k)} N/m")

    ax3.set_xlabel(r"$\Delta t$  [s]")
    ax3.set_ylabel(r"$|S_e|$  [s$^{-1}$]")
    ax3.set_title(r"Pendiente del error $S_e$ vs. $\Delta t$  —  N=800,  $t_f$=2000 s")
    ax3.legend(loc="upper left", frameon=True, framealpha=0.95,
               handlelength=2.2)

    out3 = FIGURES / "03f_s2_energy_dt_slope.png"
    fig3.savefig(out3)
    print("→", out3)

    # =========== Fig 3b: |Se| vs Δt, solo k = 10^3 ===========
    if k_match is not None:
        sub = md[md["k"] == k_match].sort_values("dt")
        y = sub["slope"].abs().replace([np.inf, -np.inf], np.nan)
        fig3k, ax3k = plt.subplots(figsize=(8.4, 5.0))
        ax3k.loglog(sub["dt"], y, "o-",
                    color=cmap_k(norm_k(k_match)), lw=1.8, markersize=7,
                    markerfacecolor="white", markeredgewidth=1.5,
                    label=f"k = {k_label(k_match)} N/m")
        ax3k.set_xlabel(r"$\Delta t$  [s]")
        ax3k.set_ylabel(r"$|S_e|$  [s$^{-1}$]")
        ax3k.set_title(rf"Pendiente del error $S_e$ vs. $\Delta t$  —  k = {k_label(k_match)} N/m,  N=800,  $t_f$=2000 s")
        ax3k.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.2)
        out3k = FIGURES / "03f_s2_energy_dt_slope_k1e3.png"
        fig3k.savefig(out3k)
        print("→", out3k)
    else:
        print(f"[warn] no hay corridas con k={K_FOCUS:g}; salteo 03f single-k")


if __name__ == "__main__":
    main()
