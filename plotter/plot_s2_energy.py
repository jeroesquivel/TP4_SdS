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


def _save_panel(panel_fn, files, cmap, out_path, cbar_label, ns_for_plain=None):
    fig, ax = plt.subplots(figsize=(7.2, 4.8))
    norm = panel_fn(ax, files, cmap)
    if norm is not None:
        sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
        cbar = fig.colorbar(sm, ax=ax, fraction=0.06, pad=0.02)
        cbar.set_label(cbar_label)
        if ns_for_plain is not None:
            plain_n_colorbar(cbar, ns_for_plain)
    fig.tight_layout()
    fig.savefig(out_path)
    print("→", out_path)


def main(argv):
    s2 = RESULTS / "s2"
    files = list(s2.glob("energy_*.csv"))
    if not files:
        print("no energy CSVs in", s2); return

    cmap = get_cmap(GRADIENT_CMAP)

    Ns_jvn = sorted({int(RE_JVN.search(f.name).group(1))
                     for f in files
                     if RE_JVN.search(f.name) and int(RE_JVN.search(f.name).group(2)) == 0})

    _save_panel(panel_validation,   files, cmap, FIGURES / "03a_s2_energy_validation.png", "k [N/m]")
    _save_panel(panel_stability,    files, cmap, FIGURES / "03b_s2_energy_jvsn.png",       "N", ns_for_plain=Ns_jvn)
    _save_panel(panel_ksweep_long,  files, cmap, FIGURES / "03c_s2_energy_ksweep.png",     "k [N/m]")


if __name__ == "__main__":
    main(sys.argv)
