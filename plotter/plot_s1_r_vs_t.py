import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES

INTEGRATORS = [
    ("euler",  "Euler PC",          "#d62728"),
    ("verlet", "Verlet original",   "#2ca02c"),
    ("beeman", "Beeman PC",         "#1f77b4"),
    ("gear5",  "Gear PC orden 5",   "#9467bd"),
]


def main():
    s1 = RESULTS / "s1"
    fig, axes = plt.subplots(2, 2, figsize=(11.0, 6.8), sharex=True, sharey=True)

    t_max = 5.0
    y_lo = 0.0
    y_hi = 0.0
    for (key, label, color), ax in zip(INTEGRATORS, axes.flat):
        f = s1 / f"trajectory_{key}_dt1e-03.csv"
        if not f.exists():
            ax.set_title(f"{label}  (sin datos)")
            ax.text(0.5, 0.5, "missing", ha="center", va="center",
                    transform=ax.transAxes, color="dimgray")
            continue
        df = pd.read_csv(f)
        ax.plot(df["t"], df["r_ana"], color="black", lw=2.2,
                label="Analítica", zorder=1)
        ax.plot(df["t"], df["r_num"], color=color, lw=1.6,
                label=label, alpha=1.0, zorder=10)
        ax.set_title(label)
        ax.legend(loc="upper right", frameon=True, framealpha=0.92,
                  handlelength=2.0, fontsize=10)
        t_max = df["t"].max()
        y_lo = min(y_lo, df["r_num"].min(), df["r_ana"].min())
        y_hi = max(y_hi, df["r_num"].max(), df["r_ana"].max())

    for ax in axes.flat:
        ax.set_xlim(0, t_max)
        ax.margins(y=0.10)
    for ax in axes[-1, :]:
        ax.set_xlabel("t [s]")
    for ax in axes[:, 0]:
        ax.set_ylabel("r [m]")

    fig.tight_layout()

    out = FIGURES / "01_s1_r_vs_t.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
