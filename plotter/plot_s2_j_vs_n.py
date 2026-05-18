import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA, plain_log_axis


def main():
    df = pd.read_csv(RESULTS / "s2" / "j_vs_n.csv").sort_values("N")
    df = df[df["N"] % 100 == 0]
    fig, ax = plt.subplots(figsize=(8.4, 5.0))

    ax.errorbar(df["N"], df["J_mean"], yerr=df["J_std"],
                fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                capsize=4, lw=1.8, markersize=7, markerfacecolor="white",
                markeredgewidth=1.8, label="TP4 (DM)")

    tp3 = TP3_DATA / "j_vs_n.csv"
    if tp3.exists():
        d3 = pd.read_csv(tp3).sort_values("N")
        yerr3 = d3.get("J_std", pd.Series([0] * len(d3)))
        ax.errorbar(d3["N"], d3["J_mean"], yerr=yerr3,
                    fmt="s--", color="#d62728", ecolor="#d62728",
                    capsize=4, lw=1.6, markersize=7, markerfacecolor="white",
                    markeredgewidth=1.8, label="TP3 (EDMD)")

    ax.set_xlabel("N")
    ax.set_ylabel(r"$\langle J \rangle$  [partículas / s]")
    plain_log_axis(ax.xaxis, sorted(df["N"]))
    ax.margins(y=0.10)
    ax.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.2)

    out = FIGURES / "06_s2_j_vs_n.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
