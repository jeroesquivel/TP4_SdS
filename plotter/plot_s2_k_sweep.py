import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.colors import LogNorm
from matplotlib.cm import ScalarMappable
from _style import RESULTS, FIGURES, GRADIENT_CMAP, plain_log_axis

get_cmap = plt.get_cmap


def k_label(k):
    e = int(round(np.log10(k)))
    return rf"$10^{{{e}}}$"


def main():
    df = pd.read_csv(RESULTS / "s2" / "k_sweep.csv")
    ks = sorted(df["k"].unique())
    cmap = get_cmap(GRADIENT_CMAP)
    norm = LogNorm(vmin=min(ks), vmax=max(ks))

    # =========== Figura: <J>(N,k) y <J^in|S~2>(N,k) ===========
    fig, axes = plt.subplots(1, 2, figsize=(13.0, 5.0))

    handles = []
    labels = []
    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        c = cmap(norm(k))
        e1 = axes[0].errorbar(sub["N"], sub["J_mean"], yerr=sub["J_std"],
                              fmt="o-", color=c, ecolor=c, capsize=4, lw=1.8,
                              markersize=7, markerfacecolor="white",
                              markeredgewidth=1.6,
                              label=f"k={k_label(k)} N/m")
        handles.append(e1)
        labels.append(f"k = {k_label(k)} N/m")
        axes[1].plot(sub["N"], sub["J_in_S2_mean"], "s-", color=c, lw=1.8,
                     markersize=7, markerfacecolor="white", markeredgewidth=1.6)

    Ns_all = sorted(df["N"].unique())
    for ax in axes:
        ax.set_xlabel("N")
        ax.margins(y=0.10)
        plain_log_axis(ax.xaxis, Ns_all)

    axes[0].set_ylabel(r"$\langle J \rangle$  [partículas / s]")
    axes[0].set_title(r"Scanning rate global  $\langle J \rangle (N, k)$")
    axes[1].set_ylabel(r"$\langle J^{in} | S\!\sim\!2 \rangle$  [m$^{-1}$ s$^{-1}$]")
    axes[1].set_title(r"$\langle J^{in} | S\!\sim\!2 \rangle (N, k)$  —  capa cercana")

    sm = ScalarMappable(norm=norm, cmap=cmap); sm.set_array([])
    cbar = fig.colorbar(sm, ax=axes, fraction=0.030, pad=0.015)
    cbar.set_label("k [N/m]")

    fig.suptitle(r"Variación con la rigidez k  —  M=10 realizaciones,  t$_f$=500 s",
                 y=0.995, fontsize=14)

    out = FIGURES / "09_s2_k_sweep.png"
    fig.savefig(out)
    print("→", out)

    # =========== Figura: escalar característico vs k ===========
    rows = []
    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        if sub.empty: continue
        idx = sub["J_mean"].idxmax()
        rows.append({"k": k, "max_J": sub.loc[idx, "J_mean"],
                     "max_J_std": sub.loc[idx, "J_std"],
                     "N_star": sub.loc[idx, "N"]})
    s = pd.DataFrame(rows).sort_values("k")

    fig2, ax = plt.subplots(figsize=(9.0, 5.4))
    ax.set_xscale("log")
    ax.set_xlabel("k [N/m]")
    k_lo, k_hi = float(s["k"].min()), float(s["k"].max())
    ax.set_xlim(k_lo / 1.6, k_hi * 1.6)
    ax.set_xticks(list(s["k"]))
    ax.set_xticklabels([k_label(k) for k in s["k"]])
    ax.minorticks_off()

    l1 = ax.errorbar(s["k"], s["max_J"], yerr=s["max_J_std"],
                     fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                     capsize=4, lw=2.0, markersize=9, markerfacecolor="white",
                     markeredgewidth=1.8,
                     label=r"$\max_N \langle J \rangle$")
    ax.set_ylabel(r"$\max_N \langle J \rangle$  [partículas/s]", color="#1f77b4")
    ax.tick_params(axis="y", labelcolor="#1f77b4")
    ax.set_yscale("log")

    ax2 = ax.twinx()
    ax2.spines["top"].set_visible(False)
    l2 = ax2.plot(s["k"], s["N_star"], "s--", color="#d62728", lw=1.8,
                  markersize=9, markerfacecolor="white", markeredgewidth=1.8,
                  label=r"$N^\star$  (argmax)")
    ax2.set_ylabel(r"$N^\star$", color="#d62728")
    ax2.tick_params(axis="y", labelcolor="#d62728")
    ax2.set_yscale("log")

    lines = [l1, l2[0]]
    labels = [r"$\max_N \langle J \rangle$", r"$N^\star$  (argmax$_N$)"]
    ax.legend(lines, labels, loc="lower right", frameon=True, framealpha=0.95,
              handlelength=2.2)

    ax.set_title("Escalar característico vs. k")

    out2 = FIGURES / "10_s2_k_scalar.png"
    fig2.savefig(out2)
    print("→", out2)


if __name__ == "__main__":
    main()
