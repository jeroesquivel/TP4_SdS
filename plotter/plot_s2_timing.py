import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA, plain_log_axis


OVERLAP_MIN, OVERLAP_MAX = 100, 800  # rango común TP3 ↔ TP4


def fit_loglog(df, x_col, y_col, n_min, n_max):
    sub = df[(df[x_col] >= n_min) & (df[x_col] <= n_max)]
    slope, intercept = np.polyfit(np.log10(sub[x_col]), np.log10(sub[y_col]), 1)
    return slope, intercept, sub


def main():
    df = pd.read_csv(RESULTS / "s2" / "timing.csv").sort_values("N")
    df = df[df["N"] % 100 == 0]
    fig, ax = plt.subplots(figsize=(10.5, 5.6))

    # TP4 — ajuste sobre el rango común con TP3 (100, 800) para que las
    # pendientes sean comparables. Datos hasta N=1000 igualmente se grafican.
    slope4, intercept4, sub4 = fit_loglog(df, "N", "t_exec_s",
                                          OVERLAP_MIN, OVERLAP_MAX)
    label_tp4 = fr"TP4 (DM)  $\alpha = {slope4:.2f}$"
    ax.semilogy(df["N"], df["t_exec_s"], "o-", color="#1f77b4", lw=2.0,
                markersize=9, markerfacecolor="white", markeredgewidth=1.8,
                label=label_tp4)

    n_span4 = np.linspace(sub4["N"].min(), sub4["N"].max(), 50)
    ax.semilogy(n_span4, 10 ** (slope4 * np.log10(n_span4) + intercept4),
                ":", color="#1f77b4", lw=1.2, alpha=0.6)

    # TP3 — ajuste estrictamente sobre el overlap; los puntos chicos
    # (N<100) están dominados por warmup JIT y distorsionan la pendiente
    # global.
    tp3 = TP3_DATA / "timing.csv"
    d3 = None
    if tp3.exists():
        d3 = pd.read_csv(tp3).sort_values("N")
        overlap3 = d3[(d3["N"] >= OVERLAP_MIN) & (d3["N"] <= OVERLAP_MAX)]
        if len(overlap3) >= 2:
            slope3, intercept3 = np.polyfit(np.log10(overlap3["N"]),
                                            np.log10(overlap3["t_exec"]), 1)
            label_tp3 = fr"TP3 (EDMD)  $\alpha = {slope3:.2f}$"
            n_span3 = np.linspace(overlap3["N"].min(), overlap3["N"].max(), 50)
            ax.semilogy(n_span3,
                      10 ** (slope3 * np.log10(n_span3) + intercept3),
                      ":", color="#d62728", lw=1.2, alpha=0.6)
        else:
            label_tp3 = "TP3 (EDMD)"
        ax.semilogy(d3["N"], d3["t_exec"], "s--", color="#d62728", lw=1.6,
                    markersize=9, markerfacecolor="white", markeredgewidth=1.8,
                    label=label_tp3)

    ax.set_xlabel("N")
    ax.set_ylabel(r"$t_{exec}$  [s]")
    # Eje X lineal con ticks cada 100. El pendiente se conserva como
    # parámetro del fit log-log calculado más arriba.
    ax.set_xticks(list(range(0, 1100, 100)))
    ax.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.2)

    out = FIGURES / "04_s2_timing.png"
    fig.savefig(out)
    print("→", out)
    print(f"   TP4 slope (overlap): {slope4:.3f}")
    if d3 is not None and 'slope3' in locals():
        print(f"   TP3 slope (overlap): {slope3:.3f}")


if __name__ == "__main__":
    main()
