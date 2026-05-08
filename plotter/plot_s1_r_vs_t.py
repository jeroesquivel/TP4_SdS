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
    fig, axes = plt.subplots(2, 1, figsize=(10.0, 6.8),
                             gridspec_kw={"height_ratios": [1.0, 1.0], "hspace": 0.36})

    # Panel superior: r(t) completo
    ax_top = axes[0]
    ana_plotted = False
    t_max = 5.0
    handles, labels = [], []
    for key, label, color in INTEGRATORS:
        f = s1 / f"trajectory_{key}_dt1e-03.csv"
        if not f.exists():
            print(f"missing {f}"); continue
        df = pd.read_csv(f)
        if not ana_plotted:
            l_ana, = ax_top.plot(df["t"], df["r_ana"], color="black", lw=2.4,
                                 label="Analítica", zorder=10)
            handles.append(l_ana); labels.append("Analítica")
            ana_plotted = True
        l_num, = ax_top.plot(df["t"], df["r_num"], color=color, lw=1.3,
                             label=label, alpha=0.9)
        handles.append(l_num); labels.append(label)
        t_max = df["t"].max()
    ax_top.set_xlabel("t [s]")
    ax_top.set_ylabel("r [m]")
    ax_top.set_xlim(0, t_max)
    ax_top.set_title(r"Trayectoria r(t) — $\Delta t = 10^{-3}$ s")

    # Panel inferior: error (r_num - r_ana)
    ax_bot = axes[1]
    for key, label, color in INTEGRATORS:
        f = s1 / f"trajectory_{key}_dt1e-03.csv"
        if not f.exists(): continue
        df = pd.read_csv(f)
        err = df["r_num"] - df["r_ana"]
        ax_bot.plot(df["t"], err, color=color, lw=1.4, alpha=0.95)
    ax_bot.axhline(0.0, color="black", lw=0.9, alpha=0.4)
    ax_bot.set_xlabel("t [s]")
    ax_bot.set_ylabel(r"$r_{num}(t) - r_{ana}(t)$ [m]")
    ax_bot.set_xlim(0, t_max)
    ax_bot.set_title("Error puntual respecto de la solución analítica")

    # Leyenda compartida fuera de los axes (debajo del título)
    fig.legend(handles, labels, loc="upper center", ncol=5,
               bbox_to_anchor=(0.5, 1.005), frameon=False,
               handlelength=2.0, columnspacing=1.6)

    out = FIGURES / "s1_r_vs_t.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
