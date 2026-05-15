"""Sandbox: J(N) (rango completo) leyendo de results/<sandbox_name>/j_vs_n.csv,
sin tocar los plots principales. Barras = SEM (no σ)."""
import argparse
from pathlib import Path
import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
from _style import RESULTS, FIGURES, TP3_DATA, plain_log_axis


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--indir", default=str(RESULTS / "s2_sandbox"),
                        help="Directorio con j_vs_n.csv (default: results/s2_sandbox)")
    parser.add_argument("--out", default=None,
                        help="Path del PNG de salida (default: auto desde label/M)")
    parser.add_argument("--label", default=None,
                        help="Etiqueta para título y nombre por defecto")
    args = parser.parse_args()

    sb_csv = Path(args.indir) / "j_vs_n.csv"
    if not sb_csv.exists():
        raise SystemExit(f"No existe {sb_csv}.")
    df = pd.read_csv(sb_csv).sort_values("N")
    if df.empty:
        raise SystemExit("Sandbox j_vs_n.csv vacío.")
    M = int(df["realizations"].iloc[0])
    k_val = float(df["k"].iloc[0])
    label = args.label or Path(args.indir).name

    fig, ax = plt.subplots(figsize=(8.8, 5.2))

    ax.errorbar(df["N"], df["J_mean"], yerr=df["J_std"] / np.sqrt(M),
                fmt="o-", color="#1f77b4", ecolor="#1f77b4",
                capsize=4, lw=1.8, markersize=8, markerfacecolor="white",
                markeredgewidth=1.8,
                label=rf"{label} (M={M}, k={k_val:.0e} N/m, barras: SEM)")

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
    ax.set_title(rf"SANDBOX  $\langle J \rangle$ vs. N  —  {label}, M={M}")
    plain_log_axis(ax.xaxis, sorted(df["N"]))
    ax.margins(y=0.10)
    ax.legend(loc="upper left", frameon=True, framealpha=0.95, handlelength=2.2)

    if args.out:
        out = Path(args.out)
        out.parent.mkdir(parents=True, exist_ok=True)
    else:
        out = FIGURES / f"sandbox_jvsn_M{M}.png"
    fig.savefig(out)
    print("→", out)


if __name__ == "__main__":
    main()
