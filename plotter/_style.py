from pathlib import Path
import matplotlib as mpl
from matplotlib.ticker import ScalarFormatter, NullFormatter

mpl.rcParams.update({
    "figure.dpi": 300,
    "savefig.dpi": 300,
    "font.size": 13,
    "axes.titlesize": 14,
    "axes.labelsize": 13,
    "legend.fontsize": 11,
    "xtick.labelsize": 12,
    "ytick.labelsize": 12,
    "lines.linewidth": 1.7,
    "axes.grid": True,
    "grid.alpha": 0.28,
    "grid.linestyle": "--",
    "axes.spines.top": False,
    "axes.spines.right": False,
    "savefig.bbox": "tight",
    "savefig.pad_inches": 0.10,
})

GRADIENT_CMAP = "viridis"

ROOT = Path(__file__).resolve().parent.parent
RESULTS = ROOT / "results"
FIGURES = ROOT / "figures"
TP3_DATA = ROOT / "tp3_data"

FIGURES.mkdir(parents=True, exist_ok=True)


def plain_log_axis(axis, ticks, rotation=30):
    """Forzar etiquetas enteras (sin notación científica) en un eje log.
    Las etiquetas chocan cuando hay muchos ticks dentro de una década,
    así que se rotan por defecto."""
    axis.set_major_formatter(ScalarFormatter())
    axis.set_minor_formatter(NullFormatter())
    axis.set_major_locator(mpl.ticker.FixedLocator(list(ticks)))
    if rotation:
        for lbl in axis.get_ticklabels():
            lbl.set_rotation(rotation)
            lbl.set_ha("right")


def plain_n_colorbar(cbar, Ns):
    """Colorbar con ticks en los N reales y etiquetas enteras."""
    Ns = list(Ns)
    cbar.set_ticks(Ns)
    cbar.set_ticklabels([str(int(n)) for n in Ns])
    cbar.ax.minorticks_off()
