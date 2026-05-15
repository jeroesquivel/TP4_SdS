"""Sandbox: rehace el plot 10 (k_scalar) combinando el main run
(k=100, 1000, 10000) con un sandbox que aporta ks intermedios (típicamente
500 y 5000). Permite identificar la ley de potencia con más puntos.

Lee:  results/s2/k_sweep.csv  +  <indir>/k_sweep.csv
Escribe: --out (o figures/sandbox_10_s2_k_scalar_extended.png)
"""
import argparse
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES


def k_label(k):
    e = np.log10(k)
    if abs(e - round(e)) < 1e-3:
        return rf"$10^{{{int(round(e))}}}$"
    mantissa = k / 10 ** np.floor(e)
    return rf"${mantissa:.0f}\cdot10^{{{int(np.floor(e))}}}$"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--indir", default=str(RESULTS / "s2_sandbox"))
    parser.add_argument("--out", default=None)
    parser.add_argument("--label", default=None)
    args = parser.parse_args()

    main_csv = RESULTS / "s2" / "k_sweep.csv"
    sb_csv = Path(args.indir) / "k_sweep.csv"

    main_df = pd.read_csv(main_csv) if main_csv.exists() else pd.DataFrame()
    sb_df = pd.read_csv(sb_csv) if sb_csv.exists() else pd.DataFrame()
    if main_df.empty and sb_df.empty:
        raise SystemExit("No hay datos en main ni en sandbox.")
    # Si el sandbox cubre un k, descarta el del main para ese k (evita mezclar seeds/M).
    if not sb_df.empty and not main_df.empty:
        sb_ks = set(sb_df["k"].unique())
        main_df = main_df[~main_df["k"].isin(sb_ks)]
    df = pd.concat([main_df, sb_df], ignore_index=True)

    ks = sorted(df["k"].unique())
    rows = []
    for k in ks:
        sub = df[df["k"] == k].sort_values("N")
        if sub.empty:
            continue
        idx = sub["J_mean"].idxmax()
        rows.append({
            "k": k,
            "max_J": sub.loc[idx, "J_mean"],
            "max_J_std": sub.loc[idx, "J_std"],
            "N_star": sub.loc[idx, "N"],
            "M": int(sub.loc[idx, "realizations"]),
        })
    s = pd.DataFrame(rows).sort_values("k").reset_index(drop=True)
    print(s.to_string(index=False))

    fig, ax = plt.subplots(figsize=(9.0, 5.4))
    ax.set_xscale("log")
    ax.set_xlabel("k [N/m]")
    k_lo, k_hi = float(s["k"].min()), float(s["k"].max())
    ax.set_xlim(k_lo / 1.6, k_hi * 1.6)
    ax.set_xticks(list(s["k"]))
    ax.set_xticklabels([k_label(k) for k in s["k"]])
    ax.minorticks_off()

    yerr = s["max_J_std"] / np.sqrt(s["M"])  # SEM
    l1 = ax.errorbar(s["k"], s["max_J"], yerr=yerr,
                     fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                     capsize=4, lw=2.0, markersize=9, markerfacecolor="white",
                     markeredgewidth=1.8,
                     label=r"$\max_N \langle J \rangle$  (barras: SEM)")
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

    ax.set_title(rf"Escalar característico vs. k  —  {len(s)} valores de k")

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
    else:
        out = FIGURES / "sandbox" / "kscalar_extendido.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
