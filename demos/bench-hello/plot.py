import json
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

iri = json.loads(Path("/tmp/bench-hello/iridium.json").read_text())
spr = json.loads(Path("/tmp/bench-hello/spring.json").read_text())

labels = ["GET /api/hello", "GET /api/hello/{name}", "POST /api/echo"]
keys = ["hello", "path", "echo"]

iri_rps = [iri[k]["rps"] for k in keys]
spr_rps = [spr[k]["rps"] for k in keys]
iri_p50 = [iri[k]["p50_ms"] for k in keys]
spr_p50 = [spr[k]["p50_ms"] for k in keys]
iri_p99 = [iri[k]["p99_ms"] for k in keys]
spr_p99 = [spr[k]["p99_ms"] for k in keys]

x = np.arange(len(labels))
width = 0.36
iri_color = "#5b8def"
spr_color = "#6db33f"

fig, axes = plt.subplots(1, 2, figsize=(11, 4.6), constrained_layout=True)
fig.patch.set_facecolor("#0f1115")
for ax in axes:
    ax.set_facecolor("#171a21")
    ax.tick_params(colors="#d7dbe3")
    ax.yaxis.label.set_color("#d7dbe3")
    ax.title.set_color("#f4f6fb")
    for spine in ax.spines.values():
        spine.set_color("#2a303b")
    ax.grid(axis="y", color="#2a303b", linewidth=0.6)
    ax.set_axisbelow(True)

bars_iri = axes[0].bar(x - width / 2, iri_rps, width, label="Iridium", color=iri_color)
bars_spr = axes[0].bar(x + width / 2, spr_rps, width, label="Spring Boot 4.1.1", color=spr_color)
axes[0].set_title("Throughput")
axes[0].set_ylabel("requests / s")
axes[0].set_xticks(x, labels)
axes[0].legend(frameon=False, labelcolor="#d7dbe3")
axes[0].bar_label(bars_iri, fmt="%.0f", padding=3, color="#d7dbe3", fontsize=8)
axes[0].bar_label(bars_spr, fmt="%.0f", padding=3, color="#d7dbe3", fontsize=8)
axes[0].set_ylim(0, max(iri_rps + spr_rps) * 1.18)

axes[1].bar(x - width / 2, iri_p99, width, label="Iridium p99", color=iri_color)
axes[1].bar(x + width / 2, spr_p99, width, label="Spring p99", color=spr_color)
axes[1].scatter(x - width / 2, iri_p50, color="#f4f6fb", s=28, zorder=3, label="p50")
axes[1].scatter(x + width / 2, spr_p50, color="#f4f6fb", s=28, zorder=3)
axes[1].set_title("Latency")
axes[1].set_ylabel("ms")
axes[1].set_xticks(x, labels)
axes[1].legend(frameon=False, labelcolor="#d7dbe3")
axes[1].set_ylim(0, max(iri_p99 + spr_p99) * 1.25)

fig.suptitle("Hello benchmark  ·  20k sequential requests, 2k warmup", color="#f4f6fb", fontsize=13)
out = Path("/home/ly/Projects/iridium-framework/demos/bench-hello/hello-benchmark.png")
fig.savefig(out, dpi=160)
print(out)
