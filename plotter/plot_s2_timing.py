import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA


def main():
    df = pd.read_csv(RESULTS / "s2" / "timing.csv").sort_values("N")
    fig, ax = plt.subplots(figsize=(8.4, 5.2))

    # TP4
    slope, intercept = np.polyfit(np.log10(df["N"]), np.log10(df["t_exec_s"]), 1)
    label_tp4 = f"TP4 (DM, Velocity-Verlet)   m = {slope:.2f}"
    ax.loglog(df["N"], df["t_exec_s"], "o-", color="#1f77b4", lw=2.0,
              markersize=9, markerfacecolor="white", markeredgewidth=1.8,
              label=label_tp4)

    # Línea de ajuste (extendida hasta el rango completo de N)
    n_span = np.array([df["N"].min(), df["N"].max()])
    ax.loglog(n_span, 10 ** (slope * np.log10(n_span) + intercept),
              ":", color="#1f77b4", lw=1.2, alpha=0.6)

    # TP3
    tp3 = TP3_DATA / "timing.csv"
    if tp3.exists():
        d3 = pd.read_csv(tp3).sort_values("N")
        if len(d3) >= 2:
            slope3, _ = np.polyfit(np.log10(d3["N"]), np.log10(d3["t_exec"]), 1)
            label_tp3 = f"TP3 (EDMD)   m = {slope3:.2f}"
        else:
            label_tp3 = "TP3 (EDMD)"
        ax.loglog(d3["N"], d3["t_exec"], "s--", color="#d62728", lw=1.6,
                  markersize=9, markerfacecolor="white", markeredgewidth=1.8,
                  label=label_tp3)

    ax.set_xlabel("N")
    ax.set_ylabel(r"$t_{exec}$  [s]")
    ax.set_title(r"Tiempo de ejecución vs. N  —  t$_f$=500 s,  k=10$^3$ N/m")
    ax.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.2)

    out = FIGURES / "s2_timing.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
