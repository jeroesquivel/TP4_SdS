import re
import sys
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm, Normalize
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP, plain_n_colorbar

get_cmap = plt.get_cmap

# Patrones:
#   energy_velocity_verlet_N100_k1e+02.csv  → validación corta de Δt (tf=5)
#   energy_k1e+02_N100.csv                  → ksweep, una realización por (k,N)
#   energy_N100_real0.csv                   → jvsn, una por (N, real)

RE_VAL = re.compile(r"energy_velocity_verlet_N(\d+)_k([\d.+\-eE]+)\.csv")
RE_KSW = re.compile(r"energy_k([\d.+\-eE]+)_N(\d+)\.csv")
RE_JVN = re.compile(r"energy_N(\d+)_real(\d+)\.csv")


def k_label(k):
    e = int(round(np.log10(k)))
    return rf"$10^{{{e}}}$"


def panel_validation(ax, files, cmap):
    """Panel 1: una corrida corta (tf=5) por k para validar que Δt conserva energía."""
    rows = []
    for f in files:
        m = RE_VAL.search(f.name)
        if m:
            rows.append((float(m.group(2)), int(m.group(1)), f))
    rows.sort(key=lambda r: r[0])
    if not rows:
        ax.text(0.5, 0.5, "sin CSVs de validación", ha="center", va="center",
                transform=ax.transAxes, color="dimgray")
        return None

    ks = sorted({r[0] for r in rows})
    norm = LogNorm(vmin=min(ks), vmax=max(ks)) if len(ks) >= 2 else None

    e_init = None
    t_max = 0.0
    for k, N, f in rows:
        df = pd.read_csv(f)
        if e_init is None:
            e_init = df["e_total"].iloc[0]
        rel = (df["e_total"] - e_init) / e_init
        c = cmap(norm(k)) if norm else "#1f77b4"
        ax.plot(df["t"], rel, color=c, lw=1.6, label=f"k = {k_label(k)} N/m")
        t_max = max(t_max, df["t"].max())

    ax.axhline(0.0, color="black", lw=0.8, alpha=0.4)
    ax.set_xlabel("t [s]")
    ax.set_ylabel(r"$\Delta E_{tot}/E_0$")
    ax.set_title(r"Validación de $\Delta t$  —  N=100,  $t_f$=5 s")
    ax.set_xlim(0, t_max)
    if norm is None:
        ax.legend(loc="best", frameon=True, framealpha=0.92)
    return norm


def panel_stability(ax, files, cmap):
    """Panel 2: estabilidad de E_tot durante el barrido jvsn (tf=500, k=10³)."""
    rows = []
    for f in files:
        m = RE_JVN.search(f.name)
        if m and int(m.group(2)) == 0:
            rows.append((int(m.group(1)), f))
    rows.sort(key=lambda r: r[0])
    if not rows:
        ax.text(0.5, 0.5, "sin CSVs de jvsn", ha="center", va="center",
                transform=ax.transAxes, color="dimgray")
        return None

    Ns = [r[0] for r in rows]
    norm = Normalize(vmin=min(Ns), vmax=max(Ns)) if len(Ns) >= 2 else None

    t_max = 0.0
    for N, f in rows:
        df = pd.read_csv(f)
        e_init = df["e_total"].iloc[0]
        rel = (df["e_total"] - e_init) / e_init
        c = cmap(norm(N)) if norm else "#1f77b4"
        ax.plot(df["t"], rel, color=c, lw=1.0, alpha=0.9)
        t_max = max(t_max, df["t"].max())

    ax.axhline(0.0, color="black", lw=0.8, alpha=0.4)
    ax.set_xlabel("t [s]")
    ax.set_ylabel(r"$\Delta E_{tot}/E_0$")
    ax.set_title(r"Estabilidad en jvsn  —  k=10$^3$ N/m,  $t_f$=500 s")
    ax.set_xlim(0, t_max)
    return norm


def panel_ksweep_long(ax, files, cmap):
    """Panel 3: estabilidad de E_tot a tf=500 s para cada (k, N=100) del ksweep."""
    rows = []
    for f in files:
        m = RE_KSW.search(f.name)
        if m and int(m.group(2)) == 100:
            rows.append((float(m.group(1)), int(m.group(2)), f))
    rows.sort(key=lambda r: r[0])
    if not rows:
        ax.text(0.5, 0.5, "sin CSVs de ksweep largo", ha="center", va="center",
                transform=ax.transAxes, color="dimgray")
        return None

    ks = sorted({r[0] for r in rows})
    norm = LogNorm(vmin=min(ks), vmax=max(ks)) if len(ks) >= 2 else None

    t_max = 0.0
    for k, N, f in rows:
        df = pd.read_csv(f)
        e_init = df["e_total"].iloc[0]
        rel = (df["e_total"] - e_init) / e_init
        c = cmap(norm(k)) if norm else "#1f77b4"
        ax.plot(df["t"], rel, color=c, lw=1.4, label=f"k = {k_label(k)} N/m")
        t_max = max(t_max, df["t"].max())

    ax.axhline(0.0, color="black", lw=0.8, alpha=0.4)
    ax.set_xlabel("t [s]")
    ax.set_ylabel(r"$\Delta E_{tot}/E_0$")
    ax.set_title(r"ksweep N=100  —  $t_f$=500 s")
    ax.set_xlim(0, t_max)
    if norm is None:
        ax.legend(loc="best", frameon=True, framealpha=0.92)
    return norm


def main(argv):
    s2 = RESULTS / "s2"
    files = list(s2.glob("energy_*.csv"))
    if not files:
        print("no energy CSVs in", s2); return

    cmap = get_cmap(GRADIENT_CMAP)

    fig, axes = plt.subplots(1, 3, figsize=(16.5, 4.6))
    norm_val = panel_validation(axes[0], files, cmap)
    norm_jvn = panel_stability(axes[1], files, cmap)
    norm_ksw = panel_ksweep_long(axes[2], files, cmap)

    # Una sola colorbar abajo (k) y otra (N) — para no saturar usamos texto
    if norm_val is not None:
        sm = ScalarMappable(norm=norm_val, cmap=cmap); sm.set_array([])
        cbar = fig.colorbar(sm, ax=axes[0], fraction=0.06, pad=0.02)
        cbar.set_label("k [N/m]")
    if norm_jvn is not None:
        sm = ScalarMappable(norm=norm_jvn, cmap=cmap); sm.set_array([])
        cbar = fig.colorbar(sm, ax=axes[1], fraction=0.06, pad=0.02)
        cbar.set_label("N")
        Ns_jvn = sorted({int(RE_JVN.search(f.name).group(1))
                         for f in files
                         if RE_JVN.search(f.name) and int(RE_JVN.search(f.name).group(2)) == 0})
        plain_n_colorbar(cbar, Ns_jvn)
    if norm_ksw is not None:
        sm = ScalarMappable(norm=norm_ksw, cmap=cmap); sm.set_array([])
        cbar = fig.colorbar(sm, ax=axes[2], fraction=0.06, pad=0.02)
        cbar.set_label("k [N/m]")

    fig.suptitle(r"Conservación de energía total  —  $\Delta E_{tot}/E_0$",
                 y=1.00, fontsize=14)

    fig.subplots_adjust(wspace=0.36)
    out = FIGURES / "03_s2_energy.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main(sys.argv)
