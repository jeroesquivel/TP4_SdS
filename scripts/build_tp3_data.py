"""Construye tp3_data/ a partir del directorio results/ del TP3 vecino.

Genera dos archivos compatibles con los plotters del TP4:
  tp3_data/timing.csv  — con columna `t_exec` (renombrada desde `avg_time_s`)
  tp3_data/j_vs_n.csv  — J(N) calculado como pendiente de Cfc(t) en
                         t >= tf/2 sobre los archivos cfc_sim_N_*.txt de TP3.

Uso: python3 scripts/build_tp3_data.py
"""
from pathlib import Path
import re
import numpy as np
import pandas as pd

TP3_RESULTS = Path.home() / "Desktop/ITBA/26-1C/SDS/TP3_SDS/results"
OUT = Path(__file__).resolve().parent.parent / "tp3_data"
OUT.mkdir(exist_ok=True)


def build_timing():
    df = pd.read_csv(TP3_RESULTS / "timing.csv")
    df = df.rename(columns={"avg_time_s": "t_exec", "std_time_s": "t_exec_std"})
    df.to_csv(OUT / "timing.csv", index=False)
    print(f"→ {OUT / 'timing.csv'}  ({len(df)} filas: N ∈ {df['N'].tolist()})")


def compute_j_from_cfc(path: Path):
    """Lee un cfc_sim_N_<N>_run_1.txt (TSV con headers '# t\\tCfc') y
    devuelve la pendiente de Cfc(t) sobre la segunda mitad del tiempo."""
    df = pd.read_csv(path, sep=r"\s+", comment="#", names=["t", "cfc"])
    if df.empty:
        return None
    t_max = df["t"].max()
    mask = df["t"] >= t_max / 2.0
    sub = df[mask]
    if len(sub) < 2:
        return None
    slope, _ = np.polyfit(sub["t"], sub["cfc"], 1)
    return slope, t_max


def build_jvsn():
    pattern = re.compile(r"cfc_sim_N_(\d+)_run_1(?:_light)?\.txt$")
    seen = {}
    for f in TP3_RESULTS.glob("cfc_sim_N_*_run_1*.txt"):
        m = pattern.match(f.name)
        if not m:
            continue
        N = int(m.group(1))
        is_light = f.name.endswith("_light.txt")
        # Preferir non-light si existe
        if N in seen and not is_light and seen[N][1]:
            seen[N] = (f, is_light)
        elif N not in seen:
            seen[N] = (f, is_light)
    rows = []
    for N in sorted(seen.keys()):
        f, _ = seen[N]
        res = compute_j_from_cfc(f)
        if res is None:
            print(f"[warn] {f.name}: sin datos suficientes")
            continue
        slope, tmax = res
        rows.append({"N": N, "J_mean": slope, "J_std": 0.0, "tf_tp3": tmax,
                     "source": f.name})
        print(f"  N={N:4d}  J={slope:.4e}  (tf={tmax:.1f}s, {f.name})")
    df = pd.DataFrame(rows)
    df.to_csv(OUT / "j_vs_n.csv", index=False)
    print(f"→ {OUT / 'j_vs_n.csv'}  ({len(df)} filas)")


if __name__ == "__main__":
    build_timing()
    print()
    build_jvsn()
