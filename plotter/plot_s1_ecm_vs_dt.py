import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES

INTEGRATORS = [
    ("euler",  "Euler PC",          "#d62728", "o"),
    ("verlet", "Verlet original",   "#2ca02c", "s"),
    ("beeman", "Beeman PC",         "#1f77b4", "^"),
    ("gear5",  "Gear PC orden 5",   "#9467bd", "D"),
]


def fit_slope(x, y):
    lx = np.log10(x); ly = np.log10(y)
    slope, _ = np.polyfit(lx, ly, 1)
    return slope


def decreasing_window(x, y):
    keep = [0]
    for i in range(1, len(y)):
        if y[i] < y[i - 1]:
            keep.append(i)
        else:
            break
    return keep


def main():
    df = pd.read_csv(RESULTS / "s1" / "ecm_sweep.csv")
    fig, ax = plt.subplots(figsize=(8.6, 5.4))

    floor_x = []
    floor_y = []
    for key, label, color, marker in INTEGRATORS:
        sub = df[df["integrator"] == key].sort_values("dt", ascending=False)
        if sub.empty: continue
        x = sub["dt"].to_numpy()
        y = sub["ecm"].to_numpy()
        idx = decreasing_window(x, y)

        if len(idx) >= 2:
            slope = fit_slope(x[idx], y[idx])
            label_full = f"{label}  (m≈{slope:.2f})"
        else:
            label_full = label

        # Tramo descendente con línea sólida
        ax.loglog(x[idx], y[idx], color=color, marker=marker, markersize=8,
                  lw=2.0, label=label_full)

        # Tramo del piso (si lo hay) con línea punteada para indicar redondeo
        if len(idx) < len(x):
            tail_x = np.concatenate([[x[idx[-1]]], x[idx[-1] + 1:]])
            tail_y = np.concatenate([[y[idx[-1]]], y[idx[-1] + 1:]])
            ax.loglog(tail_x, tail_y, color=color, marker=marker, markersize=7,
                      lw=1.4, ls=":", alpha=0.6)
            floor_x.extend(x[idx[-1] + 1:])
            floor_y.extend(y[idx[-1] + 1:])

    if floor_x:
        ax.annotate(
            "piso de redondeo\n(doble precisión)",
            xy=(min(floor_x), min(floor_y)),
            xytext=(0.18, 0.08), textcoords="axes fraction",
            fontsize=10, color="dimgray",
            arrowprops=dict(arrowstyle="->", color="dimgray", lw=0.9))

    ax.set_xlabel(r"$\Delta t$ [s]")
    ax.set_ylabel(r"ECM  $[\mathrm{m}^2]$")
    ax.invert_xaxis()
    ax.legend(loc="upper right", frameon=True, framealpha=0.95,
              handlelength=2.2)

    out = FIGURES / "02_s1_ecm_vs_dt.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
