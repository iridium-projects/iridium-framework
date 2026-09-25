import json
from pathlib import Path

import matplotlib.pyplot as plt
import numpy as np

startup = json.loads(Path("/tmp/bench-shop/startup.json").read_text())
iri = json.loads(Path("/tmp/bench-shop/iridium-resp.json").read_text())
spr = json.loads(Path("/tmp/bench-shop/spring-resp.json").read_text())

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

iri_color = "#5b8def"
spr_color = "#6db33f"

start_vals = [startup["iridium"]["median_ms"], startup["spring"]["median_ms"]]
bars = axes[0].bar(["Iridium", "Spring Boot 4.1.1"], start_vals, color=[iri_color, spr_color], width=0.55)
axes[0].bar_label(bars, fmt="%.0f ms", padding=3, color="#d7dbe3", fontsize=9)
axes[0].set_title("Startup to first /api/health")
axes[0].set_ylabel("median ms  ·  8 runs")
axes[0].set_ylim(0, max(start_vals) * 1.2)

labels = ["GET /health", "GET /products/{sku}", "POST /orders"]
keys = ["health", "product", "order"]
x = np.arange(len(labels))
width = 0.36
iri_rps = [iri[k]["rps"] for k in keys]
spr_rps = [spr[k]["rps"] for k in keys]
b1 = axes[1].bar(x - width / 2, iri_rps, width, label="Iridium", color=iri_color)
b2 = axes[1].bar(x + width / 2, spr_rps, width, label="Spring Boot 4.1.1", color=spr_color)
axes[1].bar_label(b1, fmt="%.0f", padding=3, color="#d7dbe3", fontsize=8)
axes[1].bar_label(b2, fmt="%.0f", padding=3, color="#d7dbe3", fontsize=8)
axes[1].set_title("Response throughput")
axes[1].set_ylabel("requests / s")
axes[1].set_xticks(x, labels)
axes[1].legend(frameon=False, labelcolor="#d7dbe3")
axes[1].set_ylim(0, max(iri_rps + spr_rps) * 1.18)

fig.suptitle("Shop API  ·  38 services, 3 routes  ·  sequential", color="#f4f6fb", fontsize=13)
out = Path("/home/ly/Projects/iridium-framework/demos/bench-hello/shop-benchmark.png")
fig.savefig(out, dpi=160)
print(out)
